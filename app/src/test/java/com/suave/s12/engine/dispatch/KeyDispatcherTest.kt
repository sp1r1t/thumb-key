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
import com.suave.s12.engine.modifier.modifierBehaviors
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

        var state = ctrlDispatcher.handle(Gesture.Hold(Zone.Center), ModifierState(), executed::add)
        state = letterDispatcher.handle(Gesture.Pressed, state, executed::add)
        state = letterDispatcher.handle(Gesture.Tap(Zone.Center), state, executed::add)
        state = letterDispatcher.handle(Gesture.Released, state, executed::add)
        state = ctrlDispatcher.handle(Gesture.Released, state, executed::add)

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
        var state = dispatcher.handle(Gesture.Hold(Zone.Directional(Direction.RIGHT)), ModifierState())
        assertEquals(ActivationMode.HELD, state.active.getValue(ModifierId.ALT).mode)

        // Release: must deactivate ALT (the zone this press actually engaged), immediately, and
        // must never have touched CTRL at all - this is the exact bug class from the old engine,
        // where the center action fired unconditionally on press-down regardless of which zone
        // the swipe eventually locked.
        state = dispatcher.handle(Gesture.Released, state, {})

        assertFalse(state.isActive(ModifierId.ALT))
        assertFalse("Ctrl must never have been touched by a press that locked onto the Alt zone", state.isActive(ModifierId.CTRL))
    }

    @Test
    fun `swiping to Esc then releasing without ever holding still leaves Esc as a clean one-shot`() {
        val dispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY)

        var state = dispatcher.handle(Gesture.Tap(Zone.Directional(Direction.UP)), ModifierState())
        state = dispatcher.handle(Gesture.Released, state)

        assertTrue("a quick swipe-tap to Esc should still be a one-shot activation", state.isActive(ModifierId.ESC))
        assertEquals(ActivationMode.ONE_SHOT, state.active.getValue(ModifierId.ESC).mode)
    }

    @Test
    fun `tapping Esc again while it's still queued as a one-shot combo prefix sends a real Escape instead`() {
        val dispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY)
        val executed = mutableListOf<SemanticAction>()
        val escZone = Zone.Directional(Direction.UP)

        // First press: a normal quick swipe-tap to Esc, queuing it as a one-shot combo prefix.
        var state = dispatcher.handle(Gesture.Pressed, ModifierState(), executed::add)
        state = dispatcher.handle(Gesture.SwipeLocked(Direction.UP), state, executed::add)
        state = dispatcher.handle(Gesture.Tap(escZone), state, executed::add)
        state = dispatcher.handle(Gesture.Released, state, executed::add)
        assertTrue("first tap queues a one-shot Esc combo prefix", state.isActive(ModifierId.ESC))

        // Second, separate press on the same zone while that's still queued: Esc+Esc.
        state = dispatcher.handle(Gesture.Pressed, state, executed::add)
        state = dispatcher.handle(Gesture.SwipeLocked(Direction.UP), state, executed::add)
        state = dispatcher.handle(Gesture.Tap(escZone), state, executed::add)

        assertEquals(listOf(SemanticAction.TypeCommand(CommandId.ESCAPE)), executed.filterIsInstance<SemanticAction.TypeCommand>())
        assertFalse("Esc+Esc must consume the queued one-shot instead of leaving it active", state.isActive(ModifierId.ESC))
    }

    @Test
    fun `with escAsModifier false, tapping the Esc zone sends a standalone Escape and never touches modifier state`() {
        val dispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY, modifierBehaviors = modifierBehaviors(mapOf(ModifierId.ESC to false)))
        val executed = mutableListOf<SemanticAction>()

        val state =
            dispatcher.handle(Gesture.Tap(Zone.Directional(Direction.UP)), ModifierState(), executed::add)

        assertEquals(listOf(SemanticAction.TypeCommand(CommandId.ESCAPE)), executed)
        assertFalse("standalone mode never activates Esc as a modifier", state.isActive(ModifierId.ESC))
    }

    @Test
    fun `with escAsModifier false, holding the Esc zone repeats the standalone Escape like any other command key`() {
        val dispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY, modifierBehaviors = modifierBehaviors(mapOf(ModifierId.ESC to false)))
        val executed = mutableListOf<SemanticAction>()
        val escZone = Zone.Directional(Direction.UP)

        dispatcher.handle(Gesture.Hold(escZone), ModifierState(), executed::add)
        dispatcher.handle(Gesture.HoldRepeat(escZone), ModifierState(), executed::add)

        assertEquals(
            listOf(SemanticAction.TypeCommand(CommandId.ESCAPE), SemanticAction.TypeCommand(CommandId.ESCAPE)),
            executed,
        )
    }

    @Test
    fun `a plain character key executes its text and fires no feedback on its own (Pressed already covered it)`() {
        val key = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.Text("a")))
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()
        val feedback = mutableListOf<FeedbackEvent>()

        dispatcher.handle(Gesture.Tap(Zone.Center), ModifierState(), executed::add, feedback::add)

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
        dispatcher.handle(Gesture.Pressed, ModifierState(), executed::add, feedback::add)
        // The lock, mid-drag - buzz #2, nothing executes yet.
        dispatcher.handle(Gesture.SwipeLocked(Direction.UP), ModifierState(), executed::add, feedback::add)
        // The eventual commit on release - executes the character, but no third buzz: that
        // would land right as the finger lifts, the worst moment to feel it.
        dispatcher.handle(Gesture.Tap(Zone.Directional(Direction.UP)), ModifierState(), executed::add, feedback::add)

        assertEquals(listOf(FeedbackEvent.TapRecognized, FeedbackEvent.SwipeLocked(Direction.UP)), feedback)
        assertEquals(listOf(SemanticAction.TypeText("1")), executed)
    }

    @Test
    fun `a plain tap (no swipe) buzzes exactly once, on press, not again on commit`() {
        val key = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.Text("o")))
        val dispatcher = KeyDispatcher(key)
        val feedback = mutableListOf<FeedbackEvent>()

        dispatcher.handle(Gesture.Pressed, ModifierState(), {}, feedback::add)
        dispatcher.handle(Gesture.Tap(Zone.Center), ModifierState(), {}, feedback::add)

        assertEquals(listOf(FeedbackEvent.TapRecognized), feedback)
    }

    @Test
    fun `a copy command fires once on hold and not again on hold-repeat`() {
        val key = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.Command(CommandId.COPY)))
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()

        dispatcher.handle(Gesture.Hold(Zone.Center), ModifierState(), executed::add)
        dispatcher.handle(Gesture.HoldRepeat(Zone.Center), ModifierState(), executed::add)
        dispatcher.handle(Gesture.HoldRepeat(Zone.Center), ModifierState(), executed::add)

        assertEquals(listOf(SemanticAction.TypeCommand(CommandId.COPY)), executed)
        assertFalse(KeyIntent.Command(CommandId.COPY).repeatsOnHold())
        assertTrue(KeyIntent.Command(CommandId.ENTER).repeatsOnHold())
        assertTrue(KeyIntent.Command(CommandId.UNDO).repeatsOnHold())
    }

    @Test
    fun `pressing Ctrl and immediately pressing a letter applies Ctrl without waiting for hold`() {
        val ctrlDispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY)
        val letterDispatcher = KeyDispatcher(LETTER_KEY)
        val executed = mutableListOf<SemanticAction>()

        // Ctrl's own Hold threshold never fires here - only Pressed does - matching the real
        // "tap+hold Ctrl, then immediately press a" scenario the user reported as a delay.
        var state = ctrlDispatcher.handle(Gesture.Pressed, ModifierState(), executed::add)
        state = letterDispatcher.handle(Gesture.Pressed, state, executed::add)
        state = letterDispatcher.handle(Gesture.Tap(Zone.Center), state, executed::add)

        assertEquals(listOf(SemanticAction.TypeText("s", setOf(ModifierId.CTRL))), executed)
    }

    @Test
    fun `a quick tap on Ctrl (no hold reached) buzzes once and reclassifies to a sticky one-shot`() {
        val dispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY)
        val feedback = mutableListOf<FeedbackEvent>()

        var state = dispatcher.handle(Gesture.Pressed, ModifierState(), {}, feedback::add)
        state = dispatcher.handle(Gesture.Tap(Zone.Center), state, {}, feedback::add)
        state = dispatcher.handle(Gesture.Released, state, {}, feedback::add)

        // Exactly one buzz for the whole press - the "on tap in vibrates twice" bug was Pressed's
        // TapRecognized plus a second ModifierActivated fired from the Tap branch itself.
        assertEquals(listOf(FeedbackEvent.TapRecognized), feedback)
        assertTrue(state.isActive(ModifierId.CTRL))
        assertEquals(ActivationMode.ONE_SHOT, state.active.getValue(ModifierId.CTRL).mode)
    }

    @Test
    fun `pressing then swiping from Ctrl to Alt leaves only Alt active, never both`() {
        val dispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY)

        var state = dispatcher.handle(Gesture.Pressed, ModifierState(), {})
        assertTrue("Pressed provisionally guesses the center zone's modifier", state.isActive(ModifierId.CTRL))

        state = dispatcher.handle(Gesture.SwipeLocked(Direction.RIGHT), state, {})

        assertFalse("hand-off to the zone the swipe actually locked must undo the provisional guess", state.isActive(ModifierId.CTRL))
        assertTrue(state.isActive(ModifierId.ALT))

        state = dispatcher.handle(Gesture.Hold(Zone.Directional(Direction.RIGHT)), state, {})
        state = dispatcher.handle(Gesture.Released, state, {})

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

        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, -1), ModifierState(), executed::add)
        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, -1), ModifierState(), executed::add)
        dispatcher.handle(Gesture.Released, ModifierState(), executed::add)

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

        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, -1), ModifierState(), executed::add)
        dispatcher.handle(Gesture.Released, ModifierState(), executed::add)
        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, -1), ModifierState(), executed::add)

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

        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), ModifierState(), executed::add)
        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), ModifierState(), executed::add)

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

        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), shiftHeld, executed::add)

        assertEquals(listOf(SemanticAction.ExtendSelection(CursorDirection.RIGHT, resetAnchor = true)), executed)
    }

    @Test
    fun `sliding the same cursor-move key without Shift active just moves the cursor`() {
        val key = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.Text(" ")), slideBehavior = SlideBehavior.MOVE_CURSOR)
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()

        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), ModifierState(), executed::add)

        assertEquals(listOf(SemanticAction.MoveCursor(CursorDirection.RIGHT, resetAnchor = true)), executed)
    }

    @Test
    fun `sliding a cursor-move key vertically moves the cursor up and down`() {
        val key = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.Text(" ")), slideBehavior = SlideBehavior.MOVE_CURSOR)
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()

        dispatcher.handle(Gesture.SlideStep(SlideAxis.VERTICAL, -1), ModifierState(), executed::add)
        dispatcher.handle(Gesture.SlideStep(SlideAxis.VERTICAL, 1), ModifierState(), executed::add)

        assertEquals(
            listOf(
                SemanticAction.MoveCursor(CursorDirection.UP, resetAnchor = true),
                SemanticAction.MoveCursor(CursorDirection.DOWN, resetAnchor = false),
            ),
            executed,
        )
    }

    @Test
    fun `releasing Shift mid-slide switches a cursor-move key from extending back to just moving`() {
        val key = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.Text(" ")), slideBehavior = SlideBehavior.MOVE_CURSOR)
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()
        val shiftHeld = ModifierState().activate(ModifierId.SHIFT, ActivationMode.HELD)

        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), shiftHeld, executed::add)
        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), ModifierState(), executed::add)

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

        var state = dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), shiftOneShot, executed::add)
        state = dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), state, executed::add)

        // Still selecting on the second step - a one-shot Shift wasn't consumed mid-slide, which
        // would otherwise have reverted the rest of the slide to plain cursor movement.
        assertEquals(2, executed.filterIsInstance<SemanticAction.ExtendSelection>().size)
        assertTrue("Shift must still be active mid-slide", state.isActive(ModifierId.SHIFT))

        state = dispatcher.handle(Gesture.Released, state, executed::add)

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

        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), ModifierState(), executed::add)
        dispatcher.handle(Gesture.SlideStep(SlideAxis.HORIZONTAL, 1), ModifierState(), executed::add)
        dispatcher.handle(Gesture.Released, ModifierState(), executed::add)

        assertEquals(2, executed.count { it is SemanticAction.ExtendSelection })
        assertEquals(1, executed.count { it == SemanticAction.TypeCommand(CommandId.BACKSPACE) })
    }

    @Test
    fun `with Ctrl actsAsModifier false, tapping the Ctrl zone sends a Ctrl command and never touches modifier state`() {
        val dispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY, modifierBehaviors = modifierBehaviors(mapOf(ModifierId.CTRL to false)))
        val executed = mutableListOf<SemanticAction>()

        val state = dispatcher.handle(Gesture.Tap(Zone.Center), ModifierState(), executed::add)

        assertEquals(listOf(SemanticAction.TypeCommand(CommandId.CTRL)), executed)
        assertFalse(state.isActive(ModifierId.CTRL))
    }

    @Test
    fun `copy on a swipe and enter on another swipe of the same key both fire - any intent is placeable on any zone`() {
        val key =
            KeyMapping(
                CONFIG,
                mapOf(
                    Zone.Center to KeyIntent.Text("a"),
                    Zone.Directional(Direction.LEFT) to KeyIntent.Command(CommandId.COPY),
                    Zone.Directional(Direction.RIGHT) to KeyIntent.Command(CommandId.ENTER),
                    Zone.Directional(Direction.UP) to KeyIntent.ModifierPress(ModifierId.SHIFT),
                ),
            )
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()

        dispatcher.handle(Gesture.Tap(Zone.Directional(Direction.LEFT)), ModifierState(), executed::add)
        dispatcher.handle(Gesture.Tap(Zone.Directional(Direction.RIGHT)), ModifierState(), executed::add)
        val shiftState =
            dispatcher.handle(Gesture.Tap(Zone.Directional(Direction.UP)), ModifierState(), executed::add)

        assertEquals(
            listOf(SemanticAction.TypeCommand(CommandId.COPY), SemanticAction.TypeCommand(CommandId.ENTER)),
            executed,
        )
        assertTrue(shiftState.isActive(ModifierId.SHIFT))
    }

    @Test
    fun `copy consumes a one-shot Ctrl because it is a real command press, not a side door`() {
        val key = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.Command(CommandId.COPY)))
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()
        val queued = ModifierState().activate(ModifierId.CTRL, ActivationMode.ONE_SHOT)

        val state = dispatcher.handle(Gesture.Tap(Zone.Center), queued, executed::add)

        assertEquals(listOf(SemanticAction.TypeCommand(CommandId.COPY, setOf(ModifierId.CTRL))), executed)
        assertFalse("pressing Copy must consume a queued one-shot Ctrl", state.isActive(ModifierId.CTRL))
    }

    @Test
    fun `copy on hold-repeat does not re-fire - the intent decides, not the dispatcher type switch`() {
        val key = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.Command(CommandId.COPY)))
        val dispatcher = KeyDispatcher(key)
        val executed = mutableListOf<SemanticAction>()

        dispatcher.handle(Gesture.Hold(Zone.Center), ModifierState(), executed::add)
        dispatcher.handle(Gesture.HoldRepeat(Zone.Center), ModifierState(), executed::add)

        assertEquals(listOf(SemanticAction.TypeCommand(CommandId.COPY)), executed)
        assertFalse(KeyIntent.Command(CommandId.COPY).repeatsOnHold())
        assertTrue(KeyIntent.Command(CommandId.ENTER).repeatsOnHold())
    }

    @Test
    fun `tapping a letter consumes one-shot Shift`() {
        val shiftKey = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.ModifierPress(ModifierId.SHIFT)))
        val shiftDispatcher = KeyDispatcher(shiftKey)
        val letterDispatcher = KeyDispatcher(LETTER_KEY)
        val executed = mutableListOf<SemanticAction>()

        var state = shiftDispatcher.handle(Gesture.Pressed, ModifierState())
        state = shiftDispatcher.handle(Gesture.Tap(Zone.Center), state)
        state = shiftDispatcher.handle(Gesture.Released, state)
        assertEquals(ActivationMode.ONE_SHOT, state.active.getValue(ModifierId.SHIFT).mode)

        state = letterDispatcher.handle(Gesture.Tap(Zone.Center), state, executed::add)

        assertEquals(listOf(SemanticAction.TypeText("S")), executed)
        assertFalse("a typed letter must consume one-shot Shift", state.isActive(ModifierId.SHIFT))
    }

    @Test
    fun `holding Shift until caps lock fires a second ModifierActivated buzz`() {
        val shiftKey = KeyMapping(CONFIG, mapOf(Zone.Center to KeyIntent.ModifierPress(ModifierId.SHIFT)))
        val dispatcher = KeyDispatcher(shiftKey)
        val feedback = mutableListOf<FeedbackEvent>()

        var state = dispatcher.handle(Gesture.Pressed, ModifierState(), {}, feedback::add)
        state = dispatcher.handle(Gesture.Hold(Zone.Center), state, {}, feedback::add)

        assertEquals(ActivationMode.LOCKED, state.active.getValue(ModifierId.SHIFT).mode)
        assertEquals(
            listOf(
                FeedbackEvent.TapRecognized,
                FeedbackEvent.ModifierActivated(ModifierId.SHIFT, ActivationMode.LOCKED),
            ),
            feedback,
        )
    }

    @Test
    fun `holding Ctrl does not fire a second activation buzz because Pressed already put it in HELD`() {
        val dispatcher = KeyDispatcher(CTRL_ALT_ESC_KEY)
        val feedback = mutableListOf<FeedbackEvent>()

        var state = dispatcher.handle(Gesture.Pressed, ModifierState(), {}, feedback::add)
        dispatcher.handle(Gesture.Hold(Zone.Center), state, {}, feedback::add)

        assertEquals(listOf(FeedbackEvent.TapRecognized), feedback)
    }
}
