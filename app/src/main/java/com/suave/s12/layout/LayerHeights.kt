package com.suave.s12.layout

/** Empty string: every layer uses the [NamedLayout] default (grid size, unless the layout sets one). */
const val DEFAULT_LAYER_HEIGHTS = ""

const val MAX_LAYER_HEIGHT_ROWS = 12

/** Suave emoji: five picker rows plus the functional bottom row. */
const val SUAVE_EMOJI_LAYER_HEIGHT_ROWS = 6

/** Suave clipboard: same total height as the letter grid so the keyboard does not jump. */
const val SUAVE_CLIPBOARD_LAYER_HEIGHT_ROWS = 4

/** Pre-generic compact emoji height (picker filled the three letter rows). Used only to migrate the old expand-emoji toggle off. */
const val COMPACT_EMOJI_LAYER_HEIGHT_ROWS = 4

/**
 * Parses the settings blob (`EMOJI=8,MAIN=5`) into per-layer overrides. Unknown names and
 * non-positive values are ignored so a future layer name does not break older clients.
 */
fun parseLayerHeightOverrides(stored: String): Map<LayoutLayer, Int> {
    if (stored.isBlank()) return emptyMap()
    return stored
        .split(",")
        .mapNotNull { part ->
            val pieces = part.split("=", limit = 2)
            if (pieces.size != 2) return@mapNotNull null
            val layer = LayoutLayer.entries.find { it.name == pieces[0].trim() } ?: return@mapNotNull null
            val rows = pieces[1].trim().toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            layer to rows
        }.toMap()
}

fun formatLayerHeightOverrides(overrides: Map<LayoutLayer, Int>): String =
    LayoutLayer.entries
        .mapNotNull { layer ->
            overrides[layer]?.takeIf { it > 0 }?.let { "${layer.name}=$it" }
        }.joinToString(",")
