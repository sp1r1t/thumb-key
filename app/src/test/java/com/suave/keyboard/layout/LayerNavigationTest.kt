package com.suave.keyboard.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class LayerNavigationTest {
    @Test
    fun `custom base is origin when entering emoji`() {
        val custom = ActiveLayer.Custom("custom_fn1")
        val session =
            LayerSession()
                .selectBase(custom)
                .enterOverlay(LayoutLayer.EMOJI)
        assertEquals(ActiveLayer.Emoji, session.current)
        assertEquals(custom, session.origin)
        assertEquals(custom, session.leaveOverlay().current)
    }

    @Test
    fun `toggle custom returns to main`() {
        val custom = ActiveLayer.Custom("custom_fn1")
        val on = LayerSession().toggleBase(custom)
        assertEquals(custom, on.current)
        assertEquals(ActiveLayer.Main, on.toggleBase(custom).current)
    }

    @Test
    fun `numeric then emoji restores numeric`() {
        val session =
            LayerSession()
                .selectBuiltinBase(LayoutLayer.NUMERIC)
                .enterOverlay(LayoutLayer.EMOJI)
                .leaveOverlay()
        assertEquals(ActiveLayer.Numeric, session.current)
    }
}
