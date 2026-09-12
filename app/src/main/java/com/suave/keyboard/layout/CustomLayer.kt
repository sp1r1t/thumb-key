package com.suave.keyboard.layout

import com.suave.keyboard.engine.intent.Layout

/**
 * Built-in layers stay on [LayoutLayer]. User-defined function layers are [Custom] with a stable
 * string id stored in the layout JSON. Cap is editorial ([MAX_CUSTOM_LAYERS]), not type-system.
 */
sealed class ActiveLayer {
    data class Builtin(
        val layer: LayoutLayer,
    ) : ActiveLayer()

    data class Custom(
        val id: String,
    ) : ActiveLayer()

    val isOverlay: Boolean
        get() =
            this is Builtin &&
                (layer == LayoutLayer.EMOJI || layer == LayoutLayer.CLIPBOARD)

    companion object {
        val Main: ActiveLayer = Builtin(LayoutLayer.MAIN)
        val Numeric: ActiveLayer = Builtin(LayoutLayer.NUMERIC)
        val Emoji: ActiveLayer = Builtin(LayoutLayer.EMOJI)
        val Clipboard: ActiveLayer = Builtin(LayoutLayer.CLIPBOARD)
    }
}

/** Icons users can pick for a custom function layer (Material outlined set). */
enum class CustomLayerIcon {
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
 * A user-defined full-grid layer (NUMERIC peer). No content panel - height matches the key grid.
 */
data class CustomLayer(
    val id: String,
    val title: String,
    val icon: CustomLayerIcon = CustomLayerIcon.Functions,
    val layout: Layout,
)

const val MAX_CUSTOM_LAYERS = 10

fun newCustomLayerId(): String = "custom_" + java.util.UUID.randomUUID().toString().replace("-", "").take(12)

/** Stable string used in JSON [KeyIntent.SwitchLayer] and editor memory keys. */
fun ActiveLayer.idString(): String =
    when (this) {
        is ActiveLayer.Builtin -> layer.name
        is ActiveLayer.Custom -> id
    }

/**
 * Parse a layer id from layout JSON or [KeyIntent.SwitchLayer]. Builtin names match
 * [LayoutLayer.name]; anything else is treated as a custom function-layer id.
 */
fun parseLayerId(layerId: String): ActiveLayer {
    val builtin =
        try {
            LayoutLayer.valueOf(layerId)
        } catch (_: IllegalArgumentException) {
            null
        }
    return if (builtin != null) ActiveLayer.Builtin(builtin) else ActiveLayer.Custom(layerId)
}
