package com.suave.s12.engine.gesture

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Pure, per-press gesture state machine. Create one instance when a key receives
 * [TouchPhase.DOWN], feed it every subsequent [RecognizerInput] for that same press (real touch
 * samples plus periodic time ticks so hold/repeat are driven by explicit, testable time rather
 * than a hidden timer), and discard it once it emits [Gesture.Released] or [Gesture.Cancelled].
 *
 * Deliberately has no notion of layout, layers, modifiers, or key identity - see the plan's
 * pipeline: this stage only turns a touch stream into a [Gesture]. Because each instance is
 * scoped to exactly one press and holds no reference to anything about what the key currently
 * renders, nothing about a modifier activating elsewhere can tear this down mid-gesture - unlike
 * the old engine, where toggling a modifier swapped the whole rendered keyset and could cancel
 * an in-flight Compose gesture-tracking block.
 */
class GestureRecognizer(
    private val config: GestureConfig,
) {
    private var originX = 0f
    private var originY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var startTimeMs = 0L
    private var lastHoldTickMs = 0L

    private var zone: Zone = Zone.Center
    private var zoneLocked = false
    private var sliding = false
    private var holdFired = false
    private var done = false

    private var slideAccumulator = 0f

    fun process(input: RecognizerInput): List<Gesture> {
        if (done) return emptyList()
        return when (input) {
            is RecognizerInput.Touch -> onTouch(input.event)
            is RecognizerInput.Tick -> onTick(input.nowMs)
        }
    }

    private fun onTouch(event: TouchEvent): List<Gesture> =
        when (event.phase) {
            TouchPhase.DOWN -> {
                originX = event.x
                originY = event.y
                lastX = event.x
                lastY = event.y
                startTimeMs = event.timeMs
                lastHoldTickMs = event.timeMs
                emptyList()
            }

            TouchPhase.MOVE -> {
                onMove(event)
            }

            TouchPhase.UP -> {
                onRelease()
            }

            TouchPhase.CANCEL -> {
                onCancel()
            }
        }

    private fun onMove(event: TouchEvent): List<Gesture> {
        if (sliding) return emitSlideSteps(event)

        if (zoneLocked || holdFired) {
            // Zone (or a fired hold on the center) already committed for this press; further
            // movement doesn't change it - no circular-drag or swipe-and-return support.
            lastX = event.x
            lastY = event.y
            return emptyList()
        }

        val totalDx = event.x - originX
        val totalDy = event.y - originY
        if (hypot(totalDx, totalDy) < config.minSwipeDistancePx) return emptyList()

        val slideAxis = config.slideAxis
        if (slideAxis != null && isDominantlyAlong(slideAxis, totalDx, totalDy)) {
            sliding = true
            lastX = originX
            lastY = originY
            return emitSlideSteps(event)
        }

        if (config.directions != SwipeDirections.NONE) {
            val direction = resolveDirection(totalDx, totalDy, config.directions)
            zone = Zone.Directional(direction)
            zoneLocked = true
            lastX = event.x
            lastY = event.y
            return listOf(Gesture.SwipeLocked(direction))
        }
        lastX = event.x
        lastY = event.y
        return emptyList()
    }

    private fun emitSlideSteps(event: TouchEvent): List<Gesture> {
        val axis = config.slideAxis ?: return emptyList()
        val delta = if (axis == SlideAxis.HORIZONTAL) event.x - lastX else event.y - lastY
        lastX = event.x
        lastY = event.y
        slideAccumulator += delta

        val steps = mutableListOf<Gesture>()
        while (abs(slideAccumulator) >= config.slideStepPx) {
            val step = if (slideAccumulator > 0) 1 else -1
            steps += Gesture.SlideStep(axis, step)
            slideAccumulator -= step * config.slideStepPx
        }
        return steps
    }

    private fun onTick(nowMs: Long): List<Gesture> {
        if (sliding) return emptyList()
        if (!holdFired) {
            if (nowMs - startTimeMs < config.longPressTimeoutMs) return emptyList()
            holdFired = true
            lastHoldTickMs = nowMs
            return listOf(Gesture.Hold(zone))
        }
        if (nowMs - lastHoldTickMs < config.repeatIntervalMs) return emptyList()
        lastHoldTickMs = nowMs
        return listOf(Gesture.HoldRepeat(zone))
    }

    private fun onRelease(): List<Gesture> {
        done = true
        return if (!holdFired && !sliding) {
            listOf(Gesture.Tap(zone), Gesture.Released)
        } else {
            listOf(Gesture.Released)
        }
    }

    private fun onCancel(): List<Gesture> {
        done = true
        return listOf(Gesture.Cancelled)
    }

    private fun isDominantlyAlong(
        axis: SlideAxis,
        dx: Float,
        dy: Float,
    ): Boolean =
        when (axis) {
            SlideAxis.HORIZONTAL -> abs(dx) > abs(dy)
            SlideAxis.VERTICAL -> abs(dy) > abs(dx)
        }
}

/**
 * Compass angle: 0deg = RIGHT, 90deg = UP, +-180deg = LEFT, -90deg = DOWN (screen Y grows
 * downward, so "up" is negative dy).
 */
private fun resolveDirection(
    dx: Float,
    dy: Float,
    directions: SwipeDirections,
): Direction {
    val angleDeg = Math.toDegrees(atan2(-dy, dx).toDouble())
    val normalized = (angleDeg + 360.0) % 360.0
    return when (directions) {
        SwipeDirections.NONE -> {
            error("resolveDirection called with SwipeDirections.NONE")
        }

        SwipeDirections.FOUR_WAY -> {
            when {
                normalized < 45 || normalized >= 315 -> Direction.RIGHT
                normalized < 135 -> Direction.UP
                normalized < 225 -> Direction.LEFT
                else -> Direction.DOWN
            }
        }

        SwipeDirections.EIGHT_WAY -> {
            when {
                normalized < 22.5 || normalized >= 337.5 -> Direction.RIGHT
                normalized < 67.5 -> Direction.UP_RIGHT
                normalized < 112.5 -> Direction.UP
                normalized < 157.5 -> Direction.UP_LEFT
                normalized < 202.5 -> Direction.LEFT
                normalized < 247.5 -> Direction.DOWN_LEFT
                normalized < 292.5 -> Direction.DOWN
                else -> Direction.DOWN_RIGHT
            }
        }
    }
}
