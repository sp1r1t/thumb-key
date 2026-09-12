package com.suave.s12.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class LayerNavigationTest {
    @Test
    fun `clipboard opened from numbers returns to numbers`() {
        val onNumeric = LayerSession().selectBaseLayer(LayoutLayer.NUMERIC)
        val onClipboard = onNumeric.toggleClipboard(available = true)
        assertEquals(LayoutLayer.CLIPBOARD, onClipboard.layer)
        assertEquals(LayoutLayer.NUMERIC, onClipboard.origin)
        assertEquals(LayoutLayer.NUMERIC, onClipboard.toggleClipboard(available = true).layer)
    }

    @Test
    fun `clipboard opened from letters returns to letters`() {
        val onClipboard = LayerSession().toggleClipboard(available = true)
        assertEquals(LayoutLayer.CLIPBOARD, onClipboard.layer)
        assertEquals(LayoutLayer.MAIN, onClipboard.origin)
        assertEquals(LayoutLayer.MAIN, onClipboard.leaveOverlay().layer)
    }

    @Test
    fun `emoji opened from numbers returns to numbers, not letters`() {
        val onNumeric = LayerSession().selectBaseLayer(LayoutLayer.NUMERIC)
        val onEmoji = onNumeric.toggleEmoji(available = true)
        assertEquals(LayoutLayer.EMOJI, onEmoji.layer)
        assertEquals(LayoutLayer.NUMERIC, onEmoji.origin)
        assertEquals(LayoutLayer.NUMERIC, onEmoji.toggleEmoji(available = true).layer)
    }

    @Test
    fun `switching overlay keeps the original letter or number grid`() {
        val viaClipboard =
            LayerSession()
                .selectBaseLayer(LayoutLayer.NUMERIC)
                .toggleClipboard(available = true)
                .toggleEmoji(available = true)
        assertEquals(LayoutLayer.EMOJI, viaClipboard.layer)
        assertEquals(LayoutLayer.NUMERIC, viaClipboard.origin)
        assertEquals(LayoutLayer.NUMERIC, viaClipboard.toggleEmoji(available = true).layer)
    }

    @Test
    fun `explicit 123 or ABC leaves the overlay for that grid`() {
        val onClipboard = LayerSession().selectBaseLayer(LayoutLayer.NUMERIC).toggleClipboard(available = true)
        assertEquals(LayoutLayer.MAIN, onClipboard.selectBaseLayer(LayoutLayer.MAIN).layer)
        assertEquals(LayoutLayer.NUMERIC, onClipboard.selectBaseLayer(LayoutLayer.NUMERIC).layer)
    }

    @Test
    fun `missing overlay grids are no-ops`() {
        val idle = LayerSession()
        assertEquals(idle, idle.toggleEmoji(available = false))
        assertEquals(idle, idle.toggleClipboard(available = false))
    }
}
