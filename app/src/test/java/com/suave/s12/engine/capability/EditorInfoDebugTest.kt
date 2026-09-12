package com.suave.s12.engine.capability

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorInfoDebugTest {
    @Test
    fun `null editor is NO_EDITOR`() {
        val label = EditorInfoDebug.label(editorInfo = null)
        assertEquals("NO_EDITOR", label.compact)
        assertEquals("NO_EDITOR", label.verbose)
    }

    @Test
    fun `Termux-style raw field is TYPE_NULL`() {
        assertEquals("RAW . TYPE_NULL", EditorInfoDebug.describe(InputType.TYPE_NULL))
    }

    @Test
    fun `plain text is editable text`() {
        assertEquals("EDITABLE . TEXT", EditorInfoDebug.describe(InputType.TYPE_CLASS_TEXT))
    }

    @Test
    fun `multiline text lists MULTILINE`() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        assertEquals("EDITABLE . TEXT . MULTILINE", EditorInfoDebug.describe(inputType))
    }

    @Test
    fun `IME multiline alone does not count as MULTILINE`() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE
        assertEquals("EDITABLE . TEXT", EditorInfoDebug.describe(inputType))
        assertTrue(EditorInfoDebug.verbose(inputType).contains("IME_MULTILINE"))
        assertFalse(EditorInfoDebug.verbose(inputType).contains(" . MULTILINE ."))
    }

    @Test
    fun `password variation is listed without pretending a richer class`() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        assertEquals("EDITABLE . TEXT . PASSWORD", EditorInfoDebug.describe(inputType))
    }

    @Test
    fun `content mime types are orthogonal to class`() {
        val label =
            EditorInfoDebug.label(
                inputType = InputType.TYPE_CLASS_TEXT,
                contentMimeTypes = arrayOf("image/gif", "image/jpeg"),
            )
        assertEquals("EDITABLE . TEXT . [gif, jpeg]", label.compact)
        assertTrue(label.verbose.contains("CONTENT:image/gif,image/jpeg"))
    }

    @Test
    fun `compact mime drops image type and CONTENT prefix`() {
        assertEquals("[gif, jpeg]", compactContentMimes(arrayOf("image/gif", "image/jpeg")))
        assertEquals("[image]", compactContentMimes(arrayOf("image/*")))
        assertEquals("[image/gif, text/plain]", compactContentMimes(arrayOf("image/gif", "text/plain")))
    }

    @Test
    fun `web edit text is a variation not a capability level`() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT
        assertEquals("EDITABLE . TEXT . WEB_EDIT", EditorInfoDebug.describe(inputType))
    }

    @Test
    fun `Firefox web login drops default IME noise`() {
        val inputType =
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT or
                InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or
                InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE
        val imeOptions =
            EditorInfo.IME_ACTION_NEXT or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                EditorInfo.IME_FLAG_NO_FULLSCREEN
        val label = EditorInfoDebug.label(inputType, imeOptions)
        assertEquals("EDITABLE . TEXT . WEB_EDIT", label.compact)
        assertTrue(label.verbose.contains("AUTO_CORRECT"))
        assertTrue(label.verbose.contains("IME_MULTILINE"))
        assertTrue(label.verbose.contains("NO_EXTRACT"))
        assertTrue(label.verbose.contains("NO_FULLSCREEN"))
        assertTrue(label.verbose.contains("ACTION:NEXT"))
        assertTrue(label.verbose.contains("inputType=0x"))
    }

    @Test
    fun `number password and decimal flags stack`() {
        val inputType =
            InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_VARIATION_PASSWORD or
                InputType.TYPE_NUMBER_FLAG_DECIMAL
        assertEquals("EDITABLE . NUMBER . PASSWORD . DECIMAL", EditorInfoDebug.describe(inputType))
    }

    @Test
    fun `ime action and extract flags stay on the verbose dump`() {
        val label =
            EditorInfoDebug.label(
                inputType = InputType.TYPE_CLASS_TEXT,
                imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI,
            )
        assertEquals("EDITABLE . TEXT", label.compact)
        assertTrue(label.verbose.contains("NO_EXTRACT"))
        assertTrue(label.verbose.contains("ACTION:DONE"))
    }
}
