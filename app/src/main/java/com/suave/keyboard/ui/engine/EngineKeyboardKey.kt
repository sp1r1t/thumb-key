package com.suave.keyboard.ui.engine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suave.keyboard.db.DEFAULT_ANIMATION_HELPER_SPEED
import com.suave.keyboard.db.DEFAULT_ANIMATION_SPEED
import com.suave.keyboard.engine.action.SemanticAction
import com.suave.keyboard.engine.action.SpacebarMultitapTracker
import com.suave.keyboard.engine.action.isPlainSpaceTap
import com.suave.keyboard.engine.dispatch.KeyDispatcher
import com.suave.keyboard.engine.feedback.FeedbackEvent
import com.suave.keyboard.engine.gesture.Direction
import com.suave.keyboard.engine.gesture.Gesture
import com.suave.keyboard.engine.gesture.GestureRecognizer
import com.suave.keyboard.engine.gesture.RecognizerInput
import com.suave.keyboard.engine.gesture.TouchEvent
import com.suave.keyboard.engine.gesture.TouchPhase
import com.suave.keyboard.engine.gesture.Zone
import com.suave.keyboard.engine.intent.KeyIntent
import com.suave.keyboard.engine.intent.KeyMapping
import com.suave.keyboard.engine.intent.ModifierId
import com.suave.keyboard.engine.modifier.ActivationMode
import com.suave.keyboard.engine.modifier.ModifierBehavior
import com.suave.keyboard.engine.modifier.ModifierState
import com.suave.keyboard.utils.ColorVariant
import com.suave.keyboard.utils.colorVariantToColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private const val TICK_INTERVAL_MS = 30L

/**
 * One physical key: raw touch handling + rendering, no dispatch logic of its own - see
 * [KeyDispatcher] for the gesture -> intent -> modifier -> action pipeline this hands each
 * recognized [Gesture] to.
 *
 * Reads raw pointer input directly ([awaitFirstDown]/`awaitPointerEvent`, via the standard
 * [awaitEachGesture] loop) rather than `detectDragGestures`, because [GestureRecognizer] needs
 * the DOWN event immediately - a plain tap that never moves past the swipe threshold never
 * triggers `detectDragGestures` at all, and hold/repeat timing needs to know exactly when the
 * press started. `withTimeoutOrNull` around `awaitPointerEvent()` doubles as the periodic tick
 * source for hold/repeat, so this needs no separate coroutine of its own.
 *
 * NOTE: this pointer-input loop has not been exercised on a real device yet (no attached
 * device/emulator this session) - it compiles and the dispatch logic it calls into is unit
 * tested, but the raw touch handling itself is the least-verified part of this rewrite.
 */
