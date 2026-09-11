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
 * The one piece of state this class owns across a press: which [ModifierId] (if any) the
 * press's locked zone engaged. This matters because Ctrl/Alt/Esc all live on the same physical
 * key at different zones (center/right/up) - [Gesture.Released] carries no zone, so without
 * remembering which modifier this specific press engaged, there would be no way to know which
 * one to apply the release transition to.
 */
class KeyDispatcher(
    private val mapping: KeyMapping,
    private val shiftMappings: Map<String, String> = emptyMap(),
) {
    private var engagedModifier: ModifierId? = null
    private var slideExtended = false

    fun handle(
        gesture: Gesture,
        modifierState: ModifierState,
        onExecute: (SemanticAction) -> Unit,
        onLegacyAction: (KeyAction) -> Unit,
        onFeedback: (FeedbackEvent) -> Unit,
    ): ModifierState =
        when (gesture) {
            Gesture.Pressed -> {
                // Unconditional - fires for every press regardless of what mapping.intents has
                // at any zone, before any of that is even known. See Gesture.Pressed's doc for
                // why this is intentionally separate from whatever fires later (SwipeLocked,
                // ModifierActivated, etc.) rather than folded into it.
                onFeedback(FeedbackEvent.TapRecognized)
                modifierState
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
                dispatchSlide(gesture, onExecute, onFeedback)
                modifierState
            }

            is Gesture.SwipeLocked -> {
                // Purely a feedback signal, mid-drag - the actual commit still resolves later
                // via Tap/Hold for the same zone, unchanged.
                onFeedback(FeedbackEvent.SwipeLocked(gesture.direction))
                modifierState
            }

            Gesture.Released, Gesture.Cancelled -> {
                finishPress(gesture, modifierState, onExecute, onFeedback)
            }
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
                val wasActive = modifierState.isActive(intent.modifier)
                val newState = ModifierEngine.applyModifierGesture(modifierState, intent.modifier, gesture)
                engagedModifier = intent.modifier
                reportModifierFeedback(intent.modifier, wasActive, newState, onFeedback)
                newState
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
        onExecute: (SemanticAction) -> Unit,
        onFeedback: (FeedbackEvent) -> Unit,
    ) {
        val direction = directionFor(step)
        val action =
            when (mapping.slideBehavior) {
                SlideBehavior.SELECT_AND_DELETE -> {
                    slideExtended = true
                    SemanticAction.ExtendSelection(direction)
                }

                SlideBehavior.MOVE_CURSOR, null -> {
                    SemanticAction.MoveCursor(direction)
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
        engagedModifier = null
        slideExtended = false
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
