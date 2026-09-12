package com.suave.keyboard.ui.theme

import android.content.Context
import android.util.Log
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.suave.keyboard.db.AppDB
import com.suave.keyboard.R
import com.suave.keyboard.ui.theme.json.THEME_SCHEMA_VERSION
import com.suave.keyboard.ui.theme.json.ThemeDocument
import com.suave.keyboard.ui.theme.json.ThemeJsonException
import com.suave.keyboard.ui.theme.json.encodeThemeDocument
import com.suave.keyboard.ui.theme.json.parseThemeDocument
import java.io.File
import java.util.UUID

@Entity(tableName = "UserTheme")
data class UserThemeIndex(
    @PrimaryKey val id: String,
    val title: String,
    val updatedAt: Long,
)

@Dao
interface UserThemeDao {
    @Query("SELECT * FROM UserTheme ORDER BY title COLLATE NOCASE ASC")
    fun list(): List<UserThemeIndex>

    @Query("SELECT id FROM UserTheme ORDER BY title COLLATE NOCASE ASC")
    fun listIds(): List<String>

    @Query("SELECT * FROM UserTheme WHERE id = :id")
    fun get(id: String): UserThemeIndex?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(theme: UserThemeIndex)

    @Query("DELETE FROM UserTheme WHERE id = :id")
    fun delete(id: String)
}

/**
 * User themes live as JSON under files/themes/<id>.json with a Room index in [AppDB].
 */
class ThemeStore(
    private val context: Context,
    private val dao: UserThemeDao,
) {
    private val dir: File =
        File(context.filesDir, DIR_NAME).also { it.mkdirs() }

    fun listIds(): List<String> = dao.listIds()

    fun listIndex(): List<UserThemeIndex> = dao.list()

    fun loadIntoRegistry() {
        for (index in dao.list()) {
            try {
                load(index.id)?.let { ThemeRegistry.putUserTheme(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load user theme ${index.id}: ${e.message}")
            }
        }
    }

    fun load(id: String): NamedTheme? {
        val file = fileFor(id)
        if (!file.exists()) return null
        val document = parseThemeDocument(file.readText())
        return NamedTheme.fromDocument(document, builtin = false)
    }

    fun save(document: ThemeDocument): NamedTheme {
        if (document.id == NamedTheme.DYNAMIC_ID) {
            throw ThemeJsonException("Cannot save reserved id \"dynamic\"")
        }
        if (document.id in ThemeRegistry.BUILTIN_PALETTE_IDS) {
            throw ThemeJsonException("Cannot overwrite builtin theme id \"${document.id}\"")
        }
        val validated = parseThemeDocument(encodeThemeDocument(document))
        fileFor(validated.id).writeText(encodeThemeDocument(validated))
        dao.upsert(
            UserThemeIndex(
                id = validated.id,
                title = validated.title,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        val theme = NamedTheme.fromDocument(validated, builtin = false)
        ThemeRegistry.putUserTheme(theme)
        return theme
    }

    fun delete(id: String) {
        fileFor(id).delete()
        dao.delete(id)
        ThemeRegistry.removeUserTheme(id)
    }

    fun exportJson(id: String): String {
        val theme =
            load(id)
                ?: ThemeRegistry.byId(context, id).takeIf { it.id == id && !ThemeRegistry.isDynamic(id) }
                ?: throw ThemeJsonException("Theme not found: $id")
        return encodeThemeDocument(theme.toDocument())
    }

    fun importJson(json: String): NamedTheme {
        val parsed = parseThemeDocument(json)
        val id =
            if (parsed.id in ThemeRegistry.BUILTIN_PALETTE_IDS ||
                parsed.id == NamedTheme.DYNAMIC_ID ||
                dao.get(parsed.id) != null
            ) {
                uniqueUserId(parsed.id)
            } else {
                parsed.id
            }
        val title =
            if (id != parsed.id && !parsed.title.contains("(imported)", ignoreCase = true)) {
                "${parsed.title} (imported)"
            } else {
                parsed.title
            }
        return save(parsed.copy(id = id, title = title, schemaVersion = THEME_SCHEMA_VERSION))
    }

    fun duplicateFrom(sourceId: String): NamedTheme {
        ThemeRegistry.ensureLoaded(context)
        val source =
            if (ThemeRegistry.isDynamic(sourceId)) {
                ThemeRegistry.byId(NamedTheme.DEFAULT_ID)
            } else {
                load(sourceId) ?: ThemeRegistry.byId(context, sourceId)
            }
        val newId = uniqueUserId(source.id)
        val title = context.getString(R.string.theme_copy_title, source.title)
        return save(
            source.toDocument().copy(
                id = newId,
                title = title,
                schemaVersion = THEME_SCHEMA_VERSION,
            ),
        )
    }

    private fun uniqueUserId(base: String): String {
        val sanitized =
            base
                .lowercase()
                .replace(Regex("[^a-z0-9_-]+"), "-")
                .trim('-')
                .ifBlank { "theme" }
        var candidate = "$sanitized-copy"
        var n = 2
        while (dao.get(candidate) != null ||
            candidate in ThemeRegistry.BUILTIN_PALETTE_IDS ||
            candidate == NamedTheme.DYNAMIC_ID
        ) {
            candidate = "$sanitized-copy-$n"
            n++
        }
        // Extremely defensive uniqueness if copies collide hard.
        if (dao.get(candidate) != null) {
            candidate = "theme-${UUID.randomUUID().toString().take(8)}"
        }
        return candidate
    }

    private fun fileFor(id: String): File = File(dir, "$id.json")

    companion object {
        private const val DIR_NAME = "themes"
        private const val TAG = "ThemeStore"

        @Volatile
        private var instance: ThemeStore? = null

        fun get(context: Context): ThemeStore {
            val app = context.applicationContext
            return instance
                ?: synchronized(this) {
                    instance
                        ?: ThemeStore(
                            app,
                            AppDB.getDatabase(app).userThemeDao(),
                        ).also { instance = it }
                }
        }
    }
}
