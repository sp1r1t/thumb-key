package com.suave.s12.utils

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import com.suave.s12.db.ClipboardRepository
import com.suave.s12.engine.output.MimeTypeMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ThumbKeyClipboardManager(
    private val context: Context,
    private val clipboardRepository: ClipboardRepository,
) {
    private val systemClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isListening = false
    private var lastClipText: String? = null

    // Image clips stay on the system clipboard (history is text-only). This flag tells Paste
    // to read the system clip so it does not insert the last stored string instead of the image.
    private var wasLastCopyOperationDoneViaSystem: Boolean = true

    private fun addToClipboardRepo(text: String) {
        if (text.isBlank() || text == lastClipText) return@addToClipboardRepo
        lastClipText = text
        Log.d(TAG, "Adding clipboard item: $text")
        scope.launch {
            clipboardRepository.addItem(text)
        }
    }

    private val clipboardListener =
        ClipboardManager.OnPrimaryClipChangedListener {
            val clip = systemClipboardManager.primaryClip
            if (clip == null || clip.itemCount == 0) return@OnPrimaryClipChangedListener
            if (clipIsImageOnly(clip)) {
                wasLastCopyOperationDoneViaSystem = true
                return@OnPrimaryClipChangedListener
            }
            val text = clip.getItemAt(0).coerceToText(context).toString()
            if (text.isBlank() || text == lastClipText) return@OnPrimaryClipChangedListener
            scope.launch {
                if (!clipboardRepository.shouldCaptureSystemClipboard()) return@launch
                lastClipText = text
                Log.d(TAG, "Adding clipboard item: $text")
                clipboardRepository.addItem(text)
                wasLastCopyOperationDoneViaSystem = true
            }
        }

    fun startListening() {
        if (!isListening) {
            systemClipboardManager.addPrimaryClipChangedListener(clipboardListener)
            isListening = true
        }
    }

    fun stopListening() {
        if (isListening) {
            systemClipboardManager.removePrimaryClipChangedListener(clipboardListener)
            isListening = false
        }
    }

    fun clearExpired() {
        scope.launch {
            clipboardRepository.clearExpired()
        }
    }

    fun addPrivateClip(text: String) {
        addToClipboardRepo(text)
        wasLastCopyOperationDoneViaSystem = false
    }

    fun wasLastCopyOperationDoneViaSystem(): Boolean = wasLastCopyOperationDoneViaSystem

    fun getLastClip(): String? = lastClipText

    private fun clipIsImageOnly(clip: ClipData): Boolean {
        val desc = clip.description ?: return false
        var hasImage = false
        for (i in 0 until desc.mimeTypeCount) {
            if (MimeTypeMatcher.isImage(desc.getMimeType(i))) hasImage = true
        }
        val hasText =
            desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
        return hasImage && !hasText
    }
}
