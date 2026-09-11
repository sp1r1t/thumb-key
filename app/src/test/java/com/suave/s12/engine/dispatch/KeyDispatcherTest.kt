package com.suave.s12.engine.dispatch

import com.suave.s12.engine.action.SemanticAction
import com.suave.s12.engine.feedback.FeedbackEvent
import com.suave.s12.engine.gesture.Direction
import com.suave.s12.engine.gesture.Gesture
import com.suave.s12.engine.gesture.GestureConfig
import com.suave.s12.engine.gesture.SlideAxis
import com.suave.s12.engine.gesture.SwipeDirections
import com.suave.s12.engine.gesture.Zone
import com.suave.s12.engine.intent.CommandId
import com.suave.s12.engine.intent.KeyIntent
import com.suave.s12.engine.intent.KeyMapping
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.engine.intent.SlideBehavior
import com.suave.s12.engine.modifier.ActivationMode
import com.suave.s12.engine.modifier.ModifierState
import com.suave.s12.utils.KeyAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val CONFIG = GestureConfig(minSwipeDistancePx = 64f, directions = SwipeDirections.FOUR_WAY)

/** Mirrors SUAVE_LAYOUT's (3,0): Ctrl on center, Alt on the right swipe, Esc on the up swipe. */
private val CTRL_ALT_ESC_KEY =
    KeyMapping(
        gestureConfig = CONFIG,
        intents =
            mapOf(
                Zone.Center to KeyIntent.ModifierPress(ModifierId.CTRL),
                Zone.Directional(Direction.RIGHT) to KeyIntent.ModifierPress(ModifierId.ALT),
                Zone.Directional(Direction.UP) to KeyIntent.ModifierPress(ModifierId.ESC),
            ),
    )

class KeyDispatcherTest {
    @Test
    fun `swiping to Alt on a Ctrl-Alt-Esc key activates Alt, not Ctrl`() {
        val dispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY)
        val feedback = mutableListOf<FeedbackEvent>()

        val state =
            dispatcher.handle(
                Gesture.Tap(Zone.Directional(Direction.RIGHT)),
                ModifierState(),
                onExecute = {},
                onLegacyAction = {},
                onFeedback = feedback::add,
            )

        assertTrue(state.isActive(ModifierId.ALT))
        assertFalse(state.isActive(ModifierId.CTRL))
        assertEquals(listOf(FeedbackEvent.ModifierActivated(ModifierId.ALT, ActivationMode.ONE_SHOT)), feedback)
    }

    @Test
    fun `releasing after swiping to Alt deactivates Alt via the engaged-modifier it locked onto, not Ctrl`() {
        val dispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY)

        // Hold-swipe to Alt: locks Alt as HELD.
        var state = dispatcher.handle(Gesture.Hold(Zone.Directional(Direction.RIGHT)), ModifierState(), {}, {}, {})
        assertEquals(ActivationMode.HELD, state.active.getValue(ModifierId.ALT).mode)

        // Release: must transition ALT (the zone this press actually engaged) to the one-shot
        // grace state, and must never have touched CTRL at all - this is the exact bug class
        // from the old engine, where the center action fired unconditionally on press-down
        // regardless of which zone the swipe eventually locked.
        state = dispatcher.handle(Gesture.Released, state, {}, {}, {})

        assertTrue(state.isActive(ModifierId.ALT))
        assertEquals(ActivationMode.ONE_SHOT, state.active.getValue(ModifierId.ALT).mode)
        assertFalse("Ctrl must never have been touched by a press that locked onto the Alt zone", state.isActive(ModifierId.CTRL))
    }

    @Test
    fun `swiping to Esc then releasing without ever holding still leaves Esc as a clean one-shot`() {
        val dispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY)

        var state = dispatcher.handle(Gesture.Tap(Zone.Directional(Direction.UP)), ModifierState(), {}, {}, {})
        state = dispatcher.handle(Gesture.Released, state, {}, {}, {})

        assertTrue("a quick swipe-tap to Esc should still be a one-shot activation", state.isActive(ModifierId.ESC))
        assertEquals(ActivationMode.ONE_SHOT, state.active.getValue(ModifierId.ESC).mode)
    }

    @Test
    fun `a plain character key executes and reports tap feedback, not modifier feedback`() {
        val key = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.Text("a")))
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()
        val feedback = mutableListOf<FeedbackEvent>()

        dispatcher.handle(Gesture.Tap(Zone.Center), ModifierState(), executed::add, {}, feedback::add)

        assertEquals(listOf(SemanticAction.TypeText("a")), executed)
        assertEquals(listOf(FeedbackEvent.TapRecognized), feedback)
    }

    @Test
    fun `a legacy action key fires once on tap and not again on hold-repeat`() {
        val key = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.LegacyAction(KeyAction.Copy)))
        val dispatcher = KeyDispatcher(key)
        val fired = mutableListOf<KeyAction>()

        dispatcher.handle(Gesture.Hold(Zone.Center), ModifierState(), {}, fired::add, {})
        dispatcher.handle(Gesture.HoldRepeat(Zone.Center), ModifierState(), {}, fired::add, {})
        dispatcher.handle(Gesture.HoldRepeat(Zone.Center), ModifierState(), {}, fired::add, {})

        assertEquals(listOf(KeyAction.Copy), fired)
    }

    @Test
    fun `backspace-style select-and-delete slide sends one Backspace command on release, not per slide step`() {
        val key =
            KeyMapping(
                CONFIG,
                mapOf(Zone.Center to KeyIntent.Command(CommandId.BACKSPACE)),
                slideBehavior = SlideBehavior.SELECT_AND_DELETE,
            )
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()

        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), ModifierState(), executed::add, {}, {})
        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), ModifierState(), executed::add, {}, {})
        dispatcher.handle(Gesture.Released, ModifierState(), executed::add, {}, {})

        assertEquals(2, executed.count { it is SemanticAction.ExtendSelection })
        assertEquals(1, executed.count { it == SemanticAction.TypeCommand(CommandId.BACKSPACE) })
    }
}
