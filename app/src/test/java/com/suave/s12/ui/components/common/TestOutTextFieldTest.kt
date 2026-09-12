package com.suave.s12.ui.components.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestOutTextFieldTest {
    @Test
    fun `field stays open until the keyboard has actually shown`() {
        assertFalse(shouldCollapseTestField(showField = true, imeVisible = false, imeHadShown = false))
        assertFalse(shouldCollapseTestField(showField = true, imeVisible = true, imeHadShown = false))
        assertFalse(shouldCollapseTestField(showField = true, imeVisible = true, imeHadShown = true))
    }

    @Test
    fun `field collapses after the keyboard has shown and then hidden`() {
        assertTrue(shouldCollapseTestField(showField = true, imeVisible = false, imeHadShown = true))
        assertFalse(shouldCollapseTestField(showField = false, imeVisible = false, imeHadShown = true))
    }
}
