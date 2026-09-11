package com.suave.s12.engine.capability

import android.view.inputmethod.EditorInfo

/**
 * The single place that inspects [EditorInfo] to decide what an editor can do. Centralizes what
 * used to be an inline `inputType == EditorInfo.TYPE_NULL` check reached for individually by
 * whichever feature needed to special-case raw editors like Termux (see upstream issue #1065) -
 * features ask [EditorCapabilities] instead of inspecting [EditorInfo] themselves.
 *
 * Only distinguishes RAW from BASIC_EDITABLE_TEXT for now; OBSERVABLE_EDITOR/
 * COOPERATIVE_EXTENDED are defined for later phases (dictionary, ghost-text) that need them but
 * nothing resolves to them yet - see [EditorCapabilityLevel].
 */
object EditorCapabilityResolver {
    fun resolve(editorInfo: EditorInfo?): EditorCapabilities {
        val isRaw = editorInfo == null || editorInfo.inputType == EditorInfo.TYPE_NULL
        val level = if (isRaw) EditorCapabilityLevel.RAW else EditorCapabilityLevel.BASIC_EDITABLE_TEXT
        return EditorCapabilities(level)
    }
}
