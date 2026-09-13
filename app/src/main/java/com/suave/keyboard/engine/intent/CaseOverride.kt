package com.suave.keyboard.engine.intent

/**
 * Per-text override of layout [caseMaps](com.suave.keyboard.layout.json).
 * JSON: omit = [Inherit], `null` = [Disable], string = [Fixed].
 */
sealed class CaseOverride {
    data object Inherit : CaseOverride()

    data object Disable : CaseOverride()

    data class Fixed(
        val value: String,
    ) : CaseOverride()
}

data class TextCaseOverrides(
    val shift: CaseOverride = CaseOverride.Inherit,
    val capsLock: CaseOverride = CaseOverride.Inherit,
) {
    val isDefault: Boolean
        get() = shift == CaseOverride.Inherit && capsLock == CaseOverride.Inherit

    companion object {
        val DEFAULT = TextCaseOverrides()
    }
}
