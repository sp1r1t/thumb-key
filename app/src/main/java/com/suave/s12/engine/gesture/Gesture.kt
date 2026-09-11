package com.suave.s12.engine.gesture

/** Compass direction a swipe locked onto. */
enum class Direction { UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT }

/** Which zone of a key a tap/hold resolved to: the center, or a locked swipe direction. */
sealed class Zone {
    object Center : Zone()

    data class Directional(
        val direction: Direction,
    ) : Zone()
}

/** The axis a slidable key (spacebar/backspace) reports continuous movement along. */
enum class SlideAxis { HORIZONTAL, VERTICAL }

/**
 * A recognized gesture, emitted by [GestureRecognizer]. Deliberately small: tap and swipe are
 * the same shape (a [Zone]-qualified [Tap]), hold/hold-repeat apply uniformly to any zone
 * (including a locked swipe direction), and [Released] is emitted exactly once per press
 * regardless of what else fired — callers that only care "did the physical key let go" (the
 * modifier engine's Held activation) never need to infer it from Tap/Hold.
 */
sealed class Gesture {
    data class Tap(
        val zone: Zone,
    ) : Gesture()

    data class Hold(
        val zone: Zone,
    ) : Gesture()

    data class HoldRepeat(
        val zone: Zone,
    ) : Gesture()

    /** [steps] is the signed step count since the last SlideStep for this press (usually ±1). */
    data class SlideStep(
        val axis: SlideAxis,
        val steps: Int,
    ) : Gesture()

    /** The physical touch ended normally. Always the last event for a press, exactly once. */
    object Released : Gesture()

    /** The platform cancelled the touch (e.g. a system gesture took over) rather than lifting it. */
    object Cancelled : Gesture()
}
