package com.suave.keyboard.engine.gesture

/**
 * Per-key gesture thresholds and swipe occupancy. One [GestureConfig] describes everything
 * [GestureRecognizer] needs about a single key's capabilities.
 *
 * [occupiedDirections] is a bit mask of which compass [Direction]s this key can lock onto
 * (see [occupiedSwipeMask] / docs/swipe-zone-inference.md). Empty means tap-only. Spacebar
 * and backspace set [slideAxis] as well: short off-axis movement still resolves to an occupied
 * swipe zone, and sustained movement along [slideAxis] becomes a slide instead.
 * [SlideAxis.BOTH] (spacebar) slides on whichever axis is dominant once the threshold is
 * crossed.
 */
data class GestureConfig(
    val minSwipeDistancePx: Float,
    val occupiedDirections: SwipeMask = 0,
    val slideAxis: SlideAxis? = null,
    val slideStepPx: Float = 24f,
    val longPressTimeoutMs: Long = 400L,
    val repeatIntervalMs: Long = 60L,
)
