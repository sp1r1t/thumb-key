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
    fun `app-integration keys are first-class commands on the same intent model as Enter`() {
        val settingsMenuKey = SUAVE_LAYOUT.getValue(KeyPosition(3, 1))
        val clipboardKey = SUAVE_LAYOUT.getValue(KeyPosition(3, 2))

        assertEquals(KeyIntent.Command(CommandId.TOGGLE_EMOJI_MODE), settingsMenuKey.intents[Zone.Center])
        assertEquals(KeyIntent.Command(CommandId.GOTO_SETTINGS), settingsMenuKey.intents[Zone.Directional(Direction.UP)])
        assertEquals(KeyIntent.Command(CommandId.TOGGLE_HIDE_LETTERS), settingsMenuKey.intents[Zone.Directional(Direction.UP_LEFT)])
        assertEquals(KeyIntent.Command(CommandId.SWITCH_IME), settingsMenuKey.intents[Zone.Directional(Direction.DOWN)])
        assertEquals(KeyIntent.Command(CommandId.SWITCH_IME_VOICE), settingsMenuKey.intents[Zone.Directional(Direction.DOWN_LEFT)])
        assertEquals(KeyIntent.Command(CommandId.SWITCH_LANGUAGE), settingsMenuKey.intents[Zone.Directional(Direction.LEFT)])
        assertEquals(KeyIntent.Command(CommandId.MOVE_KEYBOARD), settingsMenuKey.intents[Zone.Directional(Direction.RIGHT)])

        assertEquals(KeyIntent.Command(CommandId.TOGGLE_NUMERIC_MODE), clipboardKey.intents[Zone.Center])
        assertEquals(KeyIntent.Command(CommandId.COPY), clipboardKey.intents[Zone.Directional(Direction.UP)])
        assertEquals(KeyIntent.Command(CommandId.SELECT_ALL), clipboardKey.intents[Zone.Directional(Direction.UP_LEFT)])
        assertEquals(KeyIntent.Command(CommandId.CUT), clipboardKey.intents[Zone.Directional(Direction.UP_RIGHT)])
        assertEquals(KeyIntent.Command(CommandId.PASTE), clipboardKey.intents[Zone.Directional(Direction.DOWN)])
        assertEquals(KeyIntent.Command(CommandId.TOGGLE_CLIPBOARD_HISTORY), clipboardKey.intents[Zone.Directional(Direction.LEFT)])
        assertEquals(KeyIntent.Command(CommandId.UNDO), clipboardKey.intents[Zone.Directional(Direction.DOWN_LEFT)])
        assertEquals(KeyIntent.Command(CommandId.REDO), clipboardKey.intents[Zone.Directional(Direction.DOWN_RIGHT)])
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

    @Test
    fun `Enter spans two columns so the bottom row fills the same width as the letter rows`() {
        val enter = SUAVE_LAYOUT.getValue(KeyPosition(3, 3))

        assertEquals(2, enter.columnSpan)
        assertEquals(KeyIntent.Command(CommandId.ENTER), enter.intents[Zone.Center])

        val spansByRow =
            SUAVE_LAYOUT.entries
                .groupBy { it.key.row }
                .mapValues { (_, keys) -> keys.sumOf { it.value.columnSpan } }

        assertEquals(setOf(5), spansByRow.values.toSet())
    }

    @Test
    fun `numeric is a layer of Suave, not a separate builtin layout`() {
        val suave = BuiltinLayouts.SUAVE

        assertEquals(SUAVE_NUMERIC_LAYOUT, suave.numericLayout)
        assertEquals(SUAVE_EMOJI_BOTTOM_ROW, suave.emojiBottomRow)
        assertEquals(SUAVE_LAYOUT, suave.gridFor(LayoutLayer.MAIN))
        assertEquals(SUAVE_NUMERIC_LAYOUT, suave.gridFor(LayoutLayer.NUMERIC))
        assertEquals(SUAVE_EMOJI_BOTTOM_ROW, suave.gridFor(LayoutLayer.EMOJI))
        assertEquals(LayerContent.EmojiPicker, suave.contentFor(LayoutLayer.EMOJI))
        assertEquals(SUAVE_EMOJI_LAYER_HEIGHT_ROWS, suave.heightRows(LayoutLayer.EMOJI))
        assertEquals(listOf(suave), BuiltinLayouts.ALL)
    }

    @Test
    fun `numeric layer keeps the same grid shape and puts abc on the clipboard cluster`() {
        val abc = SUAVE_NUMERIC_LAYOUT.getValue(KeyPosition(3, 2))
        val enter = SUAVE_NUMERIC_LAYOUT.getValue(KeyPosition(3, 3))

        assertEquals(KeyIntent.Command(CommandId.TOGGLE_ABC_MODE), abc.intents[Zone.Center])
        assertEquals(KeyIntent.Command(CommandId.COPY), abc.intents[Zone.Directional(Direction.UP)])
        assertEquals(KeyIntent.Command(CommandId.TOGGLE_CLIPBOARD_HISTORY), abc.intents[Zone.Directional(Direction.LEFT)])
        assertEquals(2, enter.columnSpan)
        assertEquals(KeyIntent.Text("1"), SUAVE_NUMERIC_LAYOUT.getValue(KeyPosition(0, 0)).intents[Zone.Center])

        val spansByRow =
            SUAVE_NUMERIC_LAYOUT.entries
                .groupBy { it.key.row }
                .mapValues { (_, keys) -> keys.sumOf { it.value.columnSpan } }
        assertEquals(setOf(5), spansByRow.values.toSet())
    }

    @Test
    fun `emoji layer is a picker plus the functional bottom row, with space instead of 123`() {
        val bottom = SUAVE_EMOJI_BOTTOM_ROW
        val space = bottom.getValue(KeyPosition(0, 2))

        assertEquals(setOf(KeyPosition(0, 0), KeyPosition(0, 1), KeyPosition(0, 2), KeyPosition(0, 3)), bottom.keys)
        assertEquals(KeyIntent.Command(CommandId.BACKSPACE), bottom.getValue(KeyPosition(0, 0)).intents[Zone.Center])
        assertEquals(KeyIntent.Command(CommandId.TOGGLE_EMOJI_MODE), bottom.getValue(KeyPosition(0, 1)).intents[Zone.Center])
        assertEquals(KeyIntent.Text(" "), space.intents[Zone.Center])
        assertEquals(KeyIntent.Command(CommandId.ARROW_LEFT), space.intents[Zone.Directional(Direction.LEFT)])
        assertEquals(KeyIntent.Command(CommandId.ARROW_RIGHT), space.intents[Zone.Directional(Direction.RIGHT)])
        assertEquals(KeyIntent.Command(CommandId.ARROW_UP), space.intents[Zone.Directional(Direction.UP)])
        assertEquals(KeyIntent.Command(CommandId.ARROW_DOWN), space.intents[Zone.Directional(Direction.DOWN)])
        assertEquals(SlideBehavior.MOVE_CURSOR, space.slideBehavior)
        assertEquals(2, bottom.getValue(KeyPosition(0, 3)).columnSpan)
        assertEquals(5, bottom.values.sumOf { it.columnSpan })
    }
}
