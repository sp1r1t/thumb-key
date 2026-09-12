package com.suave.s12.engine.output

/** How Paste should deliver the clipboard into the focused editor. */
enum class PasteKind {
    /** Termux-style editors: send the terminal's own paste shortcut. */
    RAW_SHORTCUT,

    /** Private clipboard last wrote text we own: [android.view.inputmethod.InputConnection.commitText]. */
    COMMIT_TEXT,

    /** Editor declared a matching content MIME type and the clipboard has that payload. */
    COMMIT_CONTENT,

    /** Ordinary text paste via the editor's context-menu paste action. */
    CONTEXT_MENU_PASTE,
}

/**
 * Chooses the paste primitive without talking to Android. [hasMatchingCommitContent] is true
 * only when the editor's `contentMimeTypes` accept a MIME type the current clipboard can offer.
 */
object PasteDecision {
    fun kind(
        editorIsRaw: Boolean,
        useInternalClipboardText: Boolean,
        hasMatchingCommitContent: Boolean,
    ): PasteKind =
        when {
            editorIsRaw -> PasteKind.RAW_SHORTCUT
            useInternalClipboardText -> PasteKind.COMMIT_TEXT
            hasMatchingCommitContent -> PasteKind.COMMIT_CONTENT
            else -> PasteKind.CONTEXT_MENU_PASTE
        }
}
