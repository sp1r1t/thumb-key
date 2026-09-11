package com.suave.s12.layout

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
                LayoutLayer.MAIN to 5,
            ),
        )
        assertEquals("MAIN=5,EMOJI=8", stored)
        assertEquals(
            mapOf(LayoutLayer.MAIN to 5, LayoutLayer.EMOJI to 8),
            parseLayerHeightOverrides(stored),
        )
    }

    @Test
    fun `unknown names and non-positive values are ignored`() {
        val parsed = parseLayerHeightOverrides("EMOJI=6,FUTURE=9,MAIN=0,NUMERIC=abc")
        assertEquals(mapOf(LayoutLayer.EMOJI to 6), parsed)
    }

    @Test
    fun `Suave emoji default is taller than the bottom row, letters match the grid`() {
        val suave = BuiltinLayouts.SUAVE

        assertEquals(4, suave.gridRowCount(LayoutLayer.MAIN))
        assertEquals(4, suave.heightRows(LayoutLayer.MAIN))
        assertEquals(0, suave.contentRows(LayoutLayer.MAIN))
        assertEquals(LayerContent.None, suave.contentFor(LayoutLayer.MAIN))

        assertEquals(4, suave.heightRows(LayoutLayer.NUMERIC))
        assertEquals(0, suave.contentRows(LayoutLayer.NUMERIC))

        assertEquals(1, suave.gridRowCount(LayoutLayer.EMOJI))
        assertEquals(SUAVE_EMOJI_LAYER_HEIGHT_ROWS, suave.heightRows(LayoutLayer.EMOJI))
        assertEquals(5, suave.contentRows(LayoutLayer.EMOJI))
        assertEquals(LayerContent.EmojiPicker, suave.contentFor(LayoutLayer.EMOJI))
    }

    @Test
    fun `settings override cannot shrink a layer below its key grid`() {
        val suave = BuiltinLayouts.SUAVE

        assertEquals(4, suave.heightRows(LayoutLayer.MAIN, overrideRows = 2))
        assertEquals(8, suave.heightRows(LayoutLayer.EMOJI, overrideRows = 8))
        assertEquals(4, suave.heightRows(LayoutLayer.EMOJI, overrideRows = 4))
        assertEquals(3, suave.contentRows(LayoutLayer.EMOJI, overrideRows = 4))
    }

    @Test
    fun `available layers follow which optional grids the layout actually has`() {
        val suave = BuiltinLayouts.SUAVE
        assertEquals(
            listOf(LayoutLayer.MAIN, LayoutLayer.NUMERIC, LayoutLayer.EMOJI),
            suave.availableLayers(),
        )
        assertTrue(LayoutLayer.MAIN in suave.availableLayers())
    }
}
