package com.suave.keyboard.layout

import android.content.Context
import android.util.Log
import com.suave.keyboard.layout.json.LayoutJsonException
import com.suave.keyboard.layout.json.decodeNamedLayout
import com.suave.keyboard.layout.json.encodeNamedLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Persists user layouts as JSON under [layoutsDir] and mirrors metadata in [UserLayoutIndex].
 * File round-trips are plain JVM I/O so they can be unit-tested without Android.
 *
 * Template layouts cannot be overwritten; their per-app landscape-float overrides live in a
 * sidecar under [floatAppsDir] and are merged into [LayoutRegistry] on load / toggle.
 */
class UserLayoutStore(
    private val layoutsDir: File,
    private val indexDao: UserLayoutIndexDao,
    private val floatAppsDir: File = File(layoutsDir.parentFile, FLOAT_APPS_DIR),
) {
    init {
        layoutsDir.mkdirs()
        floatAppsDir.mkdirs()
    }

    private val floatAppsJson =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    suspend fun list(): List<UserLayoutIndex> =
        withContext(Dispatchers.IO) {
            indexDao.listBySource(LAYOUT_SOURCE_USER)
        }

    fun observeIndex() = indexDao.observeBySource(LAYOUT_SOURCE_USER)

    suspend fun get(id: String): NamedLayout? =
        withContext(Dispatchers.IO) {
            readUserFile(id)?.let { return@withContext it }
            val entry = indexDao.getById(id)
            if (entry?.source == LAYOUT_SOURCE_BUILTIN || LayoutRegistry.isTemplateId(id)) {
                return@withContext LayoutRegistry.byId(id).takeIf { it.id == id }
            }
            LayoutRegistry.byId(id).takeIf { it.id == id }
        }

    suspend fun save(layout: NamedLayout): NamedLayout =
        withContext(Dispatchers.IO) {
            require(layout.id.isNotBlank()) { "Layout id must not be blank" }
            require(layout.title.isNotBlank()) { "Layout title must not be blank" }
            require(!isBuiltinId(layout.id)) {
                "Cannot overwrite template layout id ${layout.id}; copy it first"
            }
            writeUserFile(layout)
            // User JSON owns float-by-app; drop any leftover builtin sidecar.
            floatAppsFile(layout.id).delete()
            indexDao.upsert(indexEntry(layout))
            LayoutRegistry.register(layout)
            layout
        }

    /**
     * Toggle landscape floating for [packageName] on [layoutId]. User layout JSON is rewritten
     * when a user file exists; otherwise a sidecar map is used (asset / kotlin builtins).
     * Updates [LayoutRegistry] immediately.
     */
    suspend fun toggleLandscapeFloatingForApp(
        layoutId: String,
        packageName: String,
    ): NamedLayout =
        withContext(Dispatchers.IO) {
            require(packageName.isNotBlank()) { "packageName must not be blank" }
            val userFile = readUserFile(layoutId)
            val base =
                if (userFile != null) {
                    userFile
                } else {
                    val cached = LayoutRegistry.byId(layoutId)
                    val sidecar = readFloatAppsSidecar(layoutId)
                    if (sidecar.isEmpty()) cached else cached.copy(landscapeFloatingByApp = sidecar)
                }
            val updated = base.withToggledLandscapeFloatingForApp(packageName)
            if (fileFor(layoutId).isFile) {
                writeUserFile(updated)
                floatAppsFile(layoutId).delete()
                indexDao.upsert(indexEntry(updated))
                LayoutRegistry.register(updated)
            } else {
                writeFloatAppsSidecar(layoutId, updated.landscapeFloatingByApp)
                LayoutRegistry.register(updated)
            }
            updated
        }

    suspend fun delete(id: String) =
        withContext(Dispatchers.IO) {
            val entry = indexDao.getById(id)
            require(entry == null || entry.source == LAYOUT_SOURCE_USER) {
                "Cannot delete template layout $id"
            }
            require(!isBuiltinId(id)) {
                "Cannot delete template layout $id"
            }
            fileFor(id).delete()
            indexDao.deleteById(id)
            LayoutRegistry.unregister(id)
        }

    /**
     * Copies [sourceId] (builtin or user) into a new user layout with a fresh id and optional
     * [newTitle].
     */
    suspend fun duplicateFrom(
        sourceId: String,
        newTitle: String? = null,
    ): NamedLayout =
        withContext(Dispatchers.IO) {
            val source =
                get(sourceId)
                    ?: LayoutRegistry.byId(sourceId).takeIf { it.id == sourceId }
                    ?: error("Unknown layout: $sourceId")
            val copy =
                source.copy(
                    id = newUserId(),
                    title = newTitle?.takeIf { it.isNotBlank() } ?: "${source.title} copy",
                )
            save(copy)
        }

    suspend fun importFromJson(json: String): NamedLayout =
        withContext(Dispatchers.IO) {
            val decoded =
                try {
                    decodeNamedLayout(json)
                } catch (e: LayoutJsonException) {
                    throw e
                }
            val id =
                when {
                    decoded.id.isBlank() || isBuiltinId(decoded.id) || indexDao.getById(decoded.id) != null ->
                        newUserId()
                    else -> decoded.id
                }
            val title = decoded.title.ifBlank { "Imported layout" }
            save(decoded.copy(id = id, title = title))
        }

    suspend fun exportToJson(id: String): String =
        withContext(Dispatchers.IO) {
            val layout =
                get(id)
                    ?: LayoutRegistry.byId(id).takeIf { it.id == id }
                    ?: error("Unknown layout: $id")
            encodeNamedLayout(layout)
        }

    /**
     * Loads every user JSON into [LayoutRegistry] and rebuilds the Room index from user files
     * only. Templates stay in the registry for Add layout, not in the available list.
     */
    suspend fun loadIntoRegistry() =
        withContext(Dispatchers.IO) {
            layoutsDir.mkdirs()
            val now = System.currentTimeMillis()

            val files = layoutsDir.listFiles().orEmpty().filter { it.extension.equals("json", true) }
            val userLayouts = mutableListOf<Pair<NamedLayout, Long>>()
            for (file in files) {
                try {
                    val layout = decodeNamedLayout(file.readText())
                    if (isBuiltinId(layout.id)) {
                        Log.w(TAG, "Skipping user file that claims template id ${layout.id}")
                        continue
                    }
                    LayoutRegistry.register(layout)
                    userLayouts.add(
                        layout to (file.lastModified().takeIf { it > 0 } ?: now),
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load user layout ${file.name}: ${e.message}")
                }
            }
            val userIds = userLayouts.map { it.first.id }.toSet()

            // Merge per-app float sidecars onto layouts that are not backed by a user JSON file.
            for (layout in LayoutRegistry.all()) {
                if (layout.id in userIds) continue
                val sidecar = readFloatAppsSidecar(layout.id)
                if (sidecar.isNotEmpty()) {
                    LayoutRegistry.register(layout.copy(landscapeFloatingByApp = sidecar))
                }
            }

            indexDao.deleteBySource(LAYOUT_SOURCE_BUILTIN)
            indexDao.deleteBySource(LAYOUT_SOURCE_USER)
            indexDao.upsertAll(
                userLayouts.map { (layout, updatedAt) ->
                    indexEntry(layout, updatedAt)
                },
            )
        }

    /**
     * Copies each enabled template id into a user layout with the same title, then returns the
     * rewritten active / enabled ids. No-op when nothing enabled is still a template.
     */
    suspend fun promoteEnabledTemplates(
        keyboardLayout: String?,
        keyboardLayouts: String?,
    ): LayoutIdSelection? =
        withContext(Dispatchers.IO) {
            val enabled =
                parseLayoutIds(keyboardLayouts)
                    .ifEmpty {
                        listOfNotNull(keyboardLayout?.takeIf { it.isNotBlank() })
                    }
            val toPromote =
                (enabled + listOfNotNull(keyboardLayout))
                    .distinct()
                    .filter { isBuiltinId(it) }
            if (toPromote.isEmpty()) return@withContext null
            val replacements = linkedMapOf<String, String>()
            for (id in toPromote) {
                val source = get(id) ?: LayoutRegistry.byId(id).takeIf { it.id == id } ?: continue
                val copy =
                    source.copy(
                        id = newUserId(),
                        title = source.title,
                    )
                save(copy)
                replacements[id] = copy.id
            }
            if (replacements.isEmpty()) return@withContext null
            rewriteLayoutIds(keyboardLayout, keyboardLayouts, replacements)
        }

    private fun readUserFile(id: String): NamedLayout? {
        val file = fileFor(id)
        if (!file.isFile) return null
        return try {
            decodeNamedLayout(file.readText())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read ${file.name}: ${e.message}")
            null
        }
    }

    private fun writeUserFile(layout: NamedLayout) {
        layoutsDir.mkdirs()
        fileFor(layout.id).writeText(encodeNamedLayout(layout))
    }

    private fun indexEntry(
        layout: NamedLayout,
        updatedAt: Long = System.currentTimeMillis(),
    ): UserLayoutIndex =
        UserLayoutIndex(
            id = layout.id,
            title = layout.title,
            updatedAt = updatedAt,
            source = LAYOUT_SOURCE_USER,
            tags = tagsToIndex(layout.tags),
        )

    private fun fileFor(id: String): File = File(layoutsDir, "$id.json")

    private fun floatAppsFile(id: String): File = File(floatAppsDir, "$id.json")

    private fun readFloatAppsSidecar(id: String): Map<String, Boolean> {
        val file = floatAppsFile(id)
        if (!file.isFile) return emptyMap()
        return try {
            floatAppsJson.decodeFromString(
                MapSerializer(String.serializer(), Boolean.serializer()),
                file.readText(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read float-apps sidecar ${file.name}: ${e.message}")
            emptyMap()
        }
    }

    private fun writeFloatAppsSidecar(
        id: String,
        map: Map<String, Boolean>,
    ) {
        floatAppsDir.mkdirs()
        val file = floatAppsFile(id)
        if (map.isEmpty()) {
            file.delete()
            return
        }
        file.writeText(
            floatAppsJson.encodeToString(
                MapSerializer(String.serializer(), Boolean.serializer()),
                map,
            ),
        )
    }

    companion object {
        private const val TAG = "UserLayoutStore"
        private const val LAYOUTS_DIR = "layouts"
        private const val FLOAT_APPS_DIR = "layout_float_apps"

        fun layoutsDirectory(context: Context): File =
            File(context.applicationContext.filesDir, LAYOUTS_DIR).also { it.mkdirs() }

        fun create(context: Context): UserLayoutStore {
            val db = com.suave.keyboard.db.AppDB.getDatabase(context)
            return UserLayoutStore(layoutsDirectory(context), db.userLayoutIndexDao())
        }

        fun newUserId(): String = "user_" + UUID.randomUUID().toString().replace("-", "").take(12)

        fun isBuiltinId(id: String): Boolean = LayoutRegistry.isTemplateId(id)
    }
}

/** JVM-friendly file helpers used by unit tests (no Room / Context). */
object UserLayoutFiles {
    fun save(
        layoutsDir: File,
        layout: NamedLayout,
    ): File {
        layoutsDir.mkdirs()
        val file = File(layoutsDir, "${layout.id}.json")
        file.writeText(encodeNamedLayout(layout))
        return file
    }

    fun load(
        layoutsDir: File,
        id: String,
    ): NamedLayout? {
        val file = File(layoutsDir, "$id.json")
        if (!file.isFile) return null
        return decodeNamedLayout(file.readText())
    }

    fun delete(
        layoutsDir: File,
        id: String,
    ): Boolean = File(layoutsDir, "$id.json").delete()

    fun exportToJson(layout: NamedLayout): String = encodeNamedLayout(layout)

    fun importFromJson(json: String): NamedLayout = decodeNamedLayout(json)
}
