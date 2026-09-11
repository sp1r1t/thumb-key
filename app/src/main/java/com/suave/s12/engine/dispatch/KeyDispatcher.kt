package com.suave.s12.engine.dispatch

import com.suave.s12.engine.action.CursorDirection
import com.suave.s12.engine.action.IntentCompiler
import com.suave.s12.engine.action.SemanticAction
import com.suave.s12.engine.feedback.FeedbackEvent
import com.suave.s12.engine.gesture.Gesture
import com.suave.s12.engine.gesture.SlideAxis
import com.suave.s12.engine.gesture.Zone
import com.suave.s12.engine.intent.CommandId
import com.suave.s12.engine.intent.KeyIntent
import com.suave.s12.engine.intent.KeyMapping
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.engine.intent.SlideBehavior
import com.suave.s12.engine.modifier.ModifierEngine
import com.suave.s12.engine.modifier.ModifierState
import com.suave.s12.utils.KeyAction
import kotlin.math.abs

/**
 * Orchestrates one physical key's full pipeline - Gesture -> intent lookup -> modifier
 * transformation -> action/legacy dispatch -> feedback - for a single press at a time. Plain
 * Kotlin, no Android/Compose dependency, so it's unit-testable like the rest of `engine/`; the
 * UI layer (Step 5) creates one instance per rendered key and feeds it every [Gesture] that
 * key's [com.suave.s12.engine.gesture.GestureRecognizer] emits.
 *
 * Callbacks are parameters of [handle] itself, not constructor fields, deliberately: a
 * long-lived object holding onto Compose-recomposition-scoped lambdas is exactly the kind of
 * stale-closure hazard this rewrite exists to avoid (see [ModifierEngine]'s own doc). Only
 * [mapping] and [shiftMappings] - genuinely static for this key's lifetime - are held.
 *
 * The state this class owns across a press: which [ModifierId] (if any) the press's locked zone
 * engaged, and whether *this specific press* is what activated it. The first matters because
 * Ctrl/Alt/Esc all live on the same physical key at different zones (center/right/up) -
 * [Gesture.Released] carries no zone, so without remembering which modifier this press engaged,
 * there would be no way to know which one to apply the release transition to. The second matters
 * because [Gesture.Pressed] can now activate a modifier provisionally before its own hold
 * threshold ever fires (see [handlePressed]) - a later [Gesture.Tap] needs to know whether it's
 * reclassifying that fresh activation into the old sticky one-shot behavior, or explicitly
 * toggling off something that was already active for an unrelated reason.
 */
