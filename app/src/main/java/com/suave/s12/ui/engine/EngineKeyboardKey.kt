package com.suave.s12.ui.engine

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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
import com.suave.s12.engine.intent.CommandId
import com.suave.s12.engine.intent.KeyIntent
import com.suave.s12.engine.intent.KeyMapping
import com.suave.s12.engine.modifier.ModifierState
import com.suave.s12.utils.KeyAction
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
    onLegacyAction: (KeyAction) -> Unit,
    onFeedback: (FeedbackEvent) -> Unit,
    shiftMappings: Map<String, String>,
    minSwipeDistancePx: Float,
    hideLetters: Boolean,
    modifier: Modifier = Modifier,
) {
    val dispatcher = remember(mapping, shiftMappings) { KeyDispatcher(mapping, shiftMappings) }

    // The pointer-input loop below is long-lived (keyed on Unit, never restarts), so it must
    // read every value that can change across recomposition through rememberUpdatedState -
    // capturing them directly would freeze it at whatever was true the first time this key was
    // composed. This is the exact staleness hazard the old engine's KeyboardKey.kt ran into.
    val currentModifierState by rememberUpdatedState(modifierState)
    val currentMinSwipeDistancePx by rememberUpdatedState(minSwipeDistancePx)
    val currentOnModifierStateChange by rememberUpdatedState(onModifierStateChange)
    val currentOnExecute by rememberUpdatedState(onExecute)
    val currentOnLegacyAction by rememberUpdatedState(onLegacyAction)
    val currentOnFeedback by rememberUpdatedState(onFeedback)

    val isModifierKeyActive =
        mapping.intents.values.any { it is KeyIntent.ModifierPress && modifierState.isActive(it.modifier) }
    val backgroundColor = if (isModifierKeyActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier =
            modifier
                .padding(2.dp)
                .background(backgroundColor)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val config = mapping.gestureConfig.copy(minSwipeDistancePx = currentMinSwipeDistancePx)
                        val recognizer = GestureRecognizer(config)

                        fun handle(gesture: Gesture) {
                            val newState =
                                dispatcher.handle(
                                    gesture,
                                    currentModifierState,
                                    currentOnExecute,
                                    currentOnLegacyAction,
                                    currentOnFeedback,
                                )
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
            val label = displayLabel(mapping.intents[Zone.Directional(direction)], hideLetters)
            if (label != null) {
                Text(label, modifier = Modifier.align(alignment), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val centerLabel = displayLabel(mapping.intents[Zone.Center], hideLetters)
        if (centerLabel != null) {
            Text(
                centerLabel,
                modifier = Modifier.align(Alignment.Center),
                fontSize = 18.sp,
                color = if (isModifierKeyActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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

private fun displayLabel(
    intent: KeyIntent?,
    hideLetters: Boolean,
): String? =
    when (intent) {
        null, KeyIntent.Noop -> null

        is KeyIntent.Text -> if (hideLetters) null else intent.text

        is KeyIntent.Command -> commandLabel(intent.id)

        is KeyIntent.ModifierPress -> intent.modifier.name.lowercase()

        // Phase 1 doesn't have per-action icons/labels for the bridged legacy actions yet.
        is KeyIntent.LegacyAction -> "•"
    }

private fun commandLabel(id: CommandId): String =
    when (id) {
        CommandId.ENTER -> "⏎"
        CommandId.TAB -> "⇥"
        CommandId.BACKSPACE -> "⌫"
        CommandId.DELETE_FORWARD -> "⌦"
        CommandId.SPACE -> "␣"
        CommandId.ARROW_LEFT -> "←"
        CommandId.ARROW_RIGHT -> "→"
        CommandId.ARROW_UP -> "↑"
        CommandId.ARROW_DOWN -> "↓"
    }
