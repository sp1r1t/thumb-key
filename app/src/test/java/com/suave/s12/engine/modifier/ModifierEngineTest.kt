package com.suave.s12.engine.modifier

import com.suave.s12.engine.gesture.Direction
import com.suave.s12.engine.gesture.Gesture
import com.suave.s12.engine.gesture.Zone
import com.suave.s12.engine.intent.CommandId
import com.suave.s12.engine.intent.KeyIntent
import com.suave.s12.engine.intent.ModifierId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val TAP_CENTER = Gesture.Tap(Zone.Center)
private val HOLD_CENTER = Gesture.Hold(Zone.Center)
private val HOLD_REPEAT_CENTER = Gesture.HoldRepeat(Zone.Center)

class ModifierEngineTest {
    // --- Ctrl: tap = one-shot, hold = held-while-down, release-while-held = one more key ---

    @Test
    fun `tapping Ctrl activates one-shot`() {
        val state = ModifierEngine.applyModifierGesture(ModifierState(), ModifierId.CTRL, TAP_CENTER)

        assertTrue(state.isActive(ModifierId.CTRL))
        assertEquals(ActivationMode.ONE_SHOT, state.active.getValue(ModifierId.CTRL).mode)
    }

    @Test
    fun `one-shot Ctrl transforms exactly the next character then consumeOneShots clears it`() {
        var state = ModifierEngine.applyModifierGesture(ModifierState(), ModifierId.CTRL, TAP_CENTER)

        val resolved = ModifierEngine.resolve(state, KeyIntent.Text("c"))
        state = ModifierEngine.consumeOneShots(state)

        assertEquals(ResolvedIntent.TypedText("c", setOf(ModifierId.CTRL)), resolved)
        assertFalse("one-shot Ctrl must not still be active for the key after it", state.isActive(ModifierId.CTRL))
    }

    @Test
    fun `holding Ctrl activates held, stays active across HoldRepeat ticks, applies to many keys`() {
        var state = ModifierEngine.applyModifierGesture(ModifierState(), ModifierId.CTRL, HOLD_CENTER)
        assertEquals(ActivationMode.HELD, state.active.getValue(ModifierId.CTRL).mode)

        // HoldRepeat on the modifier key itself is a no-op - it doesn't re-toggle anything. This
        // is what stops a held modifier from spamming its own activation once repeat-on-hold
        // applies uniformly to every key (the bug that hit Shift's capslock-hold previously).
        state = ModifierEngine.applyModifierGesture(state, ModifierId.CTRL, HOLD_REPEAT_CENTER)
        assertEquals(ActivationMode.HELD, state.active.getValue(ModifierId.CTRL).mode)

        // Two separate other-key presses, both while still held: both get Ctrl.
        val firstKey = ModifierEngine.resolve(state, KeyIntent.Text("x"))
        state = ModifierEngine.consumeOneShots(state) // no-op for HELD
        val secondKey = ModifierEngine.resolve(state, KeyIntent.Text("s"))
        state = ModifierEngine.consumeOneShots(state)

        assertEquals(ResolvedIntent.TypedText("x", setOf(ModifierId.CTRL)), firstKey)
        assertEquals(ResolvedIntent.TypedText("s", setOf(ModifierId.CTRL)), secondKey)
        assertTrue("held Ctrl must survive across multiple key presses while still down", state.isActive(ModifierId.CTRL))
    }

    @Test
    fun `releasing a held Ctrl grants exactly one more key then reverts`() {
        var state = ModifierEngine.applyModifierGesture(ModifierState(), ModifierId.CTRL, HOLD_CENTER)
        state = ModifierEngine.applyModifierGesture(state, ModifierId.CTRL, Gesture.Released)

        // This is the exact fix for the old engine's bug: releasing Ctrl must actually transition
        // to a state a subsequent key event will clear, not just flip an unread tracking flag.
        assertEquals(ActivationMode.ONE_SHOT, state.active.getValue(ModifierId.CTRL).mode)

        val oneMoreKey = ModifierEngine.resolve(state, KeyIntent.Text("a"))
        state = ModifierEngine.consumeOneShots(state)
        val nextKeyAfterThat = ModifierEngine.resolve(state, KeyIntent.Text("b"))

        assertEquals(ResolvedIntent.TypedText("a", setOf(ModifierId.CTRL)), oneMoreKey)
        assertFalse(state.isActive(ModifierId.CTRL))
        assertEquals(ResolvedIntent.TypedText("b", emptySet()), nextKeyAfterThat)
    }

    @Test
    fun `tapping an already-active modifier deactivates it outright`() {
        var state = ModifierEngine.applyModifierGesture(ModifierState(), ModifierId.CTRL, HOLD_CENTER)
        state = ModifierEngine.applyModifierGesture(state, ModifierId.CTRL, TAP_CENTER)

        assertFalse(state.isActive(ModifierId.CTRL))
    }

