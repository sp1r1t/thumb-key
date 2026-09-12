package com.suave.s12.db

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

const val CLIPBOARD_MIME_TEXT = "text/plain"

@Entity(tableName = "ClipboardItem")
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "text")
    val text: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_pinned", defaultValue = "0")
    val isPinned: Boolean = false,
    @ColumnInfo(name = "mime_type", defaultValue = CLIPBOARD_MIME_TEXT)
    val mimeType: String = CLIPBOARD_MIME_TEXT,
    @ColumnInfo(name = "local_path")
    val localPath: String? = null,
    @ColumnInfo(name = "source_key")
    val sourceKey: String? = null,
) {
    fun isImage(): Boolean = mimeType.startsWith("image/", ignoreCase = true)
}

@Dao
interface ClipboardItemDao {
    @Query("SELECT * FROM ClipboardItem ORDER BY is_pinned DESC, timestamp DESC")
    fun getAllClipboardItems(): LiveData<List<ClipboardItem>>

    @Query("SELECT * FROM ClipboardItem WHERE id = :id")
    suspend fun getById(id: Int): ClipboardItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardItem): Long

    @Update
    suspend fun update(item: ClipboardItem)

    @Delete
    suspend fun delete(item: ClipboardItem)

    @Query("DELETE FROM ClipboardItem WHERE is_pinned = 0")
    suspend fun clearUnpinnedItems()

    @Query("DELETE FROM ClipboardItem")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM ClipboardItem WHERE is_pinned = 0")
    suspend fun getUnpinnedCount(): Int

    @Query(
        "DELETE FROM ClipboardItem WHERE id IN " +
            "(SELECT id FROM ClipboardItem WHERE is_pinned = 0 ORDER BY timestamp ASC LIMIT :count)",
    )
    suspend fun deleteOldestUnpinned(count: Int)

    @Query("DELETE FROM ClipboardItem WHERE is_pinned = 0 AND timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)

    @Query("SELECT * FROM ClipboardItem WHERE text = :text AND mime_type = :mimeType LIMIT 1")
    suspend fun findByText(
        text: String,
        mimeType: String = CLIPBOARD_MIME_TEXT,
    ): ClipboardItem?

    @Query("SELECT * FROM ClipboardItem WHERE source_key = :sourceKey LIMIT 1")
    suspend fun findBySourceKey(sourceKey: String): ClipboardItem?

    @Query("SELECT local_path FROM ClipboardItem WHERE local_path IS NOT NULL")
    suspend fun getAllLocalPaths(): List<String>
}

val CLIPBOARD_MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE ClipboardItem ADD COLUMN mime_type TEXT NOT NULL DEFAULT '$CLIPBOARD_MIME_TEXT'",
            )
            db.execSQL("ALTER TABLE ClipboardItem ADD COLUMN local_path TEXT")
            db.execSQL("ALTER TABLE ClipboardItem ADD COLUMN source_key TEXT")
        }
    }

@Database(
    version = 2,
    entities = [ClipboardItem::class],
    exportSchema = true,
)
abstract class ClipboardDB : RoomDatabase() {
    abstract fun clipboardItemDao(): ClipboardItemDao

    companion object {
        @Volatile
        private var instance: ClipboardDB? = null

        fun getDatabase(context: Context): ClipboardDB {
            check(isCredentialStorageUnlocked(context)) {
                "Clipboard history lives in credential-encrypted storage and is unavailable before first unlock"
            }
            return instance ?: synchronized(this) {
                val instance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            ClipboardDB::class.java,
                            "clipboard_db",
                        ).addMigrations(CLIPBOARD_MIGRATION_1_2)
                        .fallbackToDestructiveMigration(dropAllTables = true)
                        .build()
                Companion.instance = instance
                instance
            }
        }
    }
}
