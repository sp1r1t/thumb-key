package com.suave.s12.engine.capability

/**
 * What the currently focused editor can reliably do, resolved once by
 * [EditorCapabilityResolver] rather than checked ad hoc wherever a feature needs to know. Levels
 * are ordered (a Kotlin enum's declaration order gives it [Comparable] for free) so callers can
 * write `level >= OBSERVABLE_EDITOR` instead of an enum-equality chain.
 */
enum class EditorCapabilityLevel {
    /** Raw input targets (e.g. a terminal emulator): assume nothing beyond real KeyEvents. */
    RAW,

    /** Normal `InputConnection.commitText`/key-event functionality is available. */
    BASIC_EDITABLE_TEXT,

    /** Surrounding text, selection, and composition can reliably be queried. */
    OBSERVABLE_EDITOR,

    /** The app explicitly exposes additional/cooperative IME capabilities. */
    COOPERATIVE_EXTENDED,
}

data class EditorCapabilities(
    val level: EditorCapabilityLevel,
) {
    val supportsCommitText: Boolean get() = level >= EditorCapabilityLevel.BASIC_EDITABLE_TEXT
    val supportsSurroundingTextQueries: Boolean get() = level >= EditorCapabilityLevel.OBSERVABLE_EDITOR
}
