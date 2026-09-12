package com.suave.s12.engine.feedback

import com.suave.s12.db.DEFAULT_VIBRATE_HOLD_REPEAT_TYPE
import com.suave.s12.db.DEFAULT_VIBRATE_MODIFIER_TYPE
import com.suave.s12.db.DEFAULT_VIBRATE_SLIDE_TYPE
import com.suave.s12.db.DEFAULT_VIBRATE_SWIPE_TYPE
import com.suave.s12.db.DEFAULT_VIBRATE_TAP_TYPE
import com.suave.s12.engine.gesture.Direction
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.engine.modifier.ActivationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedbackDispatcherTest {
    private class RecordingPlayer : HapticPlayer {
        var plays = 0
        var lastType: HapticType? = null

        override fun play(pattern: HapticPattern) {
            plays += 1
            lastType = pattern.type
        }
    }

    private fun settings(
        tap: HapticChannel = on(HapticType.KEYBOARD_TAP),
        swipe: HapticChannel = on(HapticType.KEYBOARD_TAP),
        slide: HapticChannel = on(HapticType.TEXT_HANDLE_MOVE),
        repeat: HapticChannel = on(HapticType.CLOCK_TICK),
        modifier: HapticChannel = on(HapticType.LONG_PRESS),
    ) = FeedbackSettings(tap, swipe, slide, repeat, modifier)

    private fun on(type: HapticType) = HapticChannel(enabled = true, type = type)

    private fun off(type: HapticType = HapticType.KEYBOARD_TAP) = HapticChannel(enabled = false, type = type)

    @Test
    fun `unknown stored ordinal falls back to keyboard tap`() {
        assertEquals(HapticType.KEYBOARD_TAP, hapticTypeFromDb(-1))
        assertEquals(HapticType.KEYBOARD_TAP, hapticTypeFromDb(99))
        assertEquals(HapticType.CLOCK_TICK, hapticTypeFromDb(HapticType.CLOCK_TICK.ordinal))
    }

    @Test
    fun `db default type ordinals match the haptic catalog`() {
        assertEquals(HapticType.KEYBOARD_TAP.ordinal, DEFAULT_VIBRATE_TAP_TYPE)
        assertEquals(HapticType.KEYBOARD_TAP.ordinal, DEFAULT_VIBRATE_SWIPE_TYPE)
        assertEquals(HapticType.TEXT_HANDLE_MOVE.ordinal, DEFAULT_VIBRATE_SLIDE_TYPE)
        assertEquals(HapticType.CLOCK_TICK.ordinal, DEFAULT_VIBRATE_HOLD_REPEAT_TYPE)
        assertEquals(HapticType.LONG_PRESS.ordinal, DEFAULT_VIBRATE_MODIFIER_TYPE)
    }

    @Test
    fun `repeat ticks follow the hold-repeat channel, not tap`() {
        val player = RecordingPlayer()

        FeedbackDispatcher.dispatch(
            FeedbackEvent.RepeatTick,
            settings(tap = on(HapticType.KEYBOARD_TAP), repeat = off()),
            player,
        )
        assertEquals(0, player.plays)

        FeedbackDispatcher.dispatch(
            FeedbackEvent.RepeatTick,
            settings(tap = off(), repeat = on(HapticType.CLOCK_TICK)),
            player,
        )
        assertEquals(1, player.plays)
        assertEquals(HapticType.CLOCK_TICK, player.lastType)
    }

    @Test
    fun `taps still follow the tap channel when hold-repeat is off`() {
        val player = RecordingPlayer()

        FeedbackDispatcher.dispatch(
            FeedbackEvent.TapRecognized,
            settings(tap = on(HapticType.VIRTUAL_KEY), repeat = off()),
            player,
        )
        assertEquals(1, player.plays)
        assertEquals(HapticType.VIRTUAL_KEY, player.lastType)

        FeedbackDispatcher.dispatch(
            FeedbackEvent.TapRecognized,
            settings(tap = off(), repeat = on(HapticType.CLOCK_TICK)),
            player,
        )
        assertEquals(1, player.plays)
    }

    @Test
    fun `slides stay on their own channel`() {
        val player = RecordingPlayer()

        FeedbackDispatcher.dispatch(
            FeedbackEvent.SlideStep,
            settings(slide = off(), tap = on(HapticType.KEYBOARD_TAP), repeat = on(HapticType.CLOCK_TICK)),
            player,
        )
        assertEquals(0, player.plays)

        FeedbackDispatcher.dispatch(
            FeedbackEvent.SlideStep,
            settings(slide = on(HapticType.TEXT_HANDLE_MOVE), tap = off(), repeat = off()),
            player,
        )
        assertEquals(1, player.plays)
        assertEquals(HapticType.TEXT_HANDLE_MOVE, player.lastType)
    }

    @Test
    fun `swipe lock follows the swipe channel, not tap`() {
        val player = RecordingPlayer()
        val swipe = FeedbackEvent.SwipeLocked(Direction.UP)

        FeedbackDispatcher.dispatch(swipe, settings(tap = on(HapticType.KEYBOARD_TAP), swipe = off()), player)
        assertEquals(0, player.plays)

        FeedbackDispatcher.dispatch(
            swipe,
            settings(tap = off(), swipe = on(HapticType.CONTEXT_CLICK)),
            player,
        )
        assertEquals(1, player.plays)
        assertEquals(HapticType.CONTEXT_CLICK, player.lastType)
    }

    @Test
    fun `modifiers follow the modifier channel`() {
        val player = RecordingPlayer()
        val activate =
            FeedbackEvent.ModifierActivated(ModifierId.SHIFT, ActivationMode.ONE_SHOT)

        FeedbackDispatcher.dispatch(activate, settings(tap = on(HapticType.KEYBOARD_TAP), modifier = off()), player)
        assertEquals(0, player.plays)

        FeedbackDispatcher.dispatch(
            activate,
            settings(tap = off(), modifier = on(HapticType.LONG_PRESS)),
            player,
        )
        assertEquals(1, player.plays)
        assertEquals(HapticType.LONG_PRESS, player.lastType)

        FeedbackDispatcher.dispatch(
            FeedbackEvent.ModifierDeactivated(ModifierId.SHIFT),
            settings(modifier = on(HapticType.CONFIRM)),
            player,
        )
        assertEquals(HapticType.CONFIRM, player.lastType)
    }

    @Test
    fun `disabled channel does not play even if a type is stored`() {
        val player = RecordingPlayer()
        FeedbackDispatcher.dispatch(
            FeedbackEvent.TapRecognized,
            settings(tap = off(HapticType.REJECT)),
            player,
        )
        assertEquals(0, player.plays)
        assertNull(player.lastType)
    }
}
