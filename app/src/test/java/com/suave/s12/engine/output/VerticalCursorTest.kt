package com.suave.s12.engine.output

import com.suave.s12.engine.action.CursorDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VerticalCursorTest {
    @Test
    fun `down on the last line stays put so callers skip DPAD_DOWN`() {
        val text = "hello"
        assertEquals(5, verticalCursorOffset(text, cursor = 5, CursorDirection.DOWN))
        assertEquals(2, verticalCursorOffset(text, cursor = 2, CursorDirection.DOWN))
    }

    @Test
    fun `up on the first line stays put so callers skip DPAD_UP`() {
        val text = "hello"
        assertEquals(0, verticalCursorOffset(text, cursor = 0, CursorDirection.UP))
        assertEquals(3, verticalCursorOffset(text, cursor = 3, CursorDirection.UP))
    }

    @Test
    fun `down preserves column onto the next logical line`() {
        val text = "abc\ndefg"
        assertEquals(6, verticalCursorOffset(text, cursor = 2, CursorDirection.DOWN)) // 'c' -> 'e'
    }

    @Test
    fun `down clamps to the end of a shorter next line`() {
        val text = "abcd\nef"
        assertEquals(7, verticalCursorOffset(text, cursor = 4, CursorDirection.DOWN)) // end of "abcd" -> end of "ef"
    }

    @Test
    fun `up preserves column onto the previous logical line`() {
        val text = "abc\ndefg"
        assertEquals(2, verticalCursorOffset(text, cursor = 6, CursorDirection.UP)) // 'e' -> 'c'
    }

    @Test
    fun `horizontal directions are not handled here`() {
        assertNull(verticalCursorOffset("a\nb", cursor = 0, CursorDirection.LEFT))
        assertNull(verticalCursorOffset("a\nb", cursor = 0, CursorDirection.RIGHT))
    }
}