    // --- Shift: tap = one-shot capital, hold = caps lock, persists past release ---

    @Test
    fun `tapping Shift capitalizes exactly the next character via uppercase, not a modifier flag`() {
        var state = ModifierEngine.applyModifierGesture(ModifierState(), ModifierId.SHIFT, TAP_CENTER)

        val resolved = ModifierEngine.resolve(state, KeyIntent.Text("a"))
        state = ModifierEngine.consumeOneShots(state)

        assertEquals(ResolvedIntent.TypedText("A", emptySet()), resolved)
        assertFalse(state.isActive(ModifierId.SHIFT))
    }

    @Test
    fun `holding Shift locks caps lock and it survives release`() {
        var state = ModifierEngine.applyModifierGesture(ModifierState(), ModifierId.SHIFT, HOLD_CENTER)
        assertEquals(ActivationMode.LOCKED, state.active.getValue(ModifierId.SHIFT).mode)

        state = ModifierEngine.applyModifierGesture(state, ModifierId.SHIFT, Gesture.Released)
        assertEquals(
            "caps lock must not revert on release like HELD does",
            ActivationMode.LOCKED,
            state.active.getValue(ModifierId.SHIFT).mode,
        )

        // Typing several letters while locked: caps lock is untouched by consumeOneShots.
        repeat(3) {
            ModifierEngine.resolve(state, KeyIntent.Text("a"))
            state = ModifierEngine.consumeOneShots(state)
        }
        assertTrue(state.isActive(ModifierId.SHIFT))
    }

    @Test
    fun `a custom shift mapping table overrides plain uppercasing`() {
        val state = ModifierEngine.applyModifierGesture(ModifierState(), ModifierId.SHIFT, TAP_CENTER)

        val resolved = ModifierEngine.resolve(state, KeyIntent.Text("1"), shiftMappings = mapOf("1" to "!"))

        assertEquals(ResolvedIntent.TypedText("!", emptySet()), resolved)
    }

    @Test
    fun `multi-character text ignores held modifiers but still gets shift-mapped`() {
        var state = ModifierEngine.applyModifierGesture(ModifierState(), ModifierId.CTRL, HOLD_CENTER)
        state = ModifierEngine.applyModifierGesture(state, ModifierId.SHIFT, TAP_CENTER)

        // "sch" has no KeyEvent representation for a Ctrl combo, so Ctrl is dropped for this key
        // - but the shift mapping table (analogous to Suave's SHIFT_MAPPINGS) still applies.
        val resolved = ModifierEngine.resolve(state, KeyIntent.Text("sch"), shiftMappings = mapOf("sch" to "Sch"))

        assertEquals(ResolvedIntent.TypedText("Sch", emptySet()), resolved)
    }

    // --- Commands keep Shift as a real modifier flag, since there's no "shifted backspace" char ---

    @Test
    fun `shift stays in the modifier set for command intents instead of transforming text`() {
        val state = ModifierEngine.applyModifierGesture(ModifierState(), ModifierId.SHIFT, TAP_CENTER)

        val resolved = ModifierEngine.resolve(state, KeyIntent.Command(CommandId.TAB))

        assertEquals(ResolvedIntent.TypedCommand(CommandId.TAB, setOf(ModifierId.SHIFT)), resolved)
    }

    // --- Combining modifiers ---

    @Test
    fun `ctrl and alt held simultaneously both apply to the same character`() {
        var state = ModifierEngine.applyModifierGesture(ModifierState(), ModifierId.CTRL, HOLD_CENTER)
        state = ModifierEngine.applyModifierGesture(state, ModifierId.ALT, HOLD_CENTER)

        val resolved = ModifierEngine.resolve(state, KeyIntent.Text("z"))

        assertEquals(ResolvedIntent.TypedText("z", setOf(ModifierId.CTRL, ModifierId.ALT)), resolved)
    }

    @Test
    fun `esc swipe zone example still resolves through the same ModifierPress routing`() {
        // Esc reached by swiping to a directional zone on the same physical key Ctrl sits on -
        // the gesture that fires applyModifierGesture is just whatever zone it locked to; the
        // modifier engine doesn't care which zone, only which ModifierId the layout bound there.
        val escTapOnSwipeZone = Gesture.Tap(Zone.Directional(Direction.UP))
        val state = ModifierEngine.applyModifierGesture(ModifierState(), ModifierId.ESC, escTapOnSwipeZone)

        assertTrue(state.isActive(ModifierId.ESC))
        assertEquals(ActivationMode.ONE_SHOT, state.active.getValue(ModifierId.ESC).mode)
    }
}
