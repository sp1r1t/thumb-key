package com.suave.s12.layout

import com.suave.s12.engine.gesture.Direction
import com.suave.s12.engine.gesture.Zone
import com.suave.s12.engine.intent.CommandId
import com.suave.s12.engine.intent.KeyIntent
import com.suave.s12.engine.intent.KeyPosition
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.engine.intent.SlideBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuaveLayoutTest {
    @Test
    fun `every grid position from the original layout is present`() {
        val expectedPositions =
            (0..2).flatMap { row -> (0..4).map { col -> KeyPosition(row, col) } } +
                (0..3).map { col -> KeyPosition(3, col) }

        assertEquals(expectedPositions.toSet(), SUAVE_LAYOUT.keys)
    }

    @Test
    fun `ctrl alt and esc all live on the same physical key, reached by center and two swipes`() {
        val modifierKey = SUAVE_LAYOUT.getValue(KeyPosition(3, 0))

        assertEquals(KeyIntent.ModifierPress(ModifierId.CTRL), modifierKey.intents[Zone.Center])
        assertEquals(KeyIntent.ModifierPress(ModifierId.ALT), modifierKey.intents[Zone.Directional(Direction.RIGHT)])
        assertEquals(KeyIntent.ModifierPress(ModifierId.ESC), modifierKey.intents[Zone.Directional(Direction.UP)])
    }

    @Test
    fun `shift key is a ModifierPress, not a hardcoded case-toggle action`() {
        val shiftKey = SUAVE_LAYOUT.getValue(KeyPosition(2, 2))

        assertEquals(KeyIntent.ModifierPress(ModifierId.SHIFT), shiftKey.intents[Zone.Center])
    }

    @Test
    fun `backspace slides to select-and-delete, spacebar slides to move the cursor`() {
        val backspace = SUAVE_LAYOUT.getValue(KeyPosition(0, 2))
        val spacebar = SUAVE_LAYOUT.getValue(KeyPosition(1, 2))

        assertEquals(SlideBehavior.SELECT_AND_DELETE, backspace.slideBehavior)
        assertEquals(SlideBehavior.MOVE_CURSOR, spacebar.slideBehavior)
        assertEquals(KeyIntent.Command(CommandId.BACKSPACE), backspace.intents[Zone.Center])
        assertEquals(KeyIntent.Text(" "), spacebar.intents[Zone.Center])
    }

    @Test
    fun `multi-character German digraphs are preserved as single text intents`() {
        val eKey = SUAVE_LAYOUT.getValue(KeyPosition(1, 1))
        val sKey = SUAVE_LAYOUT.getValue(KeyPosition(1, 4))

        assertEquals(KeyIntent.Text("ch"), eKey.intents[Zone.Directional(Direction.DOWN_RIGHT)])
        assertEquals(KeyIntent.Text("sch"), sKey.intents[Zone.Directional(Direction.DOWN_LEFT)])
    }

    @Test
    fun `app-integration keys bridge to the old KeyAction pipeline`() {
        val settingsMenuKey = SUAVE_LAYOUT.getValue(KeyPosition(3, 1))

        assertTrue(settingsMenuKey.intents.getValue(Zone.Center) is KeyIntent.LegacyAction)
        assertTrue(settingsMenuKey.intents.getValue(Zone.Directional(Direction.UP)) is KeyIntent.LegacyAction)
    }

    @Test
    fun `shift mapping table covers every irregular token this layout actually uses`() {
        val allTexts =
            SUAVE_LAYOUT.values
                .flatMap { it.intents.values }
                .filterIsInstance<KeyIntent.Text>()
                .map { it.text }
                .toSet()

        // Every SUAVE_SHIFT_MAPPINGS key should correspond to a real token on the layout -
        // otherwise it's dead data nothing will ever look up.
        for (token in SUAVE_SHIFT_MAPPINGS.keys) {
            assertTrue("shift mapping for \"$token\" has no matching layout token", token in allTexts)
        }
    }

    @Test
    fun `Suave is one named layout in the builtin registry, not a privileged singleton`() {
        assertEquals("suave", BuiltinLayouts.SUAVE.id)
        assertEquals(listOf(BuiltinLayouts.SUAVE), BuiltinLayouts.ALL)
        assertEquals(BuiltinLayouts.SUAVE, BuiltinLayouts.byIndex(0))
        assertEquals(BuiltinLayouts.SUAVE, BuiltinLayouts.byIndex(99))
        assertEquals(listOf(BuiltinLayouts.SUAVE), BuiltinLayouts.enabledFromDb("0"))
        assertEquals(listOf(BuiltinLayouts.SUAVE), BuiltinLayouts.enabledFromDb(null))
    }
}
