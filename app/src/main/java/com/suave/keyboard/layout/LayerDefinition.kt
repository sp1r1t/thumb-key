package com.suave.keyboard.layout

import com.suave.keyboard.engine.intent.Layout
import com.suave.keyboard.engine.intent.layoutRows
import kotlin.math.max

/** What fills a content strip above (or between) key rows on a layer. */
sealed class LayerContent {
    data object None : LayerContent()

    data object EmojiPicker : LayerContent()

    data object ClipboardHistory : LayerContent()
}

/**
 * Icons for any layer chip (builtin or user). Names match layout JSON `icon` strings.
 */
enum class LayerIcon {
    Abc,
    Numbers,
    EmojiEmotions,
    History,
    Functions,
    Tag,
    Star,
    Build,
    Extension,
    Code,
    Bolt,
    GridView,
    Widgets,
    Category,
}

/**
 * One layer in a [NamedLayout]: identity for switching, plus a compiled key grid and optional
 * full-width content strip (`contentRows` key-height units).
 */
data class LayerDefinition(
    val id: String,
    val title: String,
    val icon: LayerIcon = LayerIcon.Functions,
    val overlay: Boolean = false,
    val keyGrid: Layout,
    val content: LayerContent = LayerContent.None,
    val contentRows: Int = 0,
    /** Preserved for round-trip when a content cell declares width (not packed yet). */
    val contentColumnSpan: Float = 1f,
) {
    init {
        require(id.isNotBlank()) { "Layer id must not be blank" }
        require(title.isNotBlank()) { "Layer title must not be blank" }
        require(contentRows >= 0) { "contentRows must be >= 0" }
        require(contentColumnSpan > 0f) { "contentColumnSpan must be > 0" }
        if (content == LayerContent.None) {
            require(contentRows == 0) { "None content cannot have contentRows" }
        }
    }

    fun gridRowCount(): Int = layoutRows(keyGrid).size.coerceAtLeast(1)

    fun heightRows(overrideTotal: Int = 0): Int {
        val gridRows = gridRowCount()
        val defaultTotal = gridRows + contentRows
        if (overrideTotal > 0) return max(gridRows, overrideTotal)
        return defaultTotal
    }

    fun contentRowsEffective(overrideTotal: Int = 0): Int =
        heightRows(overrideTotal) - gridRowCount()
}

const val MAX_LAYERS = 14

fun newLayerId(): String =
    "layer_" + java.util.UUID.randomUUID().toString().replace("-", "").take(12)

/** Stable selection id for the keyboard and editor. */
data class ActiveLayer(
    val id: String,
) {
    init {
        require(id.isNotBlank()) { "ActiveLayer id must not be blank" }
    }

    companion object {
        const val MAIN = "main"
        const val NUMERIC = "numeric"
        const val EMOJI = "emoji"
        const val CLIPBOARD = "clipboard"

        val Main = ActiveLayer(MAIN)
        val Numeric = ActiveLayer(NUMERIC)
        val Emoji = ActiveLayer(EMOJI)
        val Clipboard = ActiveLayer(CLIPBOARD)
    }
}

fun parseLayerId(layerId: String): ActiveLayer = ActiveLayer(layerId)

fun ActiveLayer.idString(): String = id
