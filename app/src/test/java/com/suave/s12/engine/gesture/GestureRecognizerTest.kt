package com.suave.s12.engine.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

private const val START_MS = 1_000L

private fun down(
    x: Float = 0f,
    y: Float = 0f,
    t: Long = START_MS,
) = RecognizerInput.Touch(TouchEvent(x, y, t, TouchPhase.DOWN))

private fun move(
    x: Float,
    y: Float,
    t: Long,
) = RecognizerInput.Touch(TouchEvent(x, y, t, TouchPhase.MOVE))

private fun up(t: Long) = RecognizerInput.Touch(TouchEvent(0f, 0f, t, TouchPhase.UP))

private fun cancel(t: Long) = RecognizerInput.Touch(TouchEvent(0f, 0f, t, TouchPhase.CANCEL))

private fun tick(t: Long) = RecognizerInput.Tick(t)

class GestureRecognizerTest {
    private val plainKeyConfig =
        GestureConfig(
            minSwipeDistancePx = 20f,
            directions = SwipeDirections.FOUR_WAY,
            longPressTimeoutMs = 400L,
            repeatIntervalMs = 60L,
        )

    @Test
    fun `touch-down emits Pressed immediately, before anything else about the press is known`() {
        val recognizer = GestureRecognizer(plainKeyConfig)
        val result = recognizer.process(down())

        assertEquals(listOf(Gesture.Pressed), result)
    }

    @Test
    fun `quick tap with no movement emits Tap on Center then Released`() {
        val recognizer = GestureRecognizer(plainKeyConfig)
        recognizer.process(down())
        val result = recognizer.process(up(START_MS + 100))

        assertEquals(listOf(Gesture.Tap(Zone.Center), Gesture.Released), result)
    }

    @Test
    fun `swipe past threshold emits SwipeLocked immediately, then taps that zone on release`() {
        val recognizer = GestureRecognizer(plainKeyConfig)
        recognizer.process(down())
        val duringMove = recognizer.process(move(x = 0f, y = -50f, t = START_MS + 50))
        val result = recognizer.process(up(START_MS + 100))

        // Fires the instant the zone locks, mid-drag, not just at the eventual Tap on release -
        // this is the feedback signal a UI layer buzzes on while the finger still has good
        // tactile contact with the screen (see Gesture.SwipeLocked's doc for why that matters).
        assertEquals(listOf(Gesture.SwipeLocked(Direction.UP)), duringMove)
        assertEquals(listOf(Gesture.Tap(Zone.Directional(Direction.UP)), Gesture.Released), result)
    }

    @Test
    fun `further movement after a zone locks is ignored`() {
        val recognizer = GestureRecognizer(plainKeyConfig)
        recognizer.process(down())
        recognizer.process(move(x = 0f, y = -50f, t = START_MS + 50)) // locks UP
        recognizer.process(move(x = 50f, y = 0f, t = START_MS + 80)) // would be RIGHT, ignored
        val result = recognizer.process(up(START_MS + 100))

        assertEquals(listOf(Gesture.Tap(Zone.Directional(Direction.UP)), Gesture.Released), result)
    }

    @Test
    fun `holding past the long-press timeout fires Hold then HoldRepeat on interval, no Tap on release`() {
        val recognizer = GestureRecognizer(plainKeyConfig)
        recognizer.process(down())

        val beforeTimeout = recognizer.process(tick(START_MS + 300))
        val atTimeout = recognizer.process(tick(START_MS + 400))
        val firstRepeat = recognizer.process(tick(START_MS + 460))
        val tooSoonForNextRepeat = recognizer.process(tick(START_MS + 480))
        val secondRepeat = recognizer.process(tick(START_MS + 520))
        val released = recognizer.process(up(START_MS + 600))

        assertEquals(emptyList<Gesture>(), beforeTimeout)
        assertEquals(listOf(Gesture.Hold(Zone.Center)), atTimeout)
        assertEquals(listOf(Gesture.HoldRepeat(Zone.Center)), firstRepeat)
        assertEquals(emptyList<Gesture>(), tooSoonForNextRepeat)
        assertEquals(listOf(Gesture.HoldRepeat(Zone.Center)), secondRepeat)
        assertEquals("no Tap once a hold already fired for this press", listOf(Gesture.Released), released)
    }

