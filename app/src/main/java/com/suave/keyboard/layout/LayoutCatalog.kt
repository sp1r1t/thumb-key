package com.suave.keyboard.layout

/** Start-from id for an empty grid in the Add layout picker. */
const val BLANK_LAYOUT_SOURCE_ID = "blank"

enum class LayoutSort {
    TITLE,
    UPDATED,
}

data class StartLayoutChoice(
    val id: String,
    val title: String,
    val tags: List<String>,
    val isBlank: Boolean = false,
)

data class LayoutIdSelection(
    val activeId: String,
    val enabledCsv: String,
)

fun normalizeTags(raw: Iterable<String>): List<String> =
    raw.map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .distinct()

fun tagsToIndex(tags: List<String>): String = normalizeTags(tags).joinToString(",")

fun tagsFromIndex(value: String?): List<String> =
    if (value.isNullOrBlank()) {
        emptyList()
    } else {
        normalizeTags(value.split(',', ';'))
    }

fun parseLayoutIds(csv: String?): List<String> =
    csv
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty()

fun layoutMatchesQuery(
    title: String,
    id: String,
    tags: List<String>,
    query: String,
): Boolean {
    val needle = query.trim()
    if (needle.isEmpty()) return true
    if (title.contains(needle, ignoreCase = true)) return true
    if (id.contains(needle, ignoreCase = true)) return true
    return tags.any { it.contains(needle, ignoreCase = true) }
}

fun sortLayoutIndex(
    rows: List<UserLayoutIndex>,
    sort: LayoutSort,
): List<UserLayoutIndex> =
    when (sort) {
        LayoutSort.TITLE -> rows.sortedBy { it.title.lowercase() }
        LayoutSort.UPDATED -> rows.sortedByDescending { it.updatedAt }
    }

fun filterAvailableLayouts(
    rows: List<UserLayoutIndex>,
    query: String,
    sort: LayoutSort,
): List<UserLayoutIndex> =
    sortLayoutIndex(
        rows.filter { layoutMatchesQuery(it.title, it.id, tagsFromIndex(it.tags), query) },
        sort,
    )

fun rewriteLayoutIds(
    activeId: String?,
    enabledCsv: String?,
    replacements: Map<String, String>,
    fallbackId: String = LayoutRegistry.DEFAULT_ID,
): LayoutIdSelection {
    val enabled =
        parseLayoutIds(enabledCsv)
            .ifEmpty {
                listOfNotNull(activeId?.takeIf { it.isNotBlank() }).ifEmpty { listOf(fallbackId) }
            }
    val rewrittenEnabled =
        enabled
            .map { replacements[it] ?: it }
            .distinct()
            .ifEmpty { listOf(fallbackId) }
    val rewrittenActive =
        replacements[activeId]
            ?: activeId?.takeIf { it in rewrittenEnabled }
            ?: rewrittenEnabled.first()
    return LayoutIdSelection(
        activeId = rewrittenActive,
        enabledCsv = rewrittenEnabled.joinToString(","),
    )
}

fun startLayoutChoices(
    templates: List<NamedLayout>,
    query: String,
    blankTitle: String = "Blank layout",
): List<StartLayoutChoice> {
    val blank =
        StartLayoutChoice(
            id = BLANK_LAYOUT_SOURCE_ID,
            title = blankTitle,
            tags = listOf("blank"),
            isBlank = true,
        )
    val fromTemplates =
        templates
            .sortedBy { it.title.lowercase() }
            .map { layout ->
                StartLayoutChoice(
                    id = layout.id,
                    title = layout.title,
                    tags = layout.tags,
                    isBlank = false,
                )
            }
    val all = listOf(blank) + fromTemplates
    return all.filter { choice ->
        layoutMatchesQuery(choice.title, choice.id, choice.tags, query)
    }
}
