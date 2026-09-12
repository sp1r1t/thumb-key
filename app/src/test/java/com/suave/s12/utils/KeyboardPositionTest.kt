package com.suave.s12.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardPositionTest {
    @Test
    fun `Split is appended so existing ordinals stay stable`() {
        assertEquals(0, KeyboardPosition.Center.ordinal)
        assertEquals(1, KeyboardPosition.Right.ordinal)
        assertEquals(2, KeyboardPosition.Left.ordinal)
        assertEquals(3, KeyboardPosition.Dual.ordinal)
        assertEquals(4, KeyboardPosition.Split.ordinal)
    }
}
