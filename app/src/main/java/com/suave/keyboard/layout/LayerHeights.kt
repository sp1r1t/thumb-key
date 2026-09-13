package com.suave.keyboard.layout

/** Empty string: every layer uses the [NamedLayout] default (grid size, unless the layout sets one). */
const val DEFAULT_LAYER_HEIGHTS = ""

const val MAX_LAYER_HEIGHT_ROWS = 12

/** Suave emoji: five picker rows plus the functional bottom row. */
const val S12_EMOJI_LAYER_HEIGHT_ROWS = 6

/** Suave clipboard: same total height as emoji so both overlay layers match. */
const val S12_CLIPBOARD_LAYER_HEIGHT_ROWS = 6

/** Pre-generic compact emoji height (picker filled the three letter rows). Used only to migrate the old expand-emoji toggle off. */
const val COMPACT_EMOJI_LAYER_HEIGHT_ROWS = 4

/**
 * Parses the settings blob (`emoji=8,main=5`) into per-layer overrides.
 * Also accepts legacy uppercase names (`EMOJI=8`) and maps them to lowercase ids.
 * Unknown names and non-positive values are ignored.
 */
fun parseLayerHeightOverrides(stored: String): Map<String, Int> {
    if (stored.isBlank()) return emptyMap()
    return stored
        .split(",")
        .mapNotNull { part ->
            val pieces = part.split("=", limit = 2)
            if (pieces.size != 2) return@mapNotNull null
            val raw = pieces[0].trim()
            if (raw.isEmpty()) return@mapNotNull null
            val layerId = normalizeLayerHeightKey(raw)
            val rows = pieces[1].trim().toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            layerId to rows
        }.toMap()
}

fun formatLayerHeightOverrides(overrides: Map<String, Int>): String =
    overrides.entries
        .sortedBy { it.key }
        .mapNotNull { (id, rows) -> rows.takeIf { it > 0 }?.let { "$id=$it" } }
        .joinToString(",")

private fun normalizeLayerHeightKey(raw: String): String =
    when (raw) {
        "MAIN" -> ActiveLayer.MAIN
        "NUMERIC" -> ActiveLayer.NUMERIC
        "EMOJI" -> ActiveLayer.EMOJI
        "CLIPBOARD" -> ActiveLayer.CLIPBOARD
        else -> raw
    }
