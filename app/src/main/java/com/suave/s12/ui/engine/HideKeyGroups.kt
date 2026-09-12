package com.suave.s12.ui.engine

import com.suave.s12.db.AppSettings
import com.suave.s12.utils.toBool
import com.suave.s12.utils.toInt

/**
 * Chip order in Appearance, matching the hide-letter / hide-symbol / ... rows so the
 * hide-key picker and those switches stay visually aligned.
 */
val HIDE_KEY_GROUP_ORDER: List<LegendCategory> =
    listOf(
        LegendCategory.LETTER,
        LegendCategory.SYMBOL,
        LegendCategory.NUMBER,
        LegendCategory.MODIFIER,
        LegendCategory.LAYER_SWITCH,
        LegendCategory.SPECIAL,
        LegendCategory.NAVIGATION,
        LegendCategory.EDITING,
    )

fun parseHideKeyCategories(stored: String): Set<LegendCategory> {
    val parsed =
        stored
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { name -> LegendCategory.entries.find { it.name == name } }
            .toSet()
    return parsed.ifEmpty { setOf(LegendCategory.LETTER) }
}

fun formatHideKeyCategories(categories: Set<LegendCategory>): String {
    val selected = categories.ifEmpty { setOf(LegendCategory.LETTER) }
    return HIDE_KEY_GROUP_ORDER.filter { it in selected }.joinToString(",") { it.name }
}

fun toggleHideKeyGroupSelection(
    selected: Set<LegendCategory>,
    category: LegendCategory,
): Set<LegendCategory> {
    val next = if (category in selected) selected - category else selected + category
    return next.ifEmpty { setOf(LegendCategory.LETTER) }
}

/**
 * Master-toggle for [selected] groups. If any selected group is visible, hide all of them.
 * If they are all already hidden, show all of them. Groups not in [selected] stay as they are.
 */
fun toggleHideKeyGroups(
    selected: Set<LegendCategory>,
    visibility: LegendVisibility,
): LegendVisibility {
    val categories = selected.ifEmpty { setOf(LegendCategory.LETTER) }
    val shouldHide = categories.any { !visibility.hides(it) }
    return categories.fold(visibility) { next, category ->
        next.copyHidden(category, shouldHide)
    }
}

fun toggleHideLabels(settings: AppSettings): AppSettings {
    val next =
        toggleHideKeyGroups(
            parseHideKeyCategories(settings.hideKeyCategories),
            settings.toLegendVisibility(),
        )
    return settings.copy(
        hideLetters = next.hideLetters.toInt(),
        hideSymbols = next.hideSymbols.toInt(),
        hideNumbers = next.hideNumbers.toInt(),
        hideModifiers = next.hideModifiers.toInt(),
        hideLayerSwitches = next.hideLayerSwitches.toInt(),
        hideSpecials = next.hideSpecials.toInt(),
        hideNavigation = next.hideNavigation.toInt(),
        hideEditing = next.hideEditing.toInt(),
    )
}

internal fun AppSettings.toLegendVisibility(): LegendVisibility =
    LegendVisibility(
        hideLetters = hideLetters.toBool(),
        hideSymbols = hideSymbols.toBool(),
        hideNumbers = hideNumbers.toBool(),
        hideModifiers = hideModifiers.toBool(),
        hideLayerSwitches = hideLayerSwitches.toBool(),
        hideSpecials = hideSpecials.toBool(),
        hideNavigation = hideNavigation.toBool(),
        hideEditing = hideEditing.toBool(),
    )

private fun LegendVisibility.copyHidden(
    category: LegendCategory,
    hidden: Boolean,
): LegendVisibility =
    when (category) {
        LegendCategory.LETTER -> copy(hideLetters = hidden)
        LegendCategory.NUMBER -> copy(hideNumbers = hidden)
        LegendCategory.SYMBOL -> copy(hideSymbols = hidden)
        LegendCategory.MODIFIER -> copy(hideModifiers = hidden)
        LegendCategory.LAYER_SWITCH -> copy(hideLayerSwitches = hidden)
        LegendCategory.SPECIAL -> copy(hideSpecials = hidden)
        LegendCategory.NAVIGATION -> copy(hideNavigation = hidden)
        LegendCategory.EDITING -> copy(hideEditing = hidden)
    }