class KeyDispatcher(
    private val mapping: KeyMapping,
    private val shiftMappings: Map<String, String> = emptyMap(),
) {
    private var engagedModifier: ModifierId? = null
    private var freshlyActivatedByPressed = false
    private var slideExtended = false
    private var slideStarted = false

    fun handle(
        gesture: Gesture,
        modifierState: ModifierState,
        onExecute: (SemanticAction) -> Unit,
        onLegacyAction: (KeyAction) -> Unit,
        onFeedback: (FeedbackEvent) -> Unit,
    ): ModifierState =
        when (gesture) {
            Gesture.Pressed -> {
                handlePressed(modifierState, onFeedback)
            }

            is Gesture.Tap -> {
                dispatchZone(gesture.zone, gesture, modifierState, consumeOneShot = true, onExecute, onLegacyAction, onFeedback)
            }

            is Gesture.Hold -> {
                dispatchZone(gesture.zone, gesture, modifierState, consumeOneShot = true, onExecute, onLegacyAction, onFeedback)
            }

            is Gesture.HoldRepeat -> {
                dispatchZone(gesture.zone, gesture, modifierState, consumeOneShot = false, onExecute, onLegacyAction, onFeedback)
            }

            is Gesture.SlideStep -> {
                dispatchSlide(gesture, modifierState, onExecute, onFeedback)
                modifierState
            }

            is Gesture.SwipeLocked -> {
                handleSwipeLocked(gesture, modifierState, onFeedback)
            }

            Gesture.Released, Gesture.Cancelled -> {
                finishPress(gesture, modifierState, onExecute, onFeedback)
            }
        }

    /**
     * Fires the universal "something was touched" buzz for every key, then - only for a key
     * whose center is a modifier - provisionally activates it immediately (see
     * [ModifierEngine.applyModifierGesture]'s `Gesture.Pressed` branch for why). This is a
     * guess at the center zone specifically: at touch-down nothing about a swipe is known yet,
     * so if this press turns out to swipe to a *different* modifier on the same key (Alt/Esc
     * reached by swiping off Ctrl's center), [handleSwipeLocked] hands off from this guess to
     * the right one.
     */
    private fun handlePressed(
        modifierState: ModifierState,
        onFeedback: (FeedbackEvent) -> Unit,
    ): ModifierState {
        onFeedback(FeedbackEvent.TapRecognized)
        val centerIntent = mapping.intents[Zone.Center]
        if (centerIntent !is KeyIntent.ModifierPress) return modifierState
        return provisionallyActivate(centerIntent.modifier, modifierState)
    }

    private fun provisionallyActivate(
        modifier: ModifierId,
        state: ModifierState,
    ): ModifierState {
        freshlyActivatedByPressed = !state.isActive(modifier)
        engagedModifier = modifier
        return ModifierEngine.applyModifierGesture(state, modifier, Gesture.Pressed)
    }

    private fun handleSwipeLocked(
        gesture: Gesture.SwipeLocked,
        modifierState: ModifierState,
        onFeedback: (FeedbackEvent) -> Unit,
    ): ModifierState {
        onFeedback(FeedbackEvent.SwipeLocked(gesture.direction))
        val zoneIntent = mapping.intents[Zone.Directional(gesture.direction)]
        if (zoneIntent !is KeyIntent.ModifierPress || zoneIntent.modifier == engagedModifier) return modifierState
        // Hand off from whatever handlePressed guessed (this key's Ctrl/Alt/Esc all share one
        // physical key) to the modifier this swipe actually locked onto - undoing the guess
        // first, but only if it was actually us who activated it a moment ago.
        val previous = engagedModifier
        val state = if (previous != null && freshlyActivatedByPressed) modifierState.deactivate(previous) else modifierState
        return provisionallyActivate(zoneIntent.modifier, state)
    }

    private fun dispatchZone(
        zone: Zone,
        gesture: Gesture,
        modifierState: ModifierState,
        consumeOneShot: Boolean,
        onExecute: (SemanticAction) -> Unit,
        onLegacyAction: (KeyAction) -> Unit,
        onFeedback: (FeedbackEvent) -> Unit,
    ): ModifierState {
        val intent = mapping.intents[zone] ?: return modifierState
        return when (intent) {
            is KeyIntent.ModifierPress -> {
                // No feedback here: Gesture.Pressed/SwipeLocked already buzzed for this exact
                // moment (handlePressed/handleSwipeLocked, above) - firing ModifierActivated on
                // top of that was the "too many vibrations" bug. Released still reports it
                // (finishPress, below) - that's a genuinely later, distinct moment for a real
                // hold, not a duplicate of a buzz that just happened.
                engagedModifier = intent.modifier
                ModifierEngine.applyModifierGesture(
                    modifierState,
                    intent.modifier,
                    gesture,
                    freshlyActivatedByPressed = freshlyActivatedByPressed,
                )
            }

            is KeyIntent.LegacyAction -> {
                // Only fire once per press, on Tap or the first Hold - never on HoldRepeat, same
                // reasoning as a held modifier not spamming its own toggle: repeating "open
                // settings" or "toggle emoji mode" on every repeat tick isn't meaningful.
                // Feedback for the press itself already happened on Gesture.Pressed, and (for a
                // directional zone) on Gesture.SwipeLocked - nothing further fires here, this
                // just performs the actual action.
                if (gesture is Gesture.Tap || gesture is Gesture.Hold) {
                    onLegacyAction(intent.action)
                }
                modifierState
            }

            is KeyIntent.Text, is KeyIntent.Command, KeyIntent.Noop -> {
                val resolved = ModifierEngine.resolve(modifierState, intent, shiftMappings)
                onExecute(IntentCompiler.compile(resolved))
                // Feedback for the press/swipe itself already happened on Gesture.Pressed/
                // SwipeLocked; only an ongoing hold-repeat gets its own (quieter) tick here.
                if (gesture is Gesture.HoldRepeat) onFeedback(FeedbackEvent.RepeatTick)
                if (consumeOneShot) ModifierEngine.consumeOneShots(modifierState) else modifierState
            }
        }
    }

    private fun dispatchSlide(
        step: Gesture.SlideStep,
        modifierState: ModifierState,
        onExecute: (SemanticAction) -> Unit,
        onFeedback: (FeedbackEvent) -> Unit,
    ) {
        val direction = directionFor(step)
        // Only the first slide step of this press should tell engine/output to trust a fresh
        // read of the editor's position - see SemanticAction's resetAnchor doc.
        val resetAnchor = !slideStarted
        slideStarted = true
        val action =
            when (mapping.slideBehavior) {
                SlideBehavior.SELECT_AND_DELETE -> {
                    slideExtended = true
                    SemanticAction.ExtendSelection(direction, resetAnchor)
                }

                // A plain cursor-move slide becomes a selection-extend slide for as long as
                // Shift is held elsewhere on the keyboard - the same "select instead of move"
                // relationship backspace's own slide has to its delete, just modifier-driven
                // instead of hardcoded to one key. This is deliberately re-checked on every step
                // (not just the first) so starting a slide before Shift goes down, or releasing
                // Shift mid-slide, switches modes live instead of freezing whatever was true at
                // the first step.
                SlideBehavior.MOVE_CURSOR, null -> {
                    if (modifierState.isActive(ModifierId.SHIFT)) {
                        SemanticAction.ExtendSelection(direction, resetAnchor)
                    } else {
                        SemanticAction.MoveCursor(direction, resetAnchor)
                    }
                }
            }
        repeat(abs(step.steps)) { onExecute(action) }
        onFeedback(FeedbackEvent.SlideStep)
    }

    private fun finishPress(
        gesture: Gesture,
        modifierState: ModifierState,
        onExecute: (SemanticAction) -> Unit,
        onFeedback: (FeedbackEvent) -> Unit,
    ): ModifierState {
        var state = modifierState
        engagedModifier?.let { modId ->
            val wasActive = state.isActive(modId)
            state = ModifierEngine.applyModifierGesture(state, modId, gesture)
            reportModifierFeedback(modId, wasActive, state, onFeedback)
        }
        // Backspace's select-and-delete slide: the selection was only ever extended, never
        // deleted, while sliding - Android's standard convention is that a Backspace KeyEvent
        // deletes the active selection instead of one char, so a single command on release does
        // the actual delete.
        if (slideExtended && mapping.slideBehavior == SlideBehavior.SELECT_AND_DELETE) {
            onExecute(SemanticAction.TypeCommand(CommandId.BACKSPACE))
        }
        // A ONE_SHOT modifier (e.g. a quick Shift tap before sliding to select) was never
        // consumed by the slide itself - dispatchSlide's ExtendSelection/MoveCursor actions
        // bypass dispatchZone entirely, which is the only place that normally consumes one-shots.
        // Consuming here, once the whole slide press ends, is what a single physical press using
        // a one-shot modifier is supposed to do either way - and doing it here rather than per
        // step is what keeps a one-shot Shift held for the *entire* slide instead of reverting to
        // plain cursor movement after just the first step.
        if (slideStarted && gesture is Gesture.Released) {
            state = ModifierEngine.consumeOneShots(state)
        }
        engagedModifier = null
        freshlyActivatedByPressed = false
        slideExtended = false
        slideStarted = false
        return state
    }

    private fun reportModifierFeedback(
        id: ModifierId,
        wasActive: Boolean,
        newState: ModifierState,
        onFeedback: (FeedbackEvent) -> Unit,
    ) {
        val nowActive = newState.isActive(id)
        when {
            !wasActive && nowActive -> {
                onFeedback(FeedbackEvent.ModifierActivated(id, newState.active.getValue(id).mode))
            }

            wasActive && !nowActive -> {
                onFeedback(FeedbackEvent.ModifierDeactivated(id))
            }

            // No change to report: HoldRepeat's own no-op, or LOCKED persisting through release.
            else -> {}
        }
    }

    private fun directionFor(step: Gesture.SlideStep): CursorDirection =
        when (step.axis) {
            SlideAxis.HORIZONTAL -> if (step.steps > 0) CursorDirection.RIGHT else CursorDirection.LEFT
            SlideAxis.VERTICAL -> if (step.steps > 0) CursorDirection.DOWN else CursorDirection.UP
        }
}
