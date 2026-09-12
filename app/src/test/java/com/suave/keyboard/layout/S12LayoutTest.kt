package com.suave.keyboard.layout

import com.suave.keyboard.engine.action.SemanticAction
import com.suave.keyboard.engine.dispatch.KeyDispatcher
import com.suave.keyboard.engine.gesture.Direction
import com.suave.keyboard.engine.gesture.GestureRecognizer
import com.suave.keyboard.engine.gesture.RecognizerInput
import com.suave.keyboard.engine.gesture.SlideAxis
import com.suave.keyboard.engine.gesture.TouchEvent
import com.suave.keyboard.engine.gesture.TouchPhase
import com.suave.keyboard.engine.gesture.Zone
import com.suave.keyboard.engine.gesture.hasDirection
import com.suave.keyboard.engine.gesture.occupiedSwipeMask
import com.suave.keyboard.engine.intent.CommandId
import com.suave.keyboard.engine.intent.KeyIntent
import com.suave.keyboard.engine.intent.KeyPosition
import com.suave.keyboard.engine.intent.ModifierId
import com.suave.keyboard.engine.intent.SlideBehavior
import com.suave.keyboard.engine.modifier.ModifierState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class S12LayoutTest {
    @Test
    fun `every grid position from the original layout is present`() {
        val expectedPositions =
            (0..2).flatMap { row -> (0..4).map { col -> KeyPosition(row, col) } } +
                (0..3).map { col -> KeyPosition(3, col) }

        assertEquals(expectedPositions.toSet(), S12_LAYOUT.keys)
    }

    @Test
    fun `ctrl alt and esc all live on the same physical key, reached by center and two swipes`() {
        val modifierKey = S12_LAYOUT.getValue(KeyPosition(3, 0))

        assertEquals(KeyIntent.ModifierPress(ModifierId.CTRL), modifierKey.intents[Zone.Center])
        assertEquals(KeyIntent.ModifierPress(ModifierId.ALT), modifierKey.intents[Zone.Directional(Direction.RIGHT)])
        assertEquals(KeyIntent.ModifierPress(ModifierId.ESC), modifierKey.intents[Zone.Directional(Direction.UP)])
    }

    @Test
    fun `shift key is a ModifierPress, not a hardcoded case-toggle action`() {
        val shiftKey = S12_LAYOUT.getValue(KeyPosition(2, 2))

        assertEquals(KeyIntent.ModifierPress(ModifierId.SHIFT), shiftKey.intents[Zone.Center])
    }

    @Test
    fun `backspace slides to select-and-delete, spacebar slides to move the cursor`() {
        val backspace = S12_LAYOUT.getValue(KeyPosition(0, 2))
        val spacebar = S12_LAYOUT.getValue(KeyPosition(1, 2))

        assertEquals(SlideBehavior.SELECT_AND_DELETE, backspace.slideBehavior)
        assertEquals(SlideBehavior.MOVE_CURSOR, spacebar.slideBehavior)
        assertEquals(SlideAxis.HORIZONTAL, backspace.gestureConfig.slideAxis)
        assertEquals(SlideAxis.BOTH, spacebar.gestureConfig.slideAxis)
        assertEquals(KeyIntent.Command(CommandId.BACKSPACE), backspace.intents[Zone.Center])
        assertEquals(KeyIntent.Text(" "), spacebar.intents[Zone.Center])
    }

    @Test
    fun `multi-character German digraphs are preserved as single text intents`() {
        val eKey = S12_LAYOUT.getValue(KeyPosition(1, 1))
        val sKey = S12_LAYOUT.getValue(KeyPosition(1, 4))

        assertEquals(KeyIntent.Text("ch"), eKey.intents[Zone.Directional(Direction.DOWN_RIGHT)])
        assertEquals(KeyIntent.Text("sch"), sKey.intents[Zone.Directional(Direction.DOWN_LEFT)])
    }

    @Test
    fun `n-cluster has no diagonal intents so a slightly high left swipe still types g`() {
        val n = S12_LAYOUT.getValue(KeyPosition(1, 3))
        assertEquals(occupiedSwipeMask(n.intents), n.gestureConfig.occupiedDirections)
        assertTrue(n.gestureConfig.occupiedDirections.hasDirection(Direction.LEFT))
        assertFalse(n.gestureConfig.occupiedDirections.hasDirection(Direction.UP_LEFT))
        assertEquals(KeyIntent.Text("n"), n.intents[Zone.Center])
        assertEquals(KeyIntent.Text("g"), n.intents[Zone.Directional(Direction.LEFT)])
        assertEquals(null, n.intents[Zone.Directional(Direction.UP_LEFT)])

        val recognizer = GestureRecognizer(n.gestureConfig.copy(minSwipeDistancePx = 20f))
        val dispatcher = KeyDispatcher(n)
        val executed = mutableListOf<SemanticAction>()
        var state = ModifierState()

        fun feed(input: RecognizerInput) {
            recognizer.process(input).forEach { gesture ->
                state = dispatcher.handle(gesture, state, executed::add)
            }
        }
        feed(RecognizerInput.Touch(TouchEvent(0f, 0f, 1_000L, TouchPhase.DOWN)))
        feed(RecognizerInput.Touch(TouchEvent(-40f, -20f, 1_050L, TouchPhase.MOVE)))
        feed(RecognizerInput.Touch(TouchEvent(-40f, -20f, 1_100L, TouchPhase.UP)))

        assertEquals(listOf(SemanticAction.TypeText("g")), executed)
    }

    @Test
    fun `keys with diagonal tokens occupy those swipe directions`() {
        val r = S12_LAYOUT.getValue(KeyPosition(0, 1))
        val e = S12_LAYOUT.getValue(KeyPosition(1, 1))
        assertEquals(occupiedSwipeMask(r.intents), r.gestureConfig.occupiedDirections)
        assertEquals(occupiedSwipeMask(e.intents), e.gestureConfig.occupiedDirections)
        assertTrue(r.gestureConfig.occupiedDirections.hasDirection(Direction.DOWN_LEFT))
        assertTrue(e.gestureConfig.occupiedDirections.hasDirection(Direction.DOWN_RIGHT))
        assertEquals(KeyIntent.Text("?"), r.intents[Zone.Directional(Direction.DOWN_LEFT)])
        assertEquals(KeyIntent.Text("ch"), e.intents[Zone.Directional(Direction.DOWN_RIGHT)])
    }

    @Test
    fun `app-integration keys are first-class commands on the same intent model as Enter`() {
        val settingsMenuKey = S12_LAYOUT.getValue(KeyPosition(3, 1))
        val clipboardKey = S12_LAYOUT.getValue(KeyPosition(3, 2))

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
            S12_LAYOUT.values
                .flatMap { it.intents.values }
                .filterIsInstance<KeyIntent.Text>()
                .map { it.text }
                .toSet()

        // Every S12_SHIFT_MAPPINGS key should correspond to a real token on the layout -
        // otherwise it's dead data nothing will ever look up.
        for (token in S12_SHIFT_MAPPINGS.keys) {
            assertTrue("shift mapping for \"$token\" has no matching layout token", token in allTexts)
        }
        for (token in S12_CAPS_LOCK_MAPPINGS.keys) {
            assertTrue("caps-lock mapping for \"$token\" has no matching layout token", token in allTexts)
            assertTrue(
                "caps-lock override \"$token\" should also exist in the shift table (fallback base)",
                token in S12_SHIFT_MAPPINGS,
            )
            assertTrue(
                "caps-lock override for \"$token\" should differ from shift",
                S12_CAPS_LOCK_MAPPINGS.getValue(token) != S12_SHIFT_MAPPINGS.getValue(token),
            )
        }
    }

    @Test
    fun `S12 is one named layout in the builtin registry, not a privileged singleton`() {
        assertEquals("s12", BuiltinLayouts.S12.id)
        assertEquals(listOf(BuiltinLayouts.S12), BuiltinLayouts.ALL)
        assertEquals(BuiltinLayouts.S12, BuiltinLayouts.byIndex(0))
        assertEquals(BuiltinLayouts.S12, BuiltinLayouts.byIndex(99))
        assertEquals(listOf(BuiltinLayouts.S12), BuiltinLayouts.enabledFromDb("0"))
        assertEquals(listOf(BuiltinLayouts.S12), BuiltinLayouts.enabledFromDb(null))
        assertFalse(BuiltinLayouts.canSwitch(null))
        assertFalse(BuiltinLayouts.canSwitch("0"))
        assertFalse(BuiltinLayouts.canSwitch("0,0"))
        assertEquals(BuiltinLayouts.S12, LayoutRegistry.byId("s12"))
        assertEquals(listOf(BuiltinLayouts.S12), LayoutRegistry.enabledFromDb("s12"))
        assertEquals(listOf(BuiltinLayouts.S12), LayoutRegistry.enabledFromDb(null))
        assertFalse(LayoutRegistry.canSwitch(null))
        assertFalse(LayoutRegistry.canSwitch("s12"))
        assertFalse(LayoutRegistry.canSwitch("s12,s12"))
    }

    @Test
    fun `Enter spans two columns so the bottom row fills the same width as the letter rows`() {
        val enter = S12_LAYOUT.getValue(KeyPosition(3, 3))

        assertEquals(2, enter.columnSpan)
        assertEquals(KeyIntent.Command(CommandId.ENTER), enter.intents[Zone.Center])
        assertEquals(KeyIntent.Command(CommandId.TAB), enter.intents[Zone.Directional(Direction.UP)])
        assertEquals(null, enter.intents[Zone.Directional(Direction.LEFT)])

        val spansByRow =
            S12_LAYOUT.entries
                .groupBy { it.key.row }
                .mapValues { (_, keys) -> keys.sumOf { it.value.columnSpan } }

        assertEquals(setOf(5), spansByRow.values.toSet())
    }

    @Test
    fun `numeric is a layer of S12, not a separate builtin layout`() {
        val s12 = BuiltinLayouts.S12

        assertEquals(S12_NUMERIC_LAYOUT, s12.numericLayout)
        assertEquals(S12_EMOJI_BOTTOM_ROW, s12.emojiBottomRow)
        assertEquals(S12_CLIPBOARD_BOTTOM_ROW, s12.clipboardBottomRow)
        assertEquals(S12_LAYOUT, s12.gridFor(LayoutLayer.MAIN))
        assertEquals(S12_NUMERIC_LAYOUT, s12.gridFor(LayoutLayer.NUMERIC))
        assertEquals(S12_EMOJI_BOTTOM_ROW, s12.gridFor(LayoutLayer.EMOJI))
        assertEquals(LayerContent.EmojiPicker, s12.contentFor(LayoutLayer.EMOJI))
        assertEquals(S12_EMOJI_LAYER_HEIGHT_ROWS, s12.heightRows(LayoutLayer.EMOJI))
        assertEquals(LayerContent.ClipboardHistory, s12.contentFor(LayoutLayer.CLIPBOARD))
        assertEquals(S12_CLIPBOARD_LAYER_HEIGHT_ROWS, s12.heightRows(LayoutLayer.CLIPBOARD))
        assertEquals(listOf(s12), BuiltinLayouts.ALL)
    }

    @Test
    fun `clipboard layer is a history slot plus the functional bottom row, with space instead of 123`() {
        val s12 = BuiltinLayouts.S12
        val bottom = S12_CLIPBOARD_BOTTOM_ROW
        val space = bottom.getValue(KeyPosition(0, 2))

        assertEquals(bottom, s12.gridFor(LayoutLayer.CLIPBOARD))
        assertEquals(setOf(KeyPosition(0, 0), KeyPosition(0, 1), KeyPosition(0, 2), KeyPosition(0, 3)), bottom.keys)
        assertEquals(KeyIntent.Command(CommandId.BACKSPACE), bottom.getValue(KeyPosition(0, 0)).intents[Zone.Center])
        assertEquals(
            KeyIntent.Command(CommandId.TOGGLE_CLIPBOARD_HISTORY),
            bottom.getValue(KeyPosition(0, 1)).intents[Zone.Center],
        )
        assertEquals(KeyIntent.Text(" "), space.intents[Zone.Center])
        assertEquals(KeyIntent.Command(CommandId.ARROW_LEFT), space.intents[Zone.Directional(Direction.LEFT)])
        assertEquals(KeyIntent.Command(CommandId.ARROW_RIGHT), space.intents[Zone.Directional(Direction.RIGHT)])
        assertEquals(KeyIntent.Command(CommandId.ARROW_UP), space.intents[Zone.Directional(Direction.UP)])
        assertEquals(KeyIntent.Command(CommandId.ARROW_DOWN), space.intents[Zone.Directional(Direction.DOWN)])
        assertEquals(SlideBehavior.MOVE_CURSOR, space.slideBehavior)
        assertEquals(2, bottom.getValue(KeyPosition(0, 3)).columnSpan)
        assertEquals(5, bottom.values.sumOf { it.columnSpan })
    }

    @Test
    fun `numeric layer keeps the same grid shape and puts abc on the clipboard cluster`() {
        val abc = S12_NUMERIC_LAYOUT.getValue(KeyPosition(3, 2))
        val enter = S12_NUMERIC_LAYOUT.getValue(KeyPosition(3, 3))

        assertEquals(KeyIntent.Command(CommandId.TOGGLE_ABC_MODE), abc.intents[Zone.Center])
        assertEquals(KeyIntent.Command(CommandId.COPY), abc.intents[Zone.Directional(Direction.UP)])
        assertEquals(KeyIntent.Command(CommandId.TOGGLE_CLIPBOARD_HISTORY), abc.intents[Zone.Directional(Direction.LEFT)])
        assertEquals(2, enter.columnSpan)
        assertEquals(KeyIntent.Text("1"), S12_NUMERIC_LAYOUT.getValue(KeyPosition(0, 0)).intents[Zone.Center])

        val spansByRow =
            S12_NUMERIC_LAYOUT.entries
                .groupBy { it.key.row }
                .mapValues { (_, keys) -> keys.sumOf { it.value.columnSpan } }
        assertEquals(setOf(5), spansByRow.values.toSet())
    }

    @Test
    fun `emoji layer is a picker plus the functional bottom row, with space instead of 123`() {
        val bottom = S12_EMOJI_BOTTOM_ROW
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
