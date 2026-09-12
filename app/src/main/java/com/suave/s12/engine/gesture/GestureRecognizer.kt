package com.suave.s12.engine.gesture

import kotlin.math.abs
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
    private var slidingAxis: SlideAxis? = null
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
                listOf(Gesture.Pressed)
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
        if (slidingAxis != null) return emitSlideSteps(event)

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

        val lockedAxis = lockSlideAxis(config.slideAxis, totalDx, totalDy)
        if (lockedAxis != null) {
            slidingAxis = lockedAxis
            lastX = originX
            lastY = originY
            return emitSlideSteps(event)
        }

        if (config.occupiedDirections != 0) {
            val direction = resolveSwipeDirection(totalDx, totalDy, config.occupiedDirections)
            if (direction != null) {
                zone = Zone.Directional(direction)
                zoneLocked = true
                lastX = event.x
                lastY = event.y
                return listOf(Gesture.SwipeLocked(direction))
            }
            // Past threshold but angle is unclaimed: do not lock, do not buzz. Further movement
            // may still lock if the finger enters an occupied wedge; release stays Center.
        }
        lastX = event.x
        lastY = event.y
        return emptyList()
    }

    private fun emitSlideSteps(event: TouchEvent): List<Gesture> {
        val axis = slidingAxis ?: return emptyList()
        val delta =
            when (axis) {
                SlideAxis.HORIZONTAL -> event.x - lastX
                SlideAxis.VERTICAL -> event.y - lastY
                SlideAxis.BOTH -> 0f
            }
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
        if (slidingAxis != null) return emptyList()
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
        return if (!holdFired && slidingAxis == null) {
            listOf(Gesture.Tap(zone), Gesture.Released)
        } else {
            listOf(Gesture.Released)
        }
    }

    private fun onCancel(): List<Gesture> {
        done = true
        return listOf(Gesture.Cancelled)
    }

    private fun lockSlideAxis(
        configured: SlideAxis?,
        dx: Float,
        dy: Float,
    ): SlideAxis? =
        when (configured) {
            null -> null
            SlideAxis.HORIZONTAL -> if (abs(dx) > abs(dy)) SlideAxis.HORIZONTAL else null
            SlideAxis.VERTICAL -> if (abs(dy) > abs(dx)) SlideAxis.VERTICAL else null
            SlideAxis.BOTH -> if (abs(dx) >= abs(dy)) SlideAxis.HORIZONTAL else SlideAxis.VERTICAL
        }
}

