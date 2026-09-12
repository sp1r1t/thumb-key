package com.suave.s12.engine.output

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.suave.s12.IMEService
import com.suave.s12.R
import com.suave.s12.db.ClipboardItem
import com.suave.s12.engine.action.SemanticAction
import com.suave.s12.engine.capability.EditorCapabilities
import com.suave.s12.engine.capability.EditorCapabilityLevel
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.utils.ClipboardImageStore
import com.suave.s12.utils.TAG
import java.io.File

/**
 * Paste for the focused editor: images go through [InputConnection.commitContent] when the
 * field declared a matching `contentMimeTypes` entry; otherwise the existing text paste path
 * is unchanged. History images are stored as files in app storage, not as dangling content URIs.
 */
object ClipboardPaste {
    private const val CACHE_DIR = "commit-content"
    private const val CACHE_TTL_MS = 5 * 60 * 1000L
    const val FILE_PROVIDER_SUFFIX = ".commitcontent"

    fun execute(
        ime: IMEService,
        capabilities: EditorCapabilities,
    ) {
        val editorInfo = ime.currentInputEditorInfo
        val useInternalClipboardText =
            ime.clipboardUsePrivate() && !ime.clipboardWasLastCopyDoneViaSystem()
        val editorIsRaw = capabilities.level == EditorCapabilityLevel.RAW
        val image =
            if (editorIsRaw || useInternalClipboardText) {
                null
            } else {
                matchingClipboardImage(ime, editorInfo)
            }
        val kind =
            PasteDecision.kind(
                editorIsRaw = editorIsRaw,
                useInternalClipboardText = useInternalClipboardText,
                hasMatchingCommitContent = image != null,
            )
        when (kind) {
            PasteKind.RAW_SHORTCUT -> {
                // Termux binds paste to Ctrl+Alt+V (termux-app discussion #3928).
                val ic = ime.currentInputConnection ?: return
                OutputExecutor.execute(
                    SemanticAction.TypeText("v", setOf(ModifierId.CTRL, ModifierId.ALT)),
                    capabilities,
                    ic,
                )
            }
            PasteKind.COMMIT_TEXT -> {
                val ic = ime.currentInputConnection ?: return
                val text = ime.clipboardGetLastClip()
                if (!text.isNullOrEmpty()) ic.commitText(text, 1)
            }
            PasteKind.COMMIT_CONTENT -> {
                val ic = ime.currentInputConnection ?: return
                if (image == null || !commitImage(ime, ic, editorInfo, image)) {
                    ime.showNotice(ime.getString(R.string.paste_image_failed))
                }
            }
            PasteKind.CONTEXT_MENU_PASTE -> {
                val ic = ime.currentInputConnection ?: return
                ic.performContextMenuAction(android.R.id.paste)
            }
        }
    }

    /**
     * Paste a history row. Returns true when the editor received content so the clipboard
     * layer can close. Image taps that the field rejects stay on the layer and show a notice.
     */
    fun pasteHistoryItem(
        ime: IMEService,
        item: ClipboardItem,
    ): Boolean {
        if (item.isImage()) {
            return pasteStoredImage(ime, item)
        }
        val ic = ime.currentInputConnection ?: return false
        if (item.text.isEmpty()) return false
        ic.commitText(item.text, 1)
        return true
    }

    fun pasteLiveImage(
        ime: IMEService,
        live: LiveClipboardImage,
    ): Boolean {
        val editorInfo = ime.currentInputEditorInfo
        val ic = ime.currentInputConnection ?: return false
        val accepted = EditorInfoCompat.getContentMimeTypes(editorInfo)
        if (!MimeTypeMatcher.editorAccepts(accepted, live.mimeType)) {
            ime.showNotice(ime.getString(R.string.paste_image_not_accepted))
            return false
        }
        val image = ClipboardImage(uri = live.uri, mimeType = live.mimeType)
        if (!commitImage(ime, ic, editorInfo, image)) {
            ime.showNotice(ime.getString(R.string.paste_image_failed))
            return false
        }
        return true
    }

