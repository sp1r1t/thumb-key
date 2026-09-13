package com.suave.keyboard.layout

import com.suave.keyboard.layout.json.loadS12Asset
import org.junit.Assert.assertEquals
import org.junit.Test

class LayerNavigationTest {
    private val s12 by lazy { loadS12Asset() }

    @Test
    fun `custom base is origin when entering emoji`() {
        val custom = ActiveLayer("layer_fn1")
        val session =
            LayerSession()
                .selectBase(custom)
                .switchTo(ActiveLayer.Emoji, s12)
        assertEquals(ActiveLayer.Emoji, session.current)
        assertEquals(custom, session.origin)
        assertEquals(custom, session.leaveOverlay().current)
    }

    @Test
    fun `toggle custom returns to main`() {
        val custom = ActiveLayer("layer_fn1")
        val on = LayerSession().toggleBase(custom)
        assertEquals(custom, on.current)
        assertEquals(ActiveLayer.Main, on.toggleBase(custom).current)
    }

    @Test
    fun `numeric then emoji restores numeric`() {
        val session =
            LayerSession()
                .selectBase(ActiveLayer.Numeric)
                .switchTo(ActiveLayer.Emoji, s12)
                .leaveOverlay()
        assertEquals(ActiveLayer.Numeric, session.current)
    }
}