    @Test
    fun `holding a locked swipe zone repeats that same zone`() {
        val recognizer = GestureRecognizer(plainKeyConfig)
        recognizer.process(down())
        recognizer.process(move(x = 50f, y = 0f, t = START_MS + 50)) // locks RIGHT

        val hold = recognizer.process(tick(START_MS + 450))

        assertEquals(listOf(Gesture.Hold(Zone.Directional(Direction.RIGHT))), hold)
    }

    @Test
    fun `cancel emits only Cancelled`() {
        val recognizer = GestureRecognizer(plainKeyConfig)
        recognizer.process(down())
        val result = recognizer.process(cancel(START_MS + 50))

        assertEquals(listOf(Gesture.Cancelled), result)
    }

    @Test
    fun `events after release are ignored`() {
        val recognizer = GestureRecognizer(plainKeyConfig)
        recognizer.process(down())
        recognizer.process(up(START_MS + 50))
        val afterRelease = recognizer.process(tick(START_MS + 1000))

        assertEquals(emptyList<Gesture>(), afterRelease)
    }

    @Test
    fun `sliding a configured axis emits one SlideStep per step-size crossed, sign matches direction`() {
        val config =
            GestureConfig(
                minSwipeDistancePx = 20f,
                directions = SwipeDirections.FOUR_WAY,
                slideAxis = SlideAxis.HORIZONTAL,
                slideStepPx = 24f,
            )
        val recognizer = GestureRecognizer(config)
        recognizer.process(down())

        // Crosses the deadzone moving mostly horizontally, exactly one step-size -> commits to
        // sliding (not a swipe zone) and immediately emits one step, with no remainder.
        val firstMove = recognizer.process(move(x = 24f, y = 2f, t = START_MS + 30))
        // Another 24px to the right -> exactly one more step.
        val secondMove = recognizer.process(move(x = 48f, y = 2f, t = START_MS + 60))
        // 24px back to the left -> one negative step.
        val thirdMove = recognizer.process(move(x = 24f, y = 2f, t = START_MS + 90))
        val released = recognizer.process(up(START_MS + 120))

        assertEquals(listOf(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1)), firstMove)
        assertEquals(listOf(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1)), secondMove)
        assertEquals(listOf(Gesture.SlideStep(SlideAxis.HORIZONTAL, -1)), thirdMove)
        assertEquals("a committed slide never emits a Tap on release", listOf(Gesture.Released), released)
    }

    @Test
    fun `sub-step slide movement carries a remainder rather than resetting to zero each step`() {
        val config = GestureConfig(minSwipeDistancePx = 20f, slideAxis = SlideAxis.HORIZONTAL, slideStepPx = 24f)
        val recognizer = GestureRecognizer(config)
        recognizer.process(down())

        val firstMove = recognizer.process(move(x = 30f, y = 2f, t = START_MS + 30)) // 30px: one step, 6px remainder
        val secondMove = recognizer.process(move(x = 40f, y = 2f, t = START_MS + 60)) // +10px -> remainder 16, still no step
        val thirdMove = recognizer.process(move(x = 44f, y = 2f, t = START_MS + 90)) // +4px -> remainder 20, still no step
        val fourthMove = recognizer.process(move(x = 60f, y = 2f, t = START_MS + 120)) // +16px -> remainder 36 -> one more step

        assertEquals(listOf(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1)), firstMove)
        assertEquals(emptyList<Gesture>(), secondMove)
        assertEquals(emptyList<Gesture>(), thirdMove)
        assertEquals(listOf(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1)), fourthMove)
    }

    @Test
    fun `a short swipe on a slidable key still locks a discrete zone when it doesn't match the slide axis`() {
        val config =
            GestureConfig(
                minSwipeDistancePx = 20f,
                directions = SwipeDirections.FOUR_WAY,
                slideAxis = SlideAxis.HORIZONTAL,
            )
        val recognizer = GestureRecognizer(config)
        recognizer.process(down())
        recognizer.process(move(x = 2f, y = -50f, t = START_MS + 30)) // dominantly vertical
        val result = recognizer.process(up(START_MS + 60))

        assertEquals(listOf(Gesture.Tap(Zone.Directional(Direction.UP)), Gesture.Released), result)
    }
}
