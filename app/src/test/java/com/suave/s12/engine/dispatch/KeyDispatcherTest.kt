package com.suave.s12.engine.dispatch

import com.suave.s12.engine.action.CursorDirection
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

private val LETTER_KEY = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.Text("s")))

class KeyDispatcherTest {
    @Test
    fun `hold Ctrl, fire a command, release Ctrl - deactivates immediately, not a one-shot grace key`() {
        val ctrlDispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY)
        val letterDispatcher = KeyDispatcher(LETTER_KEY)
        val executed = mutableListOf<SemanticAction>()

        var state = ctrlDispatcher.handle(Gesture.Hold(Zone.Center), ModifierState(), executed::add, {}, {})
        state = letterDispatcher.handle(Gesture.Pressed, state, executed::add, {}, {})
        state = letterDispatcher.handle(Gesture.Tap(Zone.Center), state, executed::add, {}, {})
        state = letterDispatcher.handle(Gesture.Released, state, executed::add, {}, {})
        state = ctrlDispatcher.handle(Gesture.Released, state, executed::add, {}, {})

        assertEquals(listOf(SemanticAction.TypeText("s", setOf(ModifierId.CTRL))), executed)
        assertFalse("holding Ctrl through a command then releasing must deactivate it outright", state.isActive(ModifierId.CTRL))
    }

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
        // No feedback from a bare Tap call in this test - in real use Gesture.Pressed/
        // SwipeLocked already buzz for this exact moment before Tap ever runs; firing
        // ModifierActivated here too was the "too many vibrations" bug.
        assertEquals(emptyList<FeedbackEvent>(), feedback)
    }

    @Test
    fun `releasing after swiping to Alt deactivates Alt via the engaged-modifier it locked onto, not Ctrl`() {
        val dispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY)

        // Hold-swipe to Alt: locks Alt as HELD.
        var state = dispatcher.handle(Gesture.Hold(Zone.Directional(Direction.RIGHT)), ModifierState(), {}, {}, {})
        assertEquals(ActivationMode.HELD, state.active.getValue(ModifierId.ALT).mode)

        // Release: must deactivate ALT (the zone this press actually engaged), immediately, and
        // must never have touched CTRL at all - this is the exact bug class from the old engine,
        // where the center action fired unconditionally on press-down regardless of which zone
        // the swipe eventually locked.
        state = dispatcher.handle(Gesture.Released, state, {}, {}, {})

        assertFalse(state.isActive(ModifierId.ALT))
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
    fun `a plain character key executes its text and fires no feedback on its own (Pressed already covered it)`() {
        val key = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.Text("a")))
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()
        val feedback = mutableListOf<FeedbackEvent>()

        dispatcher.handle(Gesture.Tap(Zone.Center), ModifierState(), executed::add, {}, feedback::add)

        assertEquals(listOf(SemanticAction.TypeText("a")), executed)
        assertEquals(emptyList<FeedbackEvent>(), feedback)
    }

    @Test
    fun `a full swipe press buzzes exactly twice - once on press, once on swipe lock - never a third time on commit`() {
        val key =
            KeyMapping(
                CONFIG,
                mapOf(Zone.Center to KeyIntent.Text("o"), Zone.Directional(Direction.UP) to KeyIntent.Text("1")),
            )
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()
        val feedback = mutableListOf<FeedbackEvent>()

        // Touch-down - buzz #1, before anything about this press is known.
        dispatcher.handle(Gesture.Pressed, ModifierState(), executed::add, {}, feedback::add)
        // The lock, mid-drag - buzz #2, nothing executes yet.
        dispatcher.handle(Gesture.SwipeLocked(Direction.UP), ModifierState(), executed::add, {}, feedback::add)
        // The eventual commit on release - executes the character, but no third buzz: that
        // would land right as the finger lifts, the worst moment to feel it.
        dispatcher.handle(Gesture.Tap(Zone.Directional(Direction.UP)), ModifierState(), executed::add, {}, feedback::add)

        assertEquals(listOf(FeedbackEvent.TapRecognized, FeedbackEvent.SwipeLocked(Direction.UP)), feedback)
        assertEquals(listOf(SemanticAction.TypeText("1")), executed)
    }

    @Test
    fun `a plain tap (no swipe) buzzes exactly once, on press, not again on commit`() {
        val key = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.Text("o")))
        val dispatcher = KeyDispatcher(key)
        val feedback = mutableListOf<FeedbackEvent>()

        dispatcher.handle(Gesture.Pressed, ModifierState(), {}, {}, feedback::add)
        dispatcher.handle(Gesture.Tap(Zone.Center), ModifierState(), {}, {}, feedback::add)

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
    fun `pressing Ctrl and immediately pressing a letter applies Ctrl without waiting for hold`() {
        val ctrlDispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY)
        val letterDispatcher = KeyDispatcher(LETTER_KEY)
        val executed = mutableListOf<SemanticAction>()

        // Ctrl's own Hold threshold never fires here - only Pressed does - matching the real
        // "tap+hold Ctrl, then immediately press a" scenario the user reported as a delay.
        var state = ctrlDispatcher.handle(Gesture.Pressed, ModifierState(), executed::add, {}, {})
        state = letterDispatcher.handle(Gesture.Pressed, state, executed::add, {}, {})
        state = letterDispatcher.handle(Gesture.Tap(Zone.Center), state, executed::add, {}, {})

        assertEquals(listOf(SemanticAction.TypeText("s", setOf(ModifierId.CTRL))), executed)
    }

    @Test
    fun `a quick tap on Ctrl (no hold reached) buzzes once and reclassifies to a sticky one-shot`() {
        val dispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY)
        val feedback = mutableListOf<FeedbackEvent>()

        var state = dispatcher.handle(Gesture.Pressed, ModifierState(), {}, {}, feedback::add)
        state = dispatcher.handle(Gesture.Tap(Zone.Center), state, {}, {}, feedback::add)
        state = dispatcher.handle(Gesture.Released, state, {}, {}, feedback::add)

        // Exactly one buzz for the whole press - the "on tap in vibrates twice" bug was Pressed's
        // TapRecognized plus a second ModifierActivated fired from the Tap branch itself.
        assertEquals(listOf(FeedbackEvent.TapRecognized), feedback)
        assertTrue(state.isActive(ModifierId.CTRL))
        assertEquals(ActivationMode.ONE_SHOT, state.active.getValue(ModifierId.CTRL).mode)
    }

    @Test
    fun `pressing then swiping from Ctrl to Alt leaves only Alt active, never both`() {
        val dispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY)

        var state = dispatcher.handle(Gesture.Pressed, ModifierState(), {}, {}, {})
        assertTrue("Pressed provisionally guesses the center zone's modifier", state.isActive(ModifierId.CTRL))

        state = dispatcher.handle(Gesture.SwipeLocked(Direction.RIGHT), state, {}, {}, {})

        assertFalse("hand-off to the zone the swipe actually locked must undo the provisional guess", state.isActive(ModifierId.CTRL))
        assertTrue(state.isActive(ModifierId.ALT))

        state = dispatcher.handle(Gesture.Hold(Zone.Directional(Direction.RIGHT)), state, {}, {}, {})
        state = dispatcher.handle(Gesture.Released, state, {}, {}, {})

        assertFalse(state.isActive(ModifierId.ALT))
        assertFalse("Ctrl must never have leaked back active after the hand-off", state.isActive(ModifierId.CTRL))
    }

    @Test
    fun `only the first slide step of a press asks engine output to re-derive the anchor, later steps trust the cache`() {
        val key =
            KeyMapping(
                CONFIG,
                mapOf(Zone.Center to KeyIntent.Command(CommandId.BACKSPACE)),
                slideBehavior = SlideBehavior.SELECT_AND_DELETE,
            )
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()

        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, -1), ModifierState(), executed::add, {}, {})
        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, -1), ModifierState(), executed::add, {}, {})
        dispatcher.handle(Gesture.Released, ModifierState(), executed::add, {}, {})

        val steps = executed.filterIsInstance<SemanticAction.ExtendSelection>()
        assertEquals(2, steps.size)
        assertTrue("the first slide step of a press must ask for a fresh read of the editor", steps[0].resetAnchor)
        assertFalse("a later step in the same press must trust the cached position, not re-query", steps[1].resetAnchor)
    }

    @Test
    fun `a new press after Released asks for a fresh anchor again, not the previous press's cache`() {
        val key =
            KeyMapping(
                CONFIG,
                mapOf(Zone.Center to KeyIntent.Command(CommandId.BACKSPACE)),
                slideBehavior = SlideBehavior.SELECT_AND_DELETE,
            )
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()

        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, -1), ModifierState(), executed::add, {}, {})
        dispatcher.handle(Gesture.Released, ModifierState(), executed::add, {}, {})
        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, -1), ModifierState(), executed::add, {}, {})

        val steps = executed.filterIsInstance<SemanticAction.ExtendSelection>()
        assertEquals(2, steps.size)
        assertTrue(steps[1].resetAnchor)
    }

    @Test
    fun `plain cursor-move slide gets the same first-step-resets, later-steps-cache treatment`() {
        val key =
            KeyMapping(
                CONFIG,
                mapOf(Zone.Center to KeyIntent.Text(" ")),
                slideBehavior = SlideBehavior.MOVE_CURSOR,
            )
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()

        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), ModifierState(), executed::add, {}, {})
        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), ModifierState(), executed::add, {}, {})

        val steps = executed.filterIsInstance<SemanticAction.MoveCursor>()
        assertEquals(2, steps.size)
        assertTrue(steps[0].resetAnchor)
        assertFalse(steps[1].resetAnchor)
    }

    @Test
    fun `sliding a cursor-move key extends selection instead of moving while Shift is active`() {
        val key = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.Text(" ")), slideBehavior = SlideBehavior.MOVE_CURSOR)
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()
        val shiftHeld = ModifierState().activate(ModifierId.SHIFT, ActivationMode.HELD)

        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), shiftHeld, executed::add, {}, {})

        assertEquals(listOf(SemanticAction.ExtendSelection(CursorDirection.RIGHT, resetAnchor = true)), executed)
    }

    @Test
    fun `sliding the same cursor-move key without Shift active just moves the cursor`() {
        val key = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.Text(" ")), slideBehavior = SlideBehavior.MOVE_CURSOR)
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()

        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), ModifierState(), executed::add, {}, {})

        assertEquals(listOf(SemanticAction.MoveCursor(CursorDirection.RIGHT, resetAnchor = true)), executed)
    }

    @Test
    fun `releasing Shift mid-slide switches a cursor-move key from extending back to just moving`() {
        val key = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.Text(" ")), slideBehavior = SlideBehavior.MOVE_CURSOR)
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()
        val shiftHeld = ModifierState().activate(ModifierId.SHIFT, ActivationMode.HELD)

        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), shiftHeld, executed::add, {}, {})
        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), ModifierState(), executed::add, {}, {})

        assertEquals(
            listOf(
                SemanticAction.ExtendSelection(CursorDirection.RIGHT, resetAnchor = true),
                SemanticAction.MoveCursor(CursorDirection.RIGHT, resetAnchor = false),
            ),
            executed,
        )
    }

    @Test
    fun `a one-shot Shift used for a slide stays active through the whole slide, then is consumed on release`() {
        val key = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.Text(" ")), slideBehavior = SlideBehavior.MOVE_CURSOR)
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()
        val shiftOneShot = ModifierState().activate(ModifierId.SHIFT, ActivationMode.ONE_SHOT)

        var state = dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), shiftOneShot, executed::add, {}, {})
        state = dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), state, executed::add, {}, {})

        // Still selecting on the second step - a one-shot Shift wasn't consumed mid-slide, which
        // would otherwise have reverted the rest of the slide to plain cursor movement.
        assertEquals(2, executed.filterIsInstance<SemanticAction.ExtendSelection>().size)
        assertTrue("Shift must still be active mid-slide", state.isActive(ModifierId.SHIFT))

        state = dispatcher.handle(Gesture.Released, state, executed::add, {}, {})

        assertFalse("a one-shot Shift must be consumed once the whole slide press ends", state.isActive(ModifierId.SHIFT))
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
