package com.suave.s12.layout

import com.suave.s12.engine.intent.Layout

/**
 * Which grid a [NamedLayout] is currently showing. This is a layout switch, not a modifier:
 * numeric and emoji replace the letter grid the same way a second NamedLayout would, without
 * tearing down [com.suave.s12.engine.modifier.ModifierState].
 */
enum class LayoutLayer {
    MAIN,
    NUMERIC,
    EMOJI,
}

/**
 * A first-class layout the engine can persist, select, and render. Suave is one entry in
 * [BuiltinLayouts.ALL], not a privileged singleton - additional layouts join the same list
 * without a second rendering path. The in-app editor (later) should produce this type.
 *
 * [numericLayout] and [emojiBottomRow] are optional layers of this layout, not separate
 * registry entries (language switch still walks [BuiltinLayouts.ALL]). Emoji is a picker
 * plus one functional row rather than a full grid.
 */
data class NamedLayout(
    val id: String,
    val title: String,
    val layout: Layout,
    val numericLayout: Layout? = null,
    val emojiBottomRow: Layout? = null,
    val shiftMappings: Map<String, String> = emptyMap(),
) {
    fun gridFor(layer: LayoutLayer): Layout =
        when (layer) {
            LayoutLayer.MAIN -> layout
            LayoutLayer.NUMERIC -> numericLayout ?: layout
            LayoutLayer.EMOJI -> emojiBottomRow ?: layout
        }
}

object BuiltinLayouts {
    val SUAVE =
        NamedLayout(
            id = "suave",
            title = "Suave",
            layout = SUAVE_LAYOUT,
            numericLayout = SUAVE_NUMERIC_LAYOUT,
            emojiBottomRow = SUAVE_EMOJI_BOTTOM_ROW,
            shiftMappings = SUAVE_SHIFT_MAPPINGS,
        )

    val ALL: List<NamedLayout> = listOf(SUAVE)

    fun byIndex(index: Int): NamedLayout = ALL.getOrElse(index) { SUAVE }

    fun enabledFromDb(indices: String?): List<NamedLayout> {
        val parsed =
            indices
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.map { byIndex(it) }
                ?.distinct()
                .orEmpty()
        return parsed.ifEmpty { listOf(SUAVE) }
    }
}
