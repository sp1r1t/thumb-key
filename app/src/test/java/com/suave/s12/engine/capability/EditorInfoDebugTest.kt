package com.suave.s12.engine.capability

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorInfoDebugTest {
    @Test
    fun `null editor is NO_EDITOR`() {
        assertEquals("NO_EDITOR", EditorInfoDebug.describe(editorInfo = null))
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
    fun `password variation is listed without pretending a richer class`() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        assertEquals("EDITABLE . TEXT . PASSWORD", EditorInfoDebug.describe(inputType))
    }

    @Test
    fun `content mime types are orthogonal to class`() {
        val label =
            EditorInfoDebug.describe(
                inputType = InputType.TYPE_CLASS_TEXT,
                contentMimeTypes = arrayOf("image/*"),
            )
        assertEquals("EDITABLE . TEXT . CONTENT:image/*", label)
    }

    @Test
    fun `web edit text is a variation not a capability level`() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT
        assertEquals("EDITABLE . TEXT . WEB_EDIT", EditorInfoDebug.describe(inputType))
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
    fun `ime action and extract flags are listed`() {
        val label =
            EditorInfoDebug.describe(
                inputType = InputType.TYPE_CLASS_TEXT,
                imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI,
            )
        assertEquals("EDITABLE . TEXT . NO_EXTRACT . ACTION:DONE", label)
    }
}
