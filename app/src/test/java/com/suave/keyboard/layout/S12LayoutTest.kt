package com.suave.keyboard.layout

import com.suave.keyboard.engine.gesture.Zone
import com.suave.keyboard.engine.intent.KeyIntent
import com.suave.keyboard.engine.intent.KeyPosition
import com.suave.keyboard.layout.json.loadS12Asset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class S12LayoutTest {
    private val s12 by lazy { loadS12Asset() }
    private val main get() = s12.requireLayer(ActiveLayer.MAIN).keyGrid
    private val numeric get() = s12.requireLayer(ActiveLayer.NUMERIC).keyGrid
    private val emoji get() = s12.requireLayer(ActiveLayer.EMOJI).keyGrid
    private val clipboard get() = s12.requireLayer(ActiveLayer.CLIPBOARD).keyGrid

    @Test
    fun `main layer has expected key count`() {
        assertEquals(19, main.size)
    }

    @Test
    fun `shift mappings cover layout tokens`() {
        val tokens =
            main.values
                .flatMap { it.intents.values }
                .mapNotNull { (it as? KeyIntent.Text)?.text }
                .toSet()
        for (token in S12_SHIFT_MAPPINGS.keys) {
            assertTrue("$token should appear on the layout", token in tokens)
        }
        for (token in S12_CAPS_LOCK_MAPPINGS.keys) {
            assertTrue(
                S12_CAPS_LOCK_MAPPINGS.getValue(token) != S12_SHIFT_MAPPINGS.getValue(token),
            )
        }
    }

    @Test
    fun `layers expose grids and emoji height`() {
        assertEquals(main, s12.gridFor(ActiveLayer.Main))
        assertEquals(numeric, s12.gridFor(ActiveLayer.Numeric))
        assertEquals(emoji, s12.gridFor(ActiveLayer.Emoji))
        assertEquals(clipboard, s12.gridFor(ActiveLayer.Clipboard))
        assertEquals(S12_EMOJI_LAYER_HEIGHT_ROWS, s12.heightRows(ActiveLayer.Emoji))
        assertEquals(S12_CLIPBOARD_LAYER_HEIGHT_ROWS, s12.heightRows(ActiveLayer.Clipboard))
        assertEquals(5, s12.contentRows(ActiveLayer.Emoji))
        assertTrue(s12.requireLayer(ActiveLayer.EMOJI).overlay)
        assertTrue(s12.requireLayer(ActiveLayer.CLIPBOARD).overlay)
    }

    @Test
    fun `enter spans two columns on main`() {
        val enter = main.getValue(KeyPosition(3, 3))
        assertEquals(2f, enter.columnSpan)
    }

    @Test
    fun `numeric has digit one`() {
        assertEquals(
            KeyIntent.Text("1"),
            numeric.getValue(KeyPosition(0, 0)).intents[Zone.Center],
        )
    }
}
