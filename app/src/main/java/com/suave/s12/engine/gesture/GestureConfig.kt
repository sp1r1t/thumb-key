package com.suave.s12.engine.gesture

/** How many compass zones a key's swipe locks onto, or none for a plain tap-only key. */
enum class SwipeDirections { NONE, FOUR_WAY, EIGHT_WAY }

/**
 * Per-key gesture thresholds. One [GestureConfig] describes everything [GestureRecognizer]
 * needs to know about a single key's capabilities; a plain character key sets [directions] and
 * leaves [slideAxis] null, spacebar/backspace set both (short swipe still resolves to a
 * [SwipeDirections] zone within [minSwipeDistancePx]; sustained movement along [slideAxis]
 * becomes a slide instead).
 */
data class GestureConfig(
    val minSwipeDistancePx: Float,
    val directions: SwipeDirections = SwipeDirections.NONE,
    val slideAxis: SlideAxis? = null,
    val slideStepPx: Float = 24f,
    val longPressTimeoutMs: Long = 400L,
    val repeatIntervalMs: Long = 60L,
)
