package com.suave.s12.utils

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import com.suave.s12.db.ClipboardRepository
import com.suave.s12.engine.output.ClipInspector
import com.suave.s12.engine.output.LiveClipboardImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private var lastImageSourceKey: String? = null

    private val liveImageState = MutableStateFlow<LiveClipboardImage?>(null)
    val liveImage: StateFlow<LiveClipboardImage?> = liveImageState.asStateFlow()

    // When the last copy was an image (or any system clip), Paste reads the system clipboard
    // instead of inserting the last stored private-clipboard string.
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
            ingestPrimaryClip()
        }

    fun startListening() {
        if (!isListening) {
            systemClipboardManager.addPrimaryClipChangedListener(clipboardListener)
            isListening = true
            ingestPrimaryClip()
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
        if (ClipInspector.isUriLikeText(text)) return
        addToClipboardRepo(text)
        wasLastCopyOperationDoneViaSystem = false
        liveImageState.value = null
        lastImageSourceKey = null
    }

    fun wasLastCopyOperationDoneViaSystem(): Boolean = wasLastCopyOperationDoneViaSystem

    fun getLastClip(): String? = lastClipText

    fun ingestPrimaryClip() {
        val clip = systemClipboardManager.primaryClip
        if (clip == null || clip.itemCount == 0) return
        val inspected =
            ClipInspector.fromClip(clip) { uri ->
                try {
                    context.contentResolver.getType(uri)
                } catch (_: SecurityException) {
                    null
                }
            }
        if (inspected.hasImage) {
            wasLastCopyOperationDoneViaSystem = true
            val uriString = inspected.imageUri ?: return
            val mime = inspected.imageMime ?: return
            val uri = Uri.parse(uriString)
            val sourceKey = inspected.sourceKey ?: uriString
            liveImageState.value =
                LiveClipboardImage(
                    uri = uri,
                    mimeType = mime,
                    sourceKey = sourceKey,
                )
            if (sourceKey == lastImageSourceKey) return
            scope.launch {
                if (!clipboardRepository.shouldCaptureSystemClipboard()) return@launch
                val fileName =
                    ClipboardImageStore.copyFromUri(
                        context,
                        uri,
                        mime,
                    ) ?: return@launch
                clipboardRepository.addImage(
                    mimeType = mime,
                    fileName = fileName,
                    sourceKey = sourceKey,
                )
                lastImageSourceKey = sourceKey
            }
            return
        }

        liveImageState.value = null
        lastImageSourceKey = null
        val text = inspected.text
        if (text.isNullOrBlank() || text == lastClipText) return
        if (ClipInspector.isUriLikeText(text)) return
        scope.launch {
            if (!clipboardRepository.shouldCaptureSystemClipboard()) return@launch
            lastClipText = text
            Log.d(TAG, "Adding clipboard item: $text")
            clipboardRepository.addItem(text)
            wasLastCopyOperationDoneViaSystem = true
        }
    }
}
