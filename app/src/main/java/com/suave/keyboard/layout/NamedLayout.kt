package com.suave.keyboard.layout

import com.suave.keyboard.engine.intent.Layout
import com.suave.keyboard.engine.intent.layoutRows
import kotlin.math.max

/**
 * A first-class layout the engine can persist, select, and render. Layers are a uniform list:
 * home, numeric, emoji, clipboard, and user layers all share [LayerDefinition].
 */
data class NamedLayout(
    val id: String,
    val title: String,
    val homeLayerId: String = ActiveLayer.MAIN,
    val layers: List<LayerDefinition>,
    val shiftMappings: Map<String, String> = emptyMap(),
    val capsLockMappings: Map<String, String> = emptyMap(),
    /** Optional spacebar multitap replacements after the first plain space; null uses engine default. */
    val spaceMultitapCycle: List<String>? = null,
    /** Optional portrait/default key height in dp; null uses Appearance. */
    val keyHeight: Int? = null,
    /** Optional landscape key height in dp; null uses Appearance landscape height. */
    val landscapeKeyHeight: Int? = null,
    /**
     * When true in landscape, the IME floats over the app (apps keep drawing underneath;
     * Split/Dual gap stays pass-through for touches outside the key halves).
     */
    val landscapeFloating: Boolean = false,
    /**
     * Per-app overrides for [landscapeFloating], keyed by host package name. Missing key means
     * use the layout default. Cleared entries are omitted from JSON.
     */
    val landscapeFloatingByApp: Map<String, Boolean> = emptyMap(),
    /** Search tags (language, style, origin). Lowercase, unique. */
    val tags: List<String> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "Layout id must not be blank" }
        require(title.isNotBlank()) { "Layout title must not be blank" }
        require(layers.isNotEmpty()) { "Layout must define at least one layer" }
        require(layers.size <= MAX_LAYERS) { "At most $MAX_LAYERS layers" }
        val ids = layers.map { it.id }
        require(ids.distinct().size == ids.size) { "Layer ids must be unique" }
        require(layers.any { it.id == homeLayerId }) {
            "homeLayerId '$homeLayerId' must match a layer id"
        }
        keyHeight?.let { require(it in 10..200) { "keyHeight must be 10..200, got $it" } }
        landscapeKeyHeight?.let {
            require(it in 10..200) { "landscapeKeyHeight must be 10..200, got $it" }
        }
    }

    /** Layout default, or the stored override for [packageName] when present. */
    fun effectiveLandscapeFloating(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return landscapeFloating
        return landscapeFloatingByApp[packageName] ?: landscapeFloating
    }

    /**
     * Toggle float for [packageName]. Returns a copy with the override map updated; when the
     * next value matches [landscapeFloating], the package entry is removed.
     */
    fun withToggledLandscapeFloatingForApp(packageName: String): NamedLayout {
        require(packageName.isNotBlank()) { "packageName must not be blank" }
        val next = !effectiveLandscapeFloating(packageName)
        val map = landscapeFloatingByApp.toMutableMap()
        if (next == landscapeFloating) {
            map.remove(packageName)
        } else {
            map[packageName] = next
        }
        return copy(landscapeFloatingByApp = map.toMap())
    }

    fun layer(id: String): LayerDefinition? = layers.find { it.id == id }

    fun layer(active: ActiveLayer): LayerDefinition? = layer(active.id)

    fun requireLayer(id: String): LayerDefinition =
        layer(id) ?: error("Unknown layer id: $id")

    fun homeLayer(): LayerDefinition = requireLayer(homeLayerId)

    fun homeActive(): ActiveLayer = ActiveLayer(homeLayerId)

    fun availableLayers(): List<ActiveLayer> = layers.map { ActiveLayer(it.id) }

    fun isOverlay(active: ActiveLayer): Boolean = layer(active)?.overlay == true

    fun gridFor(active: ActiveLayer): Layout =
        layer(active)?.keyGrid ?: homeLayer().keyGrid

    fun gridFor(layerId: String): Layout = gridFor(ActiveLayer(layerId))

    fun gridRowCount(active: ActiveLayer): Int =
        layer(active)?.gridRowCount() ?: homeLayer().gridRowCount()

    fun contentFor(active: ActiveLayer): LayerContent =
        layer(active)?.content ?: LayerContent.None

    fun contentRows(
        active: ActiveLayer,
        overrideTotal: Int = 0,
    ): Int = layer(active)?.contentRowsEffective(overrideTotal) ?: 0

    fun heightRows(
        active: ActiveLayer,
        overrideTotal: Int = 0,
    ): Int = layer(active)?.heightRows(overrideTotal) ?: homeLayer().heightRows(overrideTotal)

    fun withLayer(updated: LayerDefinition): NamedLayout {
        val idx = layers.indexOfFirst { it.id == updated.id }
        val next =
            if (idx >= 0) {
                layers.toMutableList().also { it[idx] = updated }
            } else {
                (layers + updated).also { require(it.size <= MAX_LAYERS) }
            }
        return copy(layers = next)
    }

    fun withoutLayer(layerId: String): NamedLayout {
        require(layerId != homeLayerId) { "Cannot remove home layer" }
        return copy(layers = layers.filterNot { it.id == layerId })
    }

    fun replaceKeyGrid(
        layerId: String,
        keyGrid: Layout,
    ): NamedLayout {
        val current = requireLayer(layerId)
        return withLayer(current.copy(keyGrid = keyGrid))
    }
}

object BuiltinLayouts {
    /**
     * Fallback when assets are missing. Prefer [com.suave.keyboard.layout.LayoutRegistry]
     * loading `assets/layouts/s12.json`.
     */
    val S12: NamedLayout by lazy {
        blankNamedLayout(
            id = "s12",
            title = "Suave Layout",
            rowSizes = listOf(5, 5, 5, 4),
        ).copy(
            shiftMappings = S12_SHIFT_MAPPINGS,
            capsLockMappings = S12_CAPS_LOCK_MAPPINGS,
            tags = listOf("en", "split", "suave", "thumbkey"),
        )
    }

    val ALL: List<NamedLayout> = listOf(S12)

    fun byIndex(index: Int): NamedLayout = ALL.getOrElse(index) { S12 }

    fun enabledFromDb(indices: String?): List<NamedLayout> {
        val parsed =
            indices
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.map { byIndex(it) }
                ?.distinct()
                .orEmpty()
        return parsed.ifEmpty { listOf(S12) }
    }

    /** True when cycling layouts would land on a different one. */
    fun canSwitch(indices: String?): Boolean = enabledFromDb(indices).size > 1
}
