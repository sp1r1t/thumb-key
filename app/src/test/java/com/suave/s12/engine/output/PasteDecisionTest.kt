package com.suave.s12.engine.output

import org.junit.Assert.assertEquals
import org.junit.Test

class PasteDecisionTest {
    @Test
    fun `raw editors always get the terminal paste shortcut`() {
        assertEquals(
            PasteKind.RAW_SHORTCUT,
            PasteDecision.kind(
                editorIsRaw = true,
                useInternalClipboardText = false,
                hasMatchingCommitContent = true,
            ),
        )
    }

    @Test
    fun `private internal text wins over a clipboard image`() {
        assertEquals(
            PasteKind.COMMIT_TEXT,
            PasteDecision.kind(
                editorIsRaw = false,
                useInternalClipboardText = true,
                hasMatchingCommitContent = true,
            ),
        )
    }

    @Test
    fun `matching content mime uses commitContent instead of commitText`() {
        assertEquals(
            PasteKind.COMMIT_CONTENT,
            PasteDecision.kind(
                editorIsRaw = false,
                useInternalClipboardText = false,
                hasMatchingCommitContent = true,
            ),
        )
    }

    @Test
    fun `no matching content mime keeps ordinary text paste`() {
        assertEquals(
            PasteKind.CONTEXT_MENU_PASTE,
            PasteDecision.kind(
                editorIsRaw = false,
                useInternalClipboardText = false,
                hasMatchingCommitContent = false,
            ),
        )
        assertEquals(
            PasteKind.COMMIT_TEXT,
            PasteDecision.kind(
                editorIsRaw = false,
                useInternalClipboardText = true,
                hasMatchingCommitContent = false,
            ),
        )
    }
}
