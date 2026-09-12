package com.suave.s12.engine.output

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
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
import com.suave.s12.engine.action.SemanticAction
import com.suave.s12.engine.capability.EditorCapabilities
import com.suave.s12.engine.capability.EditorCapabilityLevel
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.utils.TAG
import java.io.File

/**
 * Paste for the focused editor: images go through [InputConnection.commitContent] when the
 * field declared a matching `contentMimeTypes` entry; otherwise the existing text paste path
 * is unchanged. Clipboard history stays text-only; this reads the current system clip.
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
        clip: ClipData,
        accepted: Array<out String>,
    ): ClipboardImage? {
        val desc = clip.description
        val descMimes =
            if (desc != null) {
                (0 until desc.mimeTypeCount).map { desc.getMimeType(it) }
            } else {
                emptyList()
            }
        for (i in 0 until clip.itemCount) {
            val item = clip.getItemAt(i)
            val uri = item.uri
            if (uri != null) {
                val resolverType =
                    try {
                        context.contentResolver.getType(uri)
                    } catch (_: SecurityException) {
                        null
                    }
                val offered = mutableListOf<String>()
                if (!resolverType.isNullOrBlank()) offered.add(resolverType)
                offered.addAll(descMimes)
                val mime = MimeTypeMatcher.chooseOfferedMime(accepted, offered)
                if (mime != null) {
                    return ClipboardImage(uri = uri, bitmap = null, mimeType = mime)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val bitmap = item.bitmap
                if (bitmap != null) {
                    val mime = MimeTypeMatcher.chooseOfferedMime(accepted, descMimes + "image/png")
                    if (mime != null) {
                        return ClipboardImage(uri = null, bitmap = bitmap, mimeType = mime)
                    }
                }
            }
        }
        return null
    }

    private fun commitImage(
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
            when {
                image.uri != null -> {
                    context.contentResolver.openInputStream(image.uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    } ?: return null
                }
                image.bitmap != null -> {
                    file.outputStream().use { output ->
                        if (!image.bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                            return null
                        }
                    }
                }
                else -> return null
            }
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
    val uri: Uri?,
    val bitmap: Bitmap?,
    val mimeType: String,
)
