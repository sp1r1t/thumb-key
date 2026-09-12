package com.suave.keyboard.ui.theme

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import com.suave.keyboard.R
import com.suave.keyboard.ui.theme.json.ThemeJsonException
import com.suave.keyboard.ui.theme.json.colorSchemesToThemeDocument
import com.suave.keyboard.ui.theme.json.parseThemeDocument
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Resolves themes by string id. Builtin JSON under assets/themes is preferred;
 * Color.kt palette functions remain a fallback when an asset is missing.
 * User themes are merged via [ThemeStore] when a context is provided.
 */
object ThemeRegistry {
    const val DYNAMIC_ID = NamedTheme.DYNAMIC_ID
    const val DEFAULT_ID = NamedTheme.DEFAULT_ID

    private const val ASSET_DIR = "themes"
    private const val TAG = "ThemeRegistry"

    private val cache = ConcurrentHashMap<String, NamedTheme>()
    private val revisionCounter = AtomicInteger(0)
    private val _revision = MutableStateFlow(0)

    /**
     * Bumps when a user theme is added, replaced, or removed. [SuaveTheme] collects this so
     * editing the active palette recomposes without changing [AppSettings.themeColor].
     */
    val revision: StateFlow<Int> = _revision.asStateFlow()

    private fun bumpRevision() {
        _revision.value = revisionCounter.incrementAndGet()
    }

    @Volatile
    private var assetsLoaded = false

    /** Builtin palette ids in preference-list order (Suave first, then the rest). */
    val BUILTIN_PALETTE_IDS =
        listOf(
            "suave",
            "green",
            "pink",
            "srcery",
            "blue",
            "dracula",
            "twilight",
            "highContrast",
            "highContrastColorful",
            "ancom",
            "matrix",
            "neon",
        )

    fun ensureLoaded(context: Context) {
        if (assetsLoaded) return
        synchronized(this) {
            if (assetsLoaded) return
            loadFromAssets(context.applicationContext.assets)
            assetsLoaded = true
        }
    }

    /** Load every JSON file from assets/themes. Safe to call more than once. */
    fun loadFromAssets(assets: AssetManager) {
        try {
            val names = assets.list(ASSET_DIR).orEmpty()
            for (name in names) {
                if (!name.endsWith(".json")) continue
                try {
                    val json =
                        assets.open("$ASSET_DIR/$name").bufferedReader().use { it.readText() }
                    val theme = NamedTheme.fromDocument(parseThemeDocument(json), builtin = true)
                    cache[theme.id] = theme
                } catch (e: ThemeJsonException) {
                    Log.e(TAG, "Failed to decode theme asset $name: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read theme asset $name: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list theme assets: ${e.message}")
        }
        seedKotlinFallbacks()
    }

    private fun seedKotlinFallbacks() {
        for ((id, title, factory) in kotlinBuiltinSeeds()) {
            cache.putIfAbsent(
                id,
                NamedTheme(
                    id = id,
                    title = title,
                    light = factory().first,
                    dark = factory().second,
                    builtin = true,
                ),
            )
        }
    }

    private fun kotlinBuiltinSeeds(): List<Triple<String, String, () -> Pair<ColorScheme, ColorScheme>>> =
        listOf(
            Triple("suave", "Suave", ::suave),
            Triple("green", "Green", ::green),
            Triple("pink", "Rose", ::pink),
            Triple("srcery", "Srcery", ::srcery),
            Triple("blue", "Teal", ::blue),
            Triple("dracula", "Dracula", ::dracula),
            Triple("twilight", "Twilight", ::twilight),
            Triple("highContrast", "High contrast", ::highContrast),
            Triple("highContrastColorful", "High contrast color", ::highContrastColorful),
            Triple("ancom", "Black and red", ::ancom),
            Triple("matrix", "Neon Green", ::matrix),
            Triple("neon", "Neon Blue", ::neon),
        )

    fun putUserTheme(theme: NamedTheme) {
        val next = theme.copy(builtin = false)
        val previous = cache[theme.id]
        val contentChanged =
            previous == null || previous.toDocument() != next.toDocument()
        cache[theme.id] = next
        if (contentChanged) {
            bumpRevision()
        }
    }

    fun removeUserTheme(id: String) {
        val existing = cache[id] ?: return
        if (!existing.builtin) {
            cache.remove(id)
            bumpRevision()
        }
    }

    fun invalidateUserCache() {
        val removed = cache.entries.removeIf { !it.value.builtin }
        if (removed) {
            bumpRevision()
        }
    }

    /** Resolve without Android context (tests / already-loaded cache). */
    fun byId(id: String): NamedTheme {
        if (id == DYNAMIC_ID) {
            return byId(DEFAULT_ID)
        }
        cache[id]?.let { return it }
        seedKotlinFallbacks()
        cache[id]?.let { return it }
        return cache[DEFAULT_ID]
            ?: NamedTheme
                .fromDocument(
                    colorSchemesToThemeDocument(DEFAULT_ID, "Suave", suave()),
                    builtin = true,
                ).also { cache[it.id] = it }
    }

    fun byId(
        context: Context,
        id: String,
    ): NamedTheme {
        ensureLoaded(context)
        ThemeStore.get(context).loadIntoRegistry()
        return byId(id)
    }

    fun isDynamic(id: String): Boolean = id == DYNAMIC_ID

    fun title(
        context: Context,
        id: String,
    ): String {
        if (id == DYNAMIC_ID) {
            return context.getString(R.string.dynamic)
        }
        titleRes(id)?.let { return context.getString(it) }
        ensureLoaded(context)
        ThemeStore.get(context).loadIntoRegistry()
        return byId(id).title
    }

    @StringRes
    fun titleRes(id: String): Int? =
        when (id) {
            DYNAMIC_ID -> R.string.dynamic
            "suave" -> R.string.theme_color_suave
            "green" -> R.string.green
            "pink" -> R.string.pink
            "srcery" -> R.string.srcery
            "blue" -> R.string.blue
            "dracula" -> R.string.dracula
            "twilight" -> R.string.twilight
            "highContrast" -> R.string.high_contrast
            "highContrastColorful" -> R.string.high_contrast_colorful
            "ancom" -> R.string.ancom
            "matrix" -> R.string.matrix
            "neon" -> R.string.neon
            else -> null
        }

    /**
     * Ids shown in the theme picker: Suave first, then Dynamic, then other builtins, then user themes.
     */
    fun selectableIds(context: Context): List<String> {
        ensureLoaded(context)
        val store = ThemeStore.get(context)
        store.loadIntoRegistry()
        val userIds = store.listIds().filter { it !in BUILTIN_PALETTE_IDS && it != DYNAMIC_ID }
        val otherBuiltins = BUILTIN_PALETTE_IDS.filter { it != DEFAULT_ID }
        return listOf(DEFAULT_ID, DYNAMIC_ID) + otherBuiltins + userIds.sorted()
    }

    fun colorSchemes(
        context: Context,
        id: String,
    ): Pair<ColorScheme, ColorScheme> {
        ensureLoaded(context)
        ThemeStore.get(context).loadIntoRegistry()
        return byId(id).schemes
    }
}
