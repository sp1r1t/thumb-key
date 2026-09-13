package com.suave.keyboard.layout

/**
 * S12's shift mapping: irregular capitalizations/combos that don't just uppercase (German
 * umlauts, "sch"/"ch" digraphs, "ß" -> "SS"). Shipped layout JSON carries the same maps under
 * `caseMaps`; these constants remain for tests and docs that talk about orthography without
 * loading the full asset.
 *
 * One-shot / held Shift title-cases digraphs ("Sch"); caps lock only overrides tokens that
 * differ ([S12_CAPS_LOCK_MAPPINGS]) and otherwise falls back to this table.
 */
val S12_SHIFT_MAPPINGS: Map<String, String> =
    mapOf(
        "ö" to "Ö",
        "ä" to "Ä",
        "ü" to "Ü",
        "ß" to "SS",
        "sch" to "Sch",
        "ch" to "Ch",
    )

/** Caps-lock overrides on top of [S12_SHIFT_MAPPINGS] (e.g. "sch" -> "SCH" instead of "Sch"). */
val S12_CAPS_LOCK_MAPPINGS: Map<String, String> =
    mapOf(
        "sch" to "SCH",
        "ch" to "CH",
    )
