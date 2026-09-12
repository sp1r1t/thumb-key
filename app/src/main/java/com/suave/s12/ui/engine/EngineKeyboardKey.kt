package com.suave.s12.ui.engine

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.suave.s12.db.DEFAULT_ANIMATION_HELPER_SPEED
import com.suave.s12.db.DEFAULT_ANIMATION_SPEED
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
import com.suave.s12.utils.ColorVariant
import com.suave.s12.utils.colorVariantToColor
import com.suave.s12.utils.fontSizeVariantToFontSize
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
    modifierState: ModifierState,
    onModifierStateChange: (ModifierState) -> Unit,
    onExecute: (SemanticAction) -> Unit,
    onFeedback: (FeedbackEvent) -> Unit,
    shiftMappings: Map<String, String>,
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
    val currentAnimations by rememberUpdatedState(animations)
    val currentIsPasswordField by rememberUpdatedState(isPasswordField)
    // MutableState (not `by`) so press/release visuals are read only in draw / a child. Writing
    // them from the pointer loop used to recompose this key mid-gesture: the highlight swapped
    // Modifier.background, legends relaid out, and a slightly slow Shift+letter crossed the
    // hold-repeat threshold as two characters.
    val isPressed = remember { mutableStateOf(false) }
    val releasedGlyph = remember { mutableStateOf<ReleasedGlyph?>(null) }

    val isModifierKeyActive =
        mapping.intents.values.any { it is KeyIntent.ModifierPress && modifierState.isActive(it.modifier) }
    val restingColor =
        if (isModifierKeyActive) {
            MaterialTheme.colorScheme.primary
        } else {
            colorVariantToColor(ColorVariant.SURFACE_VARIANT)
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
    val swipeSize = fontSizeVariantToFontSize(legendFontSizeVariant(isCenter = false), keyHeight, isUpperCase = false)
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
                            if (gesture is Gesture.Pressed && currentAnimations.pressHighlight) {
                                isPressed.value = true
                            }
                            val before = localState
                            var typed: String? = null
                            val newState =
                                currentDispatcher.handle(
                                    gesture,
                                    before,
                                    { action ->
                                        if (currentAnimations.playsRelease &&
                                            !currentIsPasswordField &&
                                            (gesture is Gesture.Tap || gesture is Gesture.Hold)
                                        ) {
                                            typedTextForReleaseAnimation(action)?.let { typed = it }
                                        }
                                        currentOnExecute(action)
                                    },
                                    currentOnFeedback,
                                )
                            localState = newState
                            if (newState != before) {
                                currentOnModifierStateChange(newState)
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
                val legend =
                    keyLegend(
                        mapping.intents[Zone.Directional(direction)],
                        legendVisibility,
                        modifierState,
                        shiftMappings,
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
                    modifierState,
                    shiftMappings,
                )
            if (centerLegend != null) {
                val isUpperCase =
                    (centerLegend as? KeyLegend.Text)?.text?.firstOrNull()?.isUpperCase() == true
                val centerSize =
                    fontSizeVariantToFontSize(legendFontSizeVariant(isCenter = true), keyHeight, isUpperCase)
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
                        fontSizeVariantToFontSize(legendFontSizeVariant(isCenter = true), keyHeight, isUpperCase = false)
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
