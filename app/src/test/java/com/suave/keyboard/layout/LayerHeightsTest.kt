package com.suave.keyboard.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayerHeightsTest {
    @Test
    fun `empty blob means no overrides`() {
        assertEquals(emptyMap<LayoutLayer, Int>(), parseLayerHeightOverrides(""))
        assertEquals(emptyMap<LayoutLayer, Int>(), parseLayerHeightOverrides("   "))
        assertEquals("", formatLayerHeightOverrides(emptyMap()))
    }

    @Test
    fun `round trip keeps known layers in enum order`() {
        val stored = formatLayerHeightOverrides(
            mapOf(
                LayoutLayer.EMOJI to 8,
                LayoutLayer.CLIPBOARD to 6,
                LayoutLayer.MAIN to 5,
            ),
        )
        assertEquals("MAIN=5,EMOJI=8,CLIPBOARD=6", stored)
        assertEquals(
            mapOf(LayoutLayer.MAIN to 5, LayoutLayer.EMOJI to 8, LayoutLayer.CLIPBOARD to 6),
            parseLayerHeightOverrides(stored),
        )
    }

    @Test
    fun `unknown names and non-positive values are ignored`() {
        val parsed = parseLayerHeightOverrides("EMOJI=6,CLIPBOARD=7,FUTURE=9,MAIN=0,NUMERIC=abc")
        assertEquals(mapOf(LayoutLayer.EMOJI to 6, LayoutLayer.CLIPBOARD to 7), parsed)
    }

    @Test
    fun `S12 emoji default is taller than the bottom row, letters match the grid`() {
        val s12 = BuiltinLayouts.S12

        assertEquals(4, s12.gridRowCount(LayoutLayer.MAIN))
        assertEquals(4, s12.heightRows(LayoutLayer.MAIN))
        assertEquals(0, s12.contentRows(LayoutLayer.MAIN))
        assertEquals(LayerContent.None, s12.contentFor(LayoutLayer.MAIN))

        assertEquals(4, s12.heightRows(LayoutLayer.NUMERIC))
        assertEquals(0, s12.contentRows(LayoutLayer.NUMERIC))

        assertEquals(1, s12.gridRowCount(LayoutLayer.EMOJI))
        assertEquals(S12_EMOJI_LAYER_HEIGHT_ROWS, s12.heightRows(LayoutLayer.EMOJI))
        assertEquals(5, s12.contentRows(LayoutLayer.EMOJI))
        assertEquals(LayerContent.EmojiPicker, s12.contentFor(LayoutLayer.EMOJI))

        assertEquals(1, s12.gridRowCount(LayoutLayer.CLIPBOARD))
        assertEquals(S12_CLIPBOARD_LAYER_HEIGHT_ROWS, s12.heightRows(LayoutLayer.CLIPBOARD))
        assertEquals(5, s12.contentRows(LayoutLayer.CLIPBOARD))
        assertEquals(LayerContent.ClipboardHistory, s12.contentFor(LayoutLayer.CLIPBOARD))
    }

    @Test
    fun `settings override cannot shrink a layer below its key grid`() {
        val s12 = BuiltinLayouts.S12

        assertEquals(4, s12.heightRows(LayoutLayer.MAIN, overrideRows = 2))
        assertEquals(8, s12.heightRows(LayoutLayer.EMOJI, overrideRows = 8))
        assertEquals(4, s12.heightRows(LayoutLayer.EMOJI, overrideRows = 4))
        assertEquals(3, s12.contentRows(LayoutLayer.EMOJI, overrideRows = 4))
        assertEquals(1, s12.heightRows(LayoutLayer.CLIPBOARD, overrideRows = 1))
        assertEquals(6, s12.heightRows(LayoutLayer.CLIPBOARD, overrideRows = 6))
        assertEquals(5, s12.contentRows(LayoutLayer.CLIPBOARD, overrideRows = 6))
        // Numbers has no content panel; height always matches the key grid.
        assertEquals(4, s12.heightRows(LayoutLayer.NUMERIC, overrideRows = 8))
        assertEquals(0, s12.contentRows(LayoutLayer.NUMERIC, overrideRows = 8))
    }

    @Test
    fun `available layers follow which optional grids the layout actually has`() {
        val s12 = BuiltinLayouts.S12
        assertEquals(
            listOf(LayoutLayer.MAIN, LayoutLayer.NUMERIC, LayoutLayer.EMOJI, LayoutLayer.CLIPBOARD),
            s12.availableBuiltinLayers(),
        )
        assertTrue(LayoutLayer.MAIN in s12.availableBuiltinLayers())
        assertEquals(
            listOf(
                ActiveLayer.Main,
                ActiveLayer.Numeric,
                ActiveLayer.Emoji,
                ActiveLayer.Clipboard,
            ),
            s12.availableLayers(),
        )
    }
}
