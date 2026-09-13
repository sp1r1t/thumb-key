package com.suave.keyboard.layout

import com.suave.keyboard.layout.json.loadS12Asset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayerHeightsTest {
    @Test
    fun `empty blob means no overrides`() {
        assertEquals(emptyMap<String, Int>(), parseLayerHeightOverrides(""))
        assertEquals(emptyMap<String, Int>(), parseLayerHeightOverrides("   "))
        assertEquals("", formatLayerHeightOverrides(emptyMap()))
    }

    @Test
    fun `round trip keeps known layers sorted by id`() {
        val stored =
            formatLayerHeightOverrides(
                mapOf(
                    ActiveLayer.EMOJI to 8,
                    ActiveLayer.CLIPBOARD to 6,
                    ActiveLayer.MAIN to 5,
                ),
            )
        assertEquals("clipboard=6,emoji=8,main=5", stored)
        assertEquals(
            mapOf(
                ActiveLayer.MAIN to 5,
                ActiveLayer.EMOJI to 8,
                ActiveLayer.CLIPBOARD to 6,
            ),
            parseLayerHeightOverrides(stored),
        )
    }

    @Test
    fun `legacy uppercase names normalize and junk is ignored`() {
        val parsed = parseLayerHeightOverrides("EMOJI=6,CLIPBOARD=7,FUTURE=9,MAIN=0,NUMERIC=abc")
        assertEquals(
            mapOf(ActiveLayer.EMOJI to 6, ActiveLayer.CLIPBOARD to 7, "FUTURE" to 9),
            parsed,
        )
    }

    @Test
    fun `S12 emoji default is taller than the bottom row, letters match the grid`() {
        val s12 = loadS12Asset()

        assertEquals(4, s12.gridRowCount(ActiveLayer.Main))
        assertEquals(4, s12.heightRows(ActiveLayer.Main))
        assertEquals(0, s12.contentRows(ActiveLayer.Main))
        assertEquals(LayerContent.None, s12.contentFor(ActiveLayer.Main))

        assertEquals(4, s12.heightRows(ActiveLayer.Numeric))
        assertEquals(0, s12.contentRows(ActiveLayer.Numeric))

        assertEquals(1, s12.gridRowCount(ActiveLayer.Emoji))
        assertEquals(S12_EMOJI_LAYER_HEIGHT_ROWS, s12.heightRows(ActiveLayer.Emoji))
        assertEquals(5, s12.contentRows(ActiveLayer.Emoji))
        assertEquals(LayerContent.EmojiPicker, s12.contentFor(ActiveLayer.Emoji))

        assertEquals(1, s12.gridRowCount(ActiveLayer.Clipboard))
        assertEquals(S12_CLIPBOARD_LAYER_HEIGHT_ROWS, s12.heightRows(ActiveLayer.Clipboard))
        assertEquals(5, s12.contentRows(ActiveLayer.Clipboard))
        assertEquals(LayerContent.ClipboardHistory, s12.contentFor(ActiveLayer.Clipboard))
    }

    @Test
    fun `settings override cannot shrink a layer below its key grid`() {
        val s12 = loadS12Asset()

        assertEquals(4, s12.heightRows(ActiveLayer.Main, overrideTotal = 2))
        assertEquals(8, s12.heightRows(ActiveLayer.Emoji, overrideTotal = 8))
        assertEquals(4, s12.heightRows(ActiveLayer.Emoji, overrideTotal = 4))
        assertEquals(3, s12.contentRows(ActiveLayer.Emoji, overrideTotal = 4))
        assertEquals(1, s12.heightRows(ActiveLayer.Clipboard, overrideTotal = 1))
        assertEquals(6, s12.heightRows(ActiveLayer.Clipboard, overrideTotal = 6))
        assertEquals(5, s12.contentRows(ActiveLayer.Clipboard, overrideTotal = 6))
    }

    @Test
    fun `available layers follow the document`() {
        val s12 = loadS12Asset()
        assertEquals(
            listOf(
                ActiveLayer.Main,
                ActiveLayer.Numeric,
                ActiveLayer.Emoji,
                ActiveLayer.Clipboard,
            ),
            s12.availableLayers(),
        )
        assertTrue(s12.layer(ActiveLayer.Main) != null)
    }
}
