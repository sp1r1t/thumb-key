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

/** The axis a slidable key (spacebar/backspace) reports continuous movement along.
 *  [BOTH] is a config value: once the swipe threshold is crossed, the recognizer locks onto
 *  whichever of [HORIZONTAL] or [VERTICAL] was dominant. [Gesture.SlideStep] always carries
 *  the locked axis, never [BOTH]. */
enum class SlideAxis { HORIZONTAL, VERTICAL, BOTH }

/**
 * A recognized gesture, emitted by [GestureRecognizer]. Deliberately small: tap and swipe are
 * the same shape (a [Zone]-qualified [Tap]), hold/hold-repeat apply uniformly to any zone
 * (including a locked swipe direction), and [Released] is emitted exactly once per press
 * regardless of what else fired — callers that only care "did the physical key let go" (the
 * modifier engine's Held activation) never need to infer it from Tap/Hold.
 */
sealed class Gesture {
    /**
     * Fired once, immediately on touch-down, before anything about this press is known - not
     * just for a plain tap, for every press regardless of what it turns into (swipe, hold,
     * slide, a modifier key). Purely a "something was touched" feedback signal: acknowledges
     * the press itself, independent of whatever else fires later for what the press resolves
     * to (e.g. [SwipeLocked] for a swipe that follows it).
     */
    object Pressed : Gesture()

    /**
     * Fired once, the instant a swipe locks onto a compass direction - mid-drag, while the
     * finger still has good tactile contact with the screen. Purely an early feedback signal,
     * additional to [Pressed] (a swipe should feel like two distinct buzzes: one on press, one
     * when the swipe registers) - the actual committed intent for this zone still resolves
     * later via [Tap]/[Hold] at release, same as ever. Firing feedback only at that later
     * commit (as this used to) meant swipe feedback landed right as the finger was lifting off
     * - the worst possible moment to feel a buzz, which is why it read as "swipe never
     * vibrates" even though the call was succeeding every time.
     */
    data class SwipeLocked(
        val direction: Direction,
    ) : Gesture()

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
