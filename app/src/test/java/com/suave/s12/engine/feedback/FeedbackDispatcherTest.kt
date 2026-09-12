package com.suave.s12.engine.feedback

import org.junit.Assert.assertEquals
import org.junit.Test

class FeedbackDispatcherTest {
    private class RecordingPlayer : HapticPlayer {
        var plays = 0

        override fun play(pattern: HapticPattern) {
            plays += 1
        }
    }

    private fun settings(
        tap: Boolean = true,
        slide: Boolean = true,
        holdRepeat: Boolean = true,
    ) = FeedbackSettings(
        tapVibrationEnabled = tap,
        slideVibrationEnabled = slide,
        holdRepeatVibrationEnabled = holdRepeat,
        baseDurationMs = 25L,
        baseAmplitude = 130,
    )

    @Test
    fun `repeat ticks follow the hold-repeat toggle, not tap`() {
        val player = RecordingPlayer()

        FeedbackDispatcher.dispatch(FeedbackEvent.RepeatTick, settings(tap = true, holdRepeat = false), player)
        assertEquals(0, player.plays)

        FeedbackDispatcher.dispatch(FeedbackEvent.RepeatTick, settings(tap = false, holdRepeat = true), player)
        assertEquals(1, player.plays)
    }

    @Test
    fun `taps still follow the tap toggle when hold-repeat is off`() {
        val player = RecordingPlayer()

        FeedbackDispatcher.dispatch(FeedbackEvent.TapRecognized, settings(tap = true, holdRepeat = false), player)
        assertEquals(1, player.plays)

        FeedbackDispatcher.dispatch(FeedbackEvent.TapRecognized, settings(tap = false, holdRepeat = true), player)
        assertEquals(1, player.plays)
    }

    @Test
    fun `slides stay on their own toggle`() {
        val player = RecordingPlayer()

        FeedbackDispatcher.dispatch(FeedbackEvent.SlideStep, settings(slide = false, holdRepeat = true, tap = true), player)
        assertEquals(0, player.plays)

        FeedbackDispatcher.dispatch(FeedbackEvent.SlideStep, settings(slide = true, holdRepeat = false, tap = false), player)
        assertEquals(1, player.plays)
    }
}