@Composable
fun EngineKeyboardKey(
    mapping: KeyMapping,
    modifierState: MutableState<ModifierState>,
    shiftLegendState: ModifierState,
    onExecute: (SemanticAction) -> Unit,
    onFeedback: (FeedbackEvent) -> Unit,
    shiftMappings: Map<String, String>,
    capsLockMappings: Map<String, String> = emptyMap(),
    minSwipeDistancePx: Float,
    legendVisibility: LegendVisibility,
    modifierBehaviors: Map<ModifierId, ModifierBehavior>,
    keyHeight: Dp,
    keyPadding: Int,
    keyPaddingVertical: Int,
    keyBorderWidthDp: Float,
    keyCornerRadius: Dp,
    animations: KeyAnimationSettings = KeyAnimationSettings(),
    isPasswordField: Boolean = false,
    distinctLetterControlColors: Boolean = true,
    spacebarMultitap: SpacebarMultitapTracker? = null,
    spacebarMultitapEnabled: Boolean = false,
    spaceMultitapCycle: List<String>? = null,
    modifier: Modifier = Modifier,
) {
    val dispatcher =
        remember(mapping, shiftMappings, capsLockMappings, modifierBehaviors) {
            KeyDispatcher(mapping, shiftMappings, modifierBehaviors, capsLockMappings)
        }

    // Values that change across recompositions of the *same* mapping are read through
    // rememberUpdatedState. The loop itself restarts when [mapping] changes: numeric/main
    // reuse the same composed key slots, and a loop keyed on Unit would keep dispatching the
    // letter-key intents after the labels had already switched.
    val currentMinSwipeDistancePx by rememberUpdatedState(minSwipeDistancePx)
    val currentOnExecute by rememberUpdatedState(onExecute)
    val currentOnFeedback by rememberUpdatedState(onFeedback)
    val currentDispatcher by rememberUpdatedState(dispatcher)
    val currentMapping by rememberUpdatedState(mapping)
    val currentAnimations by rememberUpdatedState(animations)
    val currentIsPasswordField by rememberUpdatedState(isPasswordField)
    val currentSpacebarMultitap by rememberUpdatedState(spacebarMultitap)
    val currentSpacebarMultitapEnabled by rememberUpdatedState(spacebarMultitapEnabled)
    val currentSpaceMultitapCycle by rememberUpdatedState(spaceMultitapCycle)
    // MutableState (not `by`) so press/release visuals are read only in draw / a child. Writing
    // them from the pointer loop used to recompose this key mid-gesture: the highlight swapped
    // Modifier.background, legends relaid out, and a slightly slow Shift+letter crossed the
    // hold-repeat threshold as two characters.
    val isPressed = remember { mutableStateOf(false) }
    val releasedGlyph = remember { mutableStateOf<ReleasedGlyph?>(null) }
    val hasModifierIntent = mapping.intents.values.any { it is KeyIntent.ModifierPress }
    // Letter keys must not read modifierState.value here: that would resubscribe them on
    // HELD -> ONE_SHOT and they would dispatch from a display-only copy. They take
    // [shiftLegendState] (off / shift / caps) for legends and read the live state only inside
    // the pointer loop.
    val legendModifierState =
        if (hasModifierIntent) {
            modifierState.value
        } else {
            shiftLegendState
        }

    val isModifierKeyActive =
        hasModifierIntent &&
            mapping.intents.values.any { it is KeyIntent.ModifierPress && modifierState.value.isActive(it.modifier) }
    val restingColor =
        if (isModifierKeyActive) {
            MaterialTheme.colorScheme.primary
        } else {
            colorVariantToColor(mapping.restingFillVariant(distinctLetterControlColors))
        }
    val pressHighlightColor = MaterialTheme.colorScheme.inversePrimary
    val pressHighlightEnabled = animations.pressHighlight
    val keyShape = RoundedCornerShape(keyCornerRadius)
    val keyBorderColour = MaterialTheme.colorScheme.outline
    val swipeColor = colorVariantToColor(legendColorVariant(isCenter = false))
    val centerColor =
        if (isModifierKeyActive) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            colorVariantToColor(legendColorVariant(isCenter = true))
        }
    val density = LocalDensity.current
    val legendKeySize = (keyHeight - (keyPaddingVertical * 2).dp).coerceAtLeast(1.dp)
    val swipeSize = legendFontSize(isCenter = false, legendKeySize, isUpperCase = false)
    val swipeFontSize = with(density) { swipeSize.toSp() }

    Box(
        modifier =
            modifier
                .padding(horizontal = keyPadding.dp, vertical = keyPaddingVertical.dp)
                .clip(keyShape)
                .then(
                    if (keyBorderWidthDp > 0f) {
                        Modifier.border(keyBorderWidthDp.dp, keyBorderColour, keyShape)
                    } else {
                        Modifier
                    },
                ).drawBehind {
                    val fill =
                        if (pressHighlightEnabled && isPressed.value) {
                            pressHighlightColor
                        } else {
                            restingColor
                        }
                    drawRect(fill)
                }.pointerInput(mapping) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        // Both driven by the same setting: there's no separate slide-sensitivity
                        // preference on this branch (that's the old app's multi-mode
                        // acceleration system, out of Phase 1 scope), and the layout-authored
                        // slideStepPx default (24px) proved far too sensitive in practice -
                        // reusing the swipe-length setting the user already controls gives a
                        // sensible, tunable default instead of a second hardcoded constant.
                        val config =
                            currentMapping.gestureConfig.copy(
                                minSwipeDistancePx = currentMinSwipeDistancePx,
                                slideStepPx = currentMinSwipeDistancePx,
                            )
                        val recognizer = GestureRecognizer(config)

                        // Seeded fresh per press from the live shared modifier state, then
                        // tracked locally for the rest of THIS press - not re-read on every
                        // call. A single press can emit several gestures in one synchronous
                        // batch (onRelease returns [Tap, Released] together), and Compose's
                        // snapshot-state write from the first call doesn't reach other
                        // readers until the next recomposition, which hasn't happened yet by
                        // the time the second gesture in the same batch runs. Re-reading the
                        // stale value there let Released (which passes a non-modifier key's
                        // input state straight through unchanged) silently resurrect whatever
                        // Tap had just cleared a microsecond earlier - this was the actual
                        // "Ctrl gets stuck" bug.
                        var localState = modifierState.value

                        fun handle(gesture: Gesture) {
                            if (gesture is Gesture.Pressed && currentAnimations.pressHighlight) {
                                isPressed.value = true
                            }
                            val before = localState
                            var typed: String? = null
                            var executed: SemanticAction? = null
                            val newState =
                                currentDispatcher.handle(
                                    gesture,
                                    before,
                                    { action ->
                                        val resolved =
                                            resolveSpacebarMultitap(
                                                gesture = gesture,
                                                action = action,
                                                tracker = currentSpacebarMultitap,
                                                enabled = currentSpacebarMultitapEnabled,
                                                cycle = currentSpaceMultitapCycle,
                                            )
                                        executed = resolved
                                        if (currentAnimations.playsRelease &&
                                            !currentIsPasswordField &&
                                            (gesture is Gesture.Tap || gesture is Gesture.Hold)
                                        ) {
                                            typedTextForReleaseAnimation(resolved)?.let { typed = it }
                                        }
                                        currentOnExecute(resolved)
                                    },
                                    currentOnFeedback,
                                )
                            // onExecute may set ONE_SHOT Shift for autocap, but handle's return
                            // value still has consumeOneShots(before) - which clears a Shift that
                            // was already on (e.g. multitap ". " then "? "). Prefer autocap's
                            // ONE_SHOT decision; otherwise trust the dispatcher (including when
                            // autocap clears ONE_SHOT after a comma).
                            // Only reconcile after an executed action: a pure Shift tap never
                            // runs onExecute, and reading the still-stale shared ONE_SHOT would
                            // undo the user's intentional toggle-off of auto-caps.
                            var next = newState
                            if (executed != null) {
                                when (modifierState.value.active[ModifierId.SHIFT]?.mode) {
                                    ActivationMode.ONE_SHOT ->
                                        next = next.activate(ModifierId.SHIFT, ActivationMode.ONE_SHOT)
                                    null ->
                                        if (next.active[ModifierId.SHIFT]?.mode == ActivationMode.ONE_SHOT) {
                                            next = next.deactivate(ModifierId.SHIFT)
                                        }
                                    else -> Unit
                                }
                            }
                            localState = next
                            if (next != before || executed != null) {
                                modifierState.value = next
                            }
                            if (gesture is Gesture.Released || gesture is Gesture.Cancelled) {
                                isPressed.value = false
                            }
                            typed?.let { text ->
                                releasedGlyph.value = ReleasedGlyph(text)
                            }
                        }

                        try {
                            recognizer
                                .process(
                                    RecognizerInput.Touch(
                                        TouchEvent(down.position.x, down.position.y, System.currentTimeMillis(), TouchPhase.DOWN),
                                    ),
                                ).forEach(::handle)

                            var pressed = true
                            while (pressed) {
                                val event = withTimeoutOrNull(TICK_INTERVAL_MS) { awaitPointerEvent() }
                                if (event == null) {
                                    recognizer.process(RecognizerInput.Tick(System.currentTimeMillis())).forEach(::handle)
                                    continue
                                }
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null) {
                                    // Another pointer's event (hover, second finger, overlay).
                                    // Cancelling here dropped the commit after press+swipe
                                    // already buzzed.
                                    continue
                                }
                                if (!change.pressed) {
                                    pressed = false
                                    recognizer
                                        .process(
                                            RecognizerInput.Touch(
                                                TouchEvent(
                                                    change.position.x,
                                                    change.position.y,
                                                    System.currentTimeMillis(),
                                                    TouchPhase.UP,
                                                ),
                                            ),
                                        ).forEach(::handle)
                                    change.consume()
                                } else {
                                    recognizer
                                        .process(
                                            RecognizerInput.Touch(
                                                TouchEvent(change.position.x, change.position.y, System.currentTimeMillis(), TouchPhase.MOVE),
                                            ),
                                        ).forEach(::handle)
                                    change.consume()
                                }
                            }
                        } finally {
                            isPressed.value = false
                        }
                    }
                },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(2.dp),
        ) {
            for ((direction, alignment) in DIRECTIONAL_ALIGNMENTS) {
                val zone = Zone.Directional(direction)
                val legend =
                    keyLegend(
                        mapping.intents[zone],
                        legendVisibility,
                        legendModifierState,
                        shiftMappings,
                        capsLockMappings,
                        displayLabel = mapping.displayLabels[zone],
                    )
                if (legend != null) {
                    KeyLegendMark(
                        legend = legend,
                        fontSize = swipeFontSize,
                        iconSize = swipeSize,
                        color = swipeColor,
                        modifier = Modifier.align(alignment),
                    )
                }
            }
            val centerLegend =
                keyLegend(
                    mapping.intents[Zone.Center],
                    legendVisibility,
                    legendModifierState,
                    shiftMappings,
                    capsLockMappings,
                    displayLabel = mapping.displayLabels[Zone.Center],
                )
            if (centerLegend != null) {
                val isUpperCase =
                    (centerLegend as? KeyLegend.Text)?.text?.firstOrNull()?.isUpperCase() == true
                val centerSize =
                    legendFontSize(isCenter = true, legendKeySize, isUpperCase)
                KeyLegendMark(
                    legend = centerLegend,
                    fontSize = with(density) { centerSize.toSp() },
                    iconSize = centerSize,
                    color = centerColor,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
        KeyReleaseEffects(
            releasedGlyph = releasedGlyph,
            animations = animations,
            isPasswordField = isPasswordField,
            keyHeight = keyHeight,
        )
    }
}

private class ReleasedGlyph(
    val text: String,
)

@Composable
private fun KeyReleaseEffects(
    releasedGlyph: MutableState<ReleasedGlyph?>,
    animations: KeyAnimationSettings,
    isPasswordField: Boolean,
    keyHeight: Dp,
) {
    val glyph = releasedGlyph.value
    val showRelease = glyph != null && !isPasswordField
    LaunchedEffect(glyph) {
        if (glyph == null) return@LaunchedEffect
        delay(DEFAULT_ANIMATION_HELPER_SPEED.toLong())
        if (releasedGlyph.value === glyph) {
            releasedGlyph.value = null
        }
    }
    if (animations.releaseFlash) {
        AnimatedVisibility(
            modifier = Modifier.fillMaxSize(),
            visible = showRelease,
            enter = EnterTransition.None,
            exit = fadeOut(tween(DEFAULT_ANIMATION_SPEED)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
            )
        }
    }
    if (animations.letterDrop) {
        AnimatedVisibility(
            modifier = Modifier.fillMaxSize(),
            visible = showRelease,
            enter = slideInVertically(tween(DEFAULT_ANIMATION_SPEED)),
            exit = fadeOut(tween(DEFAULT_ANIMATION_SPEED)),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                glyph?.let { shown ->
                    val dropSize =
                        legendFontSize(isCenter = true, keyHeight, isUpperCase = false)
                    val density = LocalDensity.current
                    Text(
                        text = shown.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = with(density) { dropSize.toSp() },
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

private val DIRECTIONAL_ALIGNMENTS =
    listOf(
        Direction.UP_LEFT to Alignment.TopStart,
        Direction.UP to Alignment.TopCenter,
        Direction.UP_RIGHT to Alignment.TopEnd,
        Direction.LEFT to Alignment.CenterStart,
        Direction.RIGHT to Alignment.CenterEnd,
        Direction.DOWN_LEFT to Alignment.BottomStart,
        Direction.DOWN to Alignment.BottomCenter,
        Direction.DOWN_RIGHT to Alignment.BottomEnd,
    )

/**
 * Space multitaps only advance on a plain SPACE [Gesture.Tap]. Slides and hold-repeat reset the
 * cycle so they keep inserting plain spaces / moving the cursor.
 */
private fun resolveSpacebarMultitap(
    gesture: Gesture,
    action: SemanticAction,
    tracker: SpacebarMultitapTracker?,
    enabled: Boolean,
    cycle: List<String>? = null,
): SemanticAction {
    if (tracker == null) return action
    when (gesture) {
        is Gesture.SlideStep -> {
            tracker.reset()
            return action
        }
        is Gesture.Tap -> {
            if (action.isPlainSpaceTap()) {
                return tracker.onSpaceTap(enabled, cycle)
            }
            tracker.noteOtherAction(action)
            return action
        }
        else -> {
            if (action.isPlainSpaceTap()) {
                tracker.reset()
            } else {
                tracker.noteOtherAction(action)
            }
            return action
        }
    }
}
