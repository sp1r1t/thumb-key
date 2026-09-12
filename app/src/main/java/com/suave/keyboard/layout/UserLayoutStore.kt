package com.suave.keyboard.layout

import android.content.Context
import android.util.Log
import com.suave.keyboard.layout.json.LayoutJsonException
import com.suave.keyboard.layout.json.decodeNamedLayout
import com.suave.keyboard.layout.json.encodeNamedLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Persists user layouts as JSON under [layoutsDir] and mirrors metadata in [UserLayoutIndex].
 * File round-trips are plain JVM I/O so they can be unit-tested without Android.
 */
class UserLayoutStore(
    private val layoutsDir: File,
    private val indexDao: UserLayoutIndexDao,
) {
    init {
        layoutsDir.mkdirs()
    }

    suspend fun list(): List<UserLayoutIndex> =
        withContext(Dispatchers.IO) {
            indexDao.listAll()
        }

    fun observeIndex() = indexDao.observeAll()

    suspend fun get(id: String): NamedLayout? =
        withContext(Dispatchers.IO) {
            val entry = indexDao.getById(id) ?: return@withContext null
            when (entry.source) {
                LAYOUT_SOURCE_BUILTIN -> LayoutRegistry.byId(id)
                LAYOUT_SOURCE_USER -> readUserFile(id)
                else -> readUserFile(id) ?: LayoutRegistry.byId(id)
            }
        }

    suspend fun save(layout: NamedLayout): NamedLayout =
        withContext(Dispatchers.IO) {
            require(layout.id.isNotBlank()) { "Layout id must not be blank" }
            require(layout.title.isNotBlank()) { "Layout title must not be blank" }
            require(!isBuiltinId(layout.id)) {
                "Cannot overwrite builtin layout id ${layout.id}; duplicate it first"
            }
            writeUserFile(layout)
            indexDao.upsert(
                UserLayoutIndex(
                    id = layout.id,
                    title = layout.title,
                    updatedAt = System.currentTimeMillis(),
                    source = LAYOUT_SOURCE_USER,
                ),
            )
            LayoutRegistry.register(layout)
            layout
        }

    suspend fun delete(id: String) =
        withContext(Dispatchers.IO) {
            val entry = indexDao.getById(id)
            require(entry == null || entry.source == LAYOUT_SOURCE_USER) {
                "Cannot delete builtin layout $id"
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
     * Loads every user JSON into [LayoutRegistry] and refreshes the Room index (builtins + users).
     * Safe to call from app / IME startup.
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
                        Log.w(TAG, "Skipping user file that claims builtin id ${layout.id}")
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

            // Drop previous index rows, then rebuild from registry builtins + user files.
            indexDao.deleteBySource(LAYOUT_SOURCE_BUILTIN)
            indexDao.deleteBySource(LAYOUT_SOURCE_USER)

            val builtinEntries =
                LayoutRegistry
                    .all()
                    .filter { it.id !in userIds }
                    .map { layout ->
                        UserLayoutIndex(
                            id = layout.id,
                            title = layout.title,
                            updatedAt = now,
                            source = LAYOUT_SOURCE_BUILTIN,
                        )
                    }
            indexDao.upsertAll(builtinEntries)
            indexDao.upsertAll(
                userLayouts.map { (layout, updatedAt) ->
                    UserLayoutIndex(
                        id = layout.id,
                        title = layout.title,
                        updatedAt = updatedAt,
                        source = LAYOUT_SOURCE_USER,
                    )
                },
            )
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

    private fun fileFor(id: String): File = File(layoutsDir, "$id.json")

    companion object {
        private const val TAG = "UserLayoutStore"
        private const val LAYOUTS_DIR = "layouts"

        fun layoutsDirectory(context: Context): File =
            File(context.applicationContext.filesDir, LAYOUTS_DIR).also { it.mkdirs() }

        fun create(context: Context): UserLayoutStore {
            val db = com.suave.keyboard.db.AppDB.getDatabase(context)
            return UserLayoutStore(layoutsDirectory(context), db.userLayoutIndexDao())
        }

        fun newUserId(): String = "user_" + UUID.randomUUID().toString().replace("-", "").take(12)

        fun isBuiltinId(id: String): Boolean =
            BuiltinLayouts.ALL.any { it.id == id } || id == LayoutRegistry.DEFAULT_ID
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
