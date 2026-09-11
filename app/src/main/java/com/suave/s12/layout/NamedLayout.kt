package com.suave.s12.layout

import com.suave.s12.engine.intent.Layout

/**
 * A first-class layout the engine can persist, select, and render. Suave is one entry in
 * [BuiltinLayouts.ALL], not a privileged singleton - additional layouts join the same list
 * without a second rendering path. The in-app editor (later) should produce this type.
 */
data class NamedLayout(
    val id: String,
    val title: String,
    val layout: Layout,
    val shiftMappings: Map<String, String> = emptyMap(),
)

object BuiltinLayouts {
    val SUAVE =
        NamedLayout(
            id = "suave",
            title = "Suave",
            layout = SUAVE_LAYOUT,
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
