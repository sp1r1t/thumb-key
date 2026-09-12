package com.suave.s12.layout

import com.suave.s12.engine.intent.Layout
import com.suave.s12.engine.intent.bottomRow
import com.suave.s12.engine.intent.layoutRows
import kotlin.math.max

/**
 * Which grid a [NamedLayout] is currently showing. This is a layout switch, not a modifier:
 * numeric, emoji, and clipboard replace the letter grid the same way a second NamedLayout would,
 * without tearing down [com.suave.s12.engine.modifier.ModifierState].
 */
enum class LayoutLayer {
    MAIN,
    NUMERIC,
    EMOJI,
    CLIPBOARD,
}

/**
 * What fills the space above a layer's key grid when that layer is taller than its rows.
 * The renderer does not special-case a layer by name: extra height is always this slot.
 */
sealed class LayerContent {
    data object None : LayerContent()

    data object EmojiPicker : LayerContent()

    data object ClipboardHistory : LayerContent()
}

/**
 * A first-class layout the engine can persist, select, and render. Suave is one entry in
 * [BuiltinLayouts.ALL], not a privileged singleton - additional layouts join the same list
 * without a second rendering path. The in-app editor (later) should produce this type.
 *
 * [numericLayout], [emojiBottomRow], and [clipboardBottomRow] are optional layers of this
 * layout, not separate registry entries (language switch still walks [BuiltinLayouts.ALL]).
 *
 * [layerHeights] is the default total height in key-height units per layer. Missing entries
 * use the grid's row count. Extra rows sit above the keys and show [layerContent] (emoji
 * picker, clipboard list). User overrides of these heights live in settings, not here.
 */
data class NamedLayout(
    val id: String,
    val title: String,
    val layout: Layout,
    val numericLayout: Layout? = null,
    val emojiBottomRow: Layout? = null,
    val clipboardBottomRow: Layout? = null,
    val shiftMappings: Map<String, String> = emptyMap(),
    val layerHeights: Map<LayoutLayer, Int> = emptyMap(),
    val layerContent: Map<LayoutLayer, LayerContent> = emptyMap(),
) {
    fun gridFor(layer: LayoutLayer): Layout =
        when (layer) {
            LayoutLayer.MAIN -> layout
            LayoutLayer.NUMERIC -> numericLayout ?: layout
            LayoutLayer.EMOJI -> emojiBottomRow ?: layout
            LayoutLayer.CLIPBOARD -> clipboardBottomRow ?: layout.bottomRow()
        }

    fun availableLayers(): List<LayoutLayer> =
        buildList {
            add(LayoutLayer.MAIN)
            if (numericLayout != null) add(LayoutLayer.NUMERIC)
            if (emojiBottomRow != null) add(LayoutLayer.EMOJI)
            if (clipboardBottomRow != null || layerContent[LayoutLayer.CLIPBOARD] != null) {
                add(LayoutLayer.CLIPBOARD)
            }
        }

    fun gridRowCount(layer: LayoutLayer): Int = layoutRows(gridFor(layer)).size.coerceAtLeast(1)

    fun contentFor(layer: LayoutLayer): LayerContent = layerContent[layer] ?: LayerContent.None

    /**
     * Total keyboard height in key-height units. [overrideRows] of 0 or less means "use this
     * layout's default". Never shorter than the key grid, so keys are not clipped.
     */
    fun heightRows(
        layer: LayoutLayer,
        overrideRows: Int = 0,
    ): Int {
        val gridRows = gridRowCount(layer)
        val requested = overrideRows.takeIf { it > 0 } ?: layerHeights[layer]
        return max(gridRows, requested ?: gridRows)
    }

    fun contentRows(
        layer: LayoutLayer,
        overrideRows: Int = 0,
    ): Int = heightRows(layer, overrideRows) - gridRowCount(layer)
}

object BuiltinLayouts {
    val SUAVE =
        NamedLayout(
            id = "suave",
            title = "Suave",
            layout = SUAVE_LAYOUT,
            numericLayout = SUAVE_NUMERIC_LAYOUT,
            emojiBottomRow = SUAVE_EMOJI_BOTTOM_ROW,
            clipboardBottomRow = SUAVE_CLIPBOARD_BOTTOM_ROW,
            shiftMappings = SUAVE_SHIFT_MAPPINGS,
            layerHeights =
                mapOf(
                    LayoutLayer.EMOJI to SUAVE_EMOJI_LAYER_HEIGHT_ROWS,
                    LayoutLayer.CLIPBOARD to SUAVE_CLIPBOARD_LAYER_HEIGHT_ROWS,
                ),
            layerContent =
                mapOf(
                    LayoutLayer.EMOJI to LayerContent.EmojiPicker,
                    LayoutLayer.CLIPBOARD to LayerContent.ClipboardHistory,
                ),
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

    /** True when cycling layouts would land on a different one. */
    fun canSwitch(indices: String?): Boolean = enabledFromDb(indices).size > 1
}