    private fun pasteStoredImage(
        ime: IMEService,
        item: ClipboardItem,
    ): Boolean {
        val editorInfo = ime.currentInputEditorInfo
        val ic = ime.currentInputConnection ?: return false
        val accepted = EditorInfoCompat.getContentMimeTypes(editorInfo)
        if (!MimeTypeMatcher.editorAccepts(accepted, item.mimeType)) {
            ime.showNotice(ime.getString(R.string.paste_image_not_accepted))
            return false
        }
        val fileName = item.localPath
        if (fileName.isNullOrBlank()) {
            ime.showNotice(ime.getString(R.string.paste_image_failed))
            return false
        }
        val file = ClipboardImageStore.fileFor(ime, fileName)
        if (!file.isFile) {
            ime.showNotice(ime.getString(R.string.paste_image_failed))
            return false
        }
        val image = ClipboardImage(uri = Uri.fromFile(file), mimeType = item.mimeType)
        if (!commitImage(ime, ic, editorInfo, image)) {
            ime.showNotice(ime.getString(R.string.paste_image_failed))
            return false
        }
        return true
    }

    internal fun matchingClipboardImage(
        context: Context,
        editorInfo: EditorInfo?,
    ): ClipboardImage? {
        if (editorInfo == null) return null
        val accepted = EditorInfoCompat.getContentMimeTypes(editorInfo)
        if (accepted.isEmpty()) return null
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip ?: return null
        return imageFromClip(context, clip, accepted)
    }

    internal fun imageFromClip(
        context: Context,
        clip: android.content.ClipData,
        accepted: Array<out String>,
    ): ClipboardImage? {
        val inspected =
            ClipInspector.fromClip(clip) { uri ->
                try {
                    context.contentResolver.getType(uri)
                } catch (_: SecurityException) {
                    null
                }
            }
        if (!inspected.hasImage) return null
        val mime = inspected.imageMime ?: return null
        val uriString = inspected.imageUri ?: return null
        if (!MimeTypeMatcher.editorAccepts(accepted, mime)) return null
        return ClipboardImage(uri = Uri.parse(uriString), mimeType = mime)
    }

    internal fun commitImage(
        context: Context,
        ic: InputConnection,
        editorInfo: EditorInfo?,
        image: ClipboardImage,
    ): Boolean {
        if (editorInfo == null) return false
        val exported = exportImage(context, image) ?: return false
        val description = ClipDescription("image", arrayOf(image.mimeType))
        val contentInfo = InputContentInfoCompat(exported, description, null)
        val editorPackage = editorInfo.packageName
        if (!editorPackage.isNullOrBlank()) {
            try {
                context.grantUriPermission(
                    editorPackage,
                    exported,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (e: SecurityException) {
                Log.w(TAG, "grantUriPermission for commitContent failed", e)
            }
        }
        ic.finishComposingText()
        return InputConnectionCompat.commitContent(
            ic,
            editorInfo,
            contentInfo,
            InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
            null,
        )
    }

    private fun exportImage(
        context: Context,
        image: ClipboardImage,
    ): Uri? {
        val dir = File(context.cacheDir, CACHE_DIR)
        if (!dir.exists() && !dir.mkdirs()) return null
        pruneCache(dir)
        val ext =
            MimeTypeMap.getSingleton().getExtensionFromMimeType(image.mimeType)
                ?: if (MimeTypeMatcher.isImage(image.mimeType)) "png" else "bin"
        val file = File(dir, "clip_${System.currentTimeMillis()}.$ext")
        try {
            ClipboardImageStore.openStream(context, image.uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy clipboard image for commitContent", e)
            file.delete()
            return null
        }
        if (file.length() == 0L) {
            file.delete()
            return null
        }
        return try {
            FileProvider.getUriForFile(
                context,
                fileProviderAuthority(context.packageName),
                file,
            )
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "FileProvider rejected commitContent uri", e)
            file.delete()
            null
        }
    }

    private fun pruneCache(dir: File) {
        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { file ->
            if (now - file.lastModified() > CACHE_TTL_MS) file.delete()
        }
    }

    fun fileProviderAuthority(packageName: String): String = packageName + FILE_PROVIDER_SUFFIX
}

internal data class ClipboardImage(
    val uri: Uri,
    val mimeType: String,
)
