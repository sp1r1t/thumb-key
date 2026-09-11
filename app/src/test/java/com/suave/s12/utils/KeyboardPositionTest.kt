package com.suave.s12.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardPositionTest {
    @Test
    fun `nextVisible toggles Center and Dual and skips the invisible Left Right stops`() {
        assertEquals(KeyboardPosition.Dual, KeyboardPosition.Center.nextVisible())
        assertEquals(KeyboardPosition.Center, KeyboardPosition.Dual.nextVisible())
        assertEquals(KeyboardPosition.Dual, KeyboardPosition.Left.nextVisible())
        assertEquals(KeyboardPosition.Dual, KeyboardPosition.Right.nextVisible())
    }
}
