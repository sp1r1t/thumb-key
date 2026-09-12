package com.suave.keyboard.layout

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.suave.keyboard.layout.json.LayoutJsonException
import com.suave.keyboard.layout.json.decodeNamedLayout
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves layouts by string id (not ordinal). Builtin JSON under assets/layouts is preferred;
 * Kotlin [BuiltinLayouts] remains a fallback when an asset is missing.
 */
object LayoutRegistry {
    const val DEFAULT_ID = "s12"

    private const val ASSET_DIR = "layouts"
    private const val TAG = "LayoutRegistry"

    private val cache = ConcurrentHashMap<String, NamedLayout>()

    @Volatile
    private var assetsLoaded = false

    fun ensureLoaded(context: Context) {
        if (assetsLoaded) return
        synchronized(this) {
            if (assetsLoaded) return
            loadFromAssets(context.applicationContext.assets)
            assetsLoaded = true
        }
    }

    /** Load every JSON file from assets/layouts. Safe to call more than once. */
    fun loadFromAssets(assets: AssetManager) {
        try {
            val names = assets.list(ASSET_DIR).orEmpty()
            for (name in names) {
                if (!name.endsWith(".json")) continue
                try {
                    val json =
                        assets.open("$ASSET_DIR/$name").bufferedReader().use { it.readText() }
                    val layout = decodeNamedLayout(json)
                    cache[layout.id] = layout
                } catch (e: LayoutJsonException) {
                    Log.e(TAG, "Failed to decode layout asset $name: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read layout asset $name: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list layout assets: ${e.message}")
        }
        seedKotlinFallbacks()
    }

    private fun seedKotlinFallbacks() {
        for (layout in BuiltinLayouts.ALL) {
            cache.putIfAbsent(layout.id, layout)
        }
    }

    /** Resolve without Android context (tests / already-loaded cache). Falls back to Kotlin builtins. */
    fun byId(id: String): NamedLayout {
        cache[id]?.let { return it }
        BuiltinLayouts.ALL.find { it.id == id }?.let { return it }
        return cache[DEFAULT_ID] ?: BuiltinLayouts.S12
    }

    fun byId(
        context: Context,
        id: String,
    ): NamedLayout {
        ensureLoaded(context)
        return byId(id)
    }

    /**
     * Comma-separated layout ids from the DB. Empty or unknown-only lists fall back to default.
     */
    fun enabledFromDb(ids: String?): List<NamedLayout> {
        seedKotlinFallbacks()
        val parsed =
            ids
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
        val layouts = parsed.map { byId(it) }.distinctBy { it.id }
        return layouts.ifEmpty { listOf(byId(DEFAULT_ID)) }
    }

    fun enabledFromDb(
        context: Context,
        ids: String?,
    ): List<NamedLayout> {
        ensureLoaded(context)
        return enabledFromDb(ids)
    }

    fun canSwitch(ids: String?): Boolean = enabledFromDb(ids).size > 1

    fun canSwitch(
        context: Context,
        ids: String?,
    ): Boolean {
        ensureLoaded(context)
        return canSwitch(ids)
    }

    /** Register or replace a layout in the in-memory cache (user layouts, tests). */
    fun register(layout: NamedLayout) {
        require(layout.id.isNotBlank()) { "Layout id must not be blank" }
        cache[layout.id] = layout
    }

    /** Remove a cached layout. Builtin Kotlin fallbacks remain available via [byId]. */
    fun unregister(id: String) {
        cache.remove(id)
    }

    /** All layouts currently in the cache (assets + registered users), seeded with Kotlin fallbacks. */
    fun all(): List<NamedLayout> {
        seedKotlinFallbacks()
        return cache.values.sortedBy { it.title.lowercase() }
    }

    fun all(context: Context): List<NamedLayout> {
        ensureLoaded(context)
        return all()
    }
}
