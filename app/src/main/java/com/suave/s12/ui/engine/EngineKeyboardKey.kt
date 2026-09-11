package com.suave.s12.ui.engine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suave.s12.engine.action.SemanticAction
import com.suave.s12.engine.dispatch.KeyDispatcher
import com.suave.s12.engine.feedback.FeedbackEvent
import com.suave.s12.engine.gesture.Direction
import com.suave.s12.engine.gesture.Gesture
import com.suave.s12.engine.gesture.GestureRecognizer
import com.suave.s12.engine.gesture.RecognizerInput
import com.suave.s12.engine.gesture.TouchEvent
import com.suave.s12.engine.gesture.TouchPhase
import com.suave.s12.engine.gesture.Zone
import com.suave.s12.engine.intent.KeyIntent
import com.suave.s12.engine.intent.KeyMapping
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.engine.modifier.ModifierBehavior
import com.suave.s12.engine.modifier.ModifierState
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
    modifierState: ModifierState,
    onModifierStateChange: (ModifierState) -> Unit,
    onExecute: (SemanticAction) -> Unit,
    onFeedback: (FeedbackEvent) -> Unit,
    shiftMappings: Map<String, String>,
    minSwipeDistancePx: Float,
    hideLetters: Boolean,
    modifierBehaviors: Map<ModifierId, ModifierBehavior>,
    keyPadding: Int,
    keyBorderWidthDp: Float,
    keyCornerRadius: Dp,
    modifier: Modifier = Modifier,
) {
    val dispatcher =
        remember(mapping, shiftMappings, modifierBehaviors) { KeyDispatcher(mapping, shiftMappings, modifierBehaviors) }

    // Values that change across recompositions of the *same* mapping are read through
    // rememberUpdatedState. The loop itself restarts when [mapping] changes: numeric/main
    // reuse the same composed key slots, and a loop keyed on Unit would keep dispatching the
    // letter-key intents after the labels had already switched.
    val currentModifierState by rememberUpdatedState(modifierState)
    val currentMinSwipeDistancePx by rememberUpdatedState(minSwipeDistancePx)
    val currentOnModifierStateChange by rememberUpdatedState(onModifierStateChange)
    val currentOnExecute by rememberUpdatedState(onExecute)
    val currentOnFeedback by rememberUpdatedState(onFeedback)
    val currentDispatcher by rememberUpdatedState(dispatcher)
    val currentMapping by rememberUpdatedState(mapping)

    val isModifierKeyActive =
        mapping.intents.values.any { it is KeyIntent.ModifierPress && modifierState.isActive(it.modifier) }
    val backgroundColor = if (isModifierKeyActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val keyShape = RoundedCornerShape(keyCornerRadius)
    val keyBorderColour = MaterialTheme.colorScheme.outline

    Box(
        modifier =
            modifier
                .padding(keyPadding.dp)
                .clip(keyShape)
                .then(
                    if (keyBorderWidthDp > 0f) {
                        Modifier.border(keyBorderWidthDp.dp, keyBorderColour, keyShape)
                    } else {
                        Modifier
                    },
                )
                .background(backgroundColor)
                .pointerInput(mapping) {
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

                        // Seeded fresh per press from the latest cross-key state, then tracked
                        // locally for the rest of THIS press - not re-read from
                        // currentModifierState on every call. A single press can emit several
                        // gestures in one synchronous batch (onRelease returns [Tap, Released]
                        // together), and Compose's snapshot-state write from the first call's
                        // onModifierStateChange doesn't reach currentModifierState until the
                        // next recomposition, which hasn't happened yet by the time the second
                        // gesture in the same batch runs. Re-reading the stale value there let
                        // Released (which passes a non-modifier key's input state straight
                        // through unchanged) silently resurrect whatever Tap had just cleared a
                        // microsecond earlier - this was the actual "Ctrl gets stuck" bug.
                        var localState = currentModifierState

                        fun handle(gesture: Gesture) {
                            val before = localState
                            val newState =
                                currentDispatcher.handle(
                                    gesture,
                                    before,
                                    currentOnExecute,
                                    currentOnFeedback,
                                )
                            localState = newState
                            currentOnModifierStateChange(newState)
                        }

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
                            if (change == null || !change.pressed) {
                                pressed = false
                                val phase = if (change == null) TouchPhase.CANCEL else TouchPhase.UP
                                val position = change?.position ?: down.position
                                recognizer
                                    .process(
                                        RecognizerInput.Touch(TouchEvent(position.x, position.y, System.currentTimeMillis(), phase)),
                                    ).forEach(::handle)
                                change?.consume()
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
                    }
                },
    ) {
        for ((direction, alignment) in DIRECTIONAL_ALIGNMENTS) {
            val legend =
                keyLegend(
                    mapping.intents[Zone.Directional(direction)],
                    hideLetters,
                    modifierState,
                    shiftMappings,
                )
            if (legend != null) {
                KeyLegendMark(
                    legend = legend,
                    fontSize = 10.sp,
                    iconSize = 12.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(alignment),
                )
            }
        }
        val centerLegend =
            keyLegend(
                mapping.intents[Zone.Center],
                hideLetters,
                modifierState,
                shiftMappings,
            )
        if (centerLegend != null) {
            KeyLegendMark(
                legend = centerLegend,
                fontSize = 18.sp,
                iconSize = 22.dp,
                color = if (isModifierKeyActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun KeyLegendMark(
    legend: KeyLegend,
    fontSize: TextUnit,
    iconSize: Dp,
    color: Color,
    modifier: Modifier,
) {
    when (legend) {
        is KeyLegend.Text -> {
            Text(legend.text, modifier = modifier, fontSize = fontSize, color = color)
        }
        is KeyLegend.Icon -> {
            Icon(
                imageVector = legend.icon,
                contentDescription = legend.icon.name,
                tint = color,
                modifier = modifier.size(iconSize),
            )
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
