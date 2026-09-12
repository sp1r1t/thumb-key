package com.suave.s12.db

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.suave.s12.utils.ClipboardImageStore
import com.suave.s12.utils.toBool

class ClipboardRepository(
    private val clipboardItemDao: ClipboardItemDao,
    private val appSettingsDao: AppSettingsDao,
    private val appContext: Context,
) {
    val allClipboardItems: LiveData<List<ClipboardItem>> = clipboardItemDao.getAllClipboardItems()

    @WorkerThread
    suspend fun addItem(text: String) {
        val settings = appSettingsDao.getSettingsSync()
        if (settings?.clipboardHistoryEnabled?.toBool() != true) return
        if (text.isBlank()) return

        clearExpired()

        val existingItem = clipboardItemDao.findByText(text, CLIPBOARD_MIME_TEXT)
        if (existingItem != null) {
            clipboardItemDao.update(existingItem.copy(timestamp = System.currentTimeMillis()))
            return
        }

        val item = ClipboardItem(text = text, mimeType = CLIPBOARD_MIME_TEXT)
        clipboardItemDao.insert(item)

        if (settings.clipboardSizeLimitEnabled.toBool()) {
            enforceSizeLimit(settings.clipboardMaxSize)
        }
        pruneImageFiles()
    }

    @WorkerThread
    suspend fun addImage(
        mimeType: String,
        fileName: String,
        sourceKey: String?,
    ) {
        val settings = appSettingsDao.getSettingsSync()
        if (settings?.clipboardHistoryEnabled?.toBool() != true) {
            ClipboardImageStore.fileFor(appContext, fileName).delete()
            return
        }

        clearExpired()

        if (!sourceKey.isNullOrBlank()) {
            val existing = clipboardItemDao.findBySourceKey(sourceKey)
            if (existing != null) {
                clipboardItemDao.update(existing.copy(timestamp = System.currentTimeMillis()))
                if (existing.localPath != fileName) {
                    ClipboardImageStore.fileFor(appContext, fileName).delete()
                }
                pruneImageFiles()
                return
            }
        }

        val item =
            ClipboardItem(
                text = "",
                mimeType = mimeType,
                localPath = fileName,
                sourceKey = sourceKey,
            )
        clipboardItemDao.insert(item)

        if (settings.clipboardSizeLimitEnabled.toBool()) {
            enforceSizeLimit(settings.clipboardMaxSize)
        }
        pruneImageFiles()
    }

    @WorkerThread
    suspend fun togglePin(item: ClipboardItem) {
        clipboardItemDao.update(item.copy(isPinned = !item.isPinned))
    }

    @WorkerThread
    suspend fun deleteItem(item: ClipboardItem) {
        clipboardItemDao.delete(item)
        item.localPath?.let { ClipboardImageStore.fileFor(appContext, it).delete() }
        pruneImageFiles()
    }

    @WorkerThread
    suspend fun clearAll() {
        clipboardItemDao.clearAll()
        pruneImageFiles()
    }

    @WorkerThread
    suspend fun clearUnpinned() {
        clipboardItemDao.clearUnpinnedItems()
        pruneImageFiles()
    }

    @WorkerThread
    suspend fun clearExpired() {
        val settings = appSettingsDao.getSettingsSync() ?: return
        if (!settings.clipboardAutoCleanupEnabled.toBool()) return

        val cutoffTime = System.currentTimeMillis() - (settings.clipboardCleanupAfterMinutes * 60 * 1000L)
        clipboardItemDao.deleteOlderThan(cutoffTime)
        pruneImageFiles()
    }

    private suspend fun enforceSizeLimit(maxSize: Int) {
        val unpinnedCount = clipboardItemDao.getUnpinnedCount()
        if (unpinnedCount > maxSize) {
            clipboardItemDao.deleteOldestUnpinned(unpinnedCount - maxSize)
        }
    }

    @WorkerThread
    suspend fun enforceSizeLimit() {
        val settings = appSettingsDao.getSettingsSync() ?: return
        if (!settings.clipboardSizeLimitEnabled.toBool()) return
        enforceSizeLimit(settings.clipboardMaxSize)
        pruneImageFiles()
    }

    fun shouldCaptureSystemClipboard(): Boolean {
        val settings = appSettingsDao.getSettingsSync() ?: return false
        return settings.clipboardHistoryEnabled.toBool() && settings.captureSystemClipboard.toBool()
    }

    private suspend fun pruneImageFiles() {
        val keep = clipboardItemDao.getAllLocalPaths().filter { it.isNotBlank() }
        ClipboardImageStore.prune(appContext, keep)
    }
}
