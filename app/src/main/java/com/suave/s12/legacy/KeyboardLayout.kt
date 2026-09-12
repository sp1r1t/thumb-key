package com.suave.s12.legacy

import com.suave.s12.legacy.keyboards.KB_DE_TYPESPLIT_SUAVE
import com.suave.s12.legacy.utils.KeyboardDefinition

// Suave is the only layout this fork ships; the historical multi-layout registry (and its
// ordinal-stability requirement) was removed when this branch cut over to Suave-only.
enum class KeyboardLayout(
    val keyboardDefinition: KeyboardDefinition,
) {
    DETypeSplitSuave(KB_DE_TYPESPLIT_SUAVE), // deutsch type-split suave
}
