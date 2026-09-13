package com.suave.keyboard.layout

import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** Builtin assets vs user-authored JSON under files/layouts. */
const val LAYOUT_SOURCE_BUILTIN = "builtin"
const val LAYOUT_SOURCE_USER = "user"

/**
 * Room index of known layouts. JSON for [LAYOUT_SOURCE_USER] lives under files/layouts.
 * Builtin / template layouts stay in assets and are offered only when adding a layout.
 */
@Entity(tableName = "UserLayoutIndex")
data class UserLayoutIndex(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "tags", defaultValue = "")
    val tags: String = "",
)

@Dao
interface UserLayoutIndexDao {
    @Query("SELECT * FROM UserLayoutIndex WHERE source = :source ORDER BY title ASC")
    fun observeBySource(source: String): LiveData<List<UserLayoutIndex>>

    @Query("SELECT * FROM UserLayoutIndex ORDER BY source ASC, title ASC")
    fun observeAll(): LiveData<List<UserLayoutIndex>>

    @Query("SELECT * FROM UserLayoutIndex WHERE source = :source ORDER BY title ASC")
    suspend fun listBySource(source: String): List<UserLayoutIndex>

    @Query("SELECT * FROM UserLayoutIndex ORDER BY source ASC, title ASC")
    suspend fun listAll(): List<UserLayoutIndex>

    @Query("SELECT * FROM UserLayoutIndex WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UserLayoutIndex?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: UserLayoutIndex)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<UserLayoutIndex>)

    @Query("DELETE FROM UserLayoutIndex WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM UserLayoutIndex WHERE source = :source")
    suspend fun deleteBySource(source: String)
}
