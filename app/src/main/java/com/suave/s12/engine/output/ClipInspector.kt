package com.suave.s12.engine.output

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Reads a clipboard [ClipData] into text and/or an image URI without turning image URIs
 * into fake text strings (Android's coerceToText does that).
 */
object ClipInspector {
    data class ItemSnapshot(
        val uriString: String? = null,
        val text: String? = null,
    )

    data class InspectedClip(
        val text: String? = null,
        val imageUri: String? = null,
        val imageMime: String? = null,
        val sourceKey: String? = null,
    ) {
        val hasImage: Boolean
            get() = !imageUri.isNullOrBlank() && !imageMime.isNullOrBlank()
    }

    fun inspect(
        mimeTypes: List<String>,
        items: List<ItemSnapshot>,
        resolveType: (String) -> String? = { null },
    ): InspectedClip {
        var imageUri: String? = null
        var imageMime: String? = null
        var text: String? = null
        val descHasImage = mimeTypes.any { MimeTypeMatcher.isImage(it) }
        val descImageMime = mimeTypes.firstOrNull { MimeTypeMatcher.isImage(it) }

        for (item in items) {
            val uriString = item.uriString
            if (uriString != null && imageUri == null) {
                val resolved = resolveType(uriString)
                val offered =
                    buildList {
                        if (!resolved.isNullOrBlank()) add(resolved)
                        addAll(mimeTypes)
                    }
                val mime =
                    offered.firstOrNull { MimeTypeMatcher.isImage(it) }
                        ?: descImageMime.takeIf { descHasImage }
                if (mime != null) {
                    imageUri = uriString
                    imageMime = MimeTypeMatcher.concreteType(mime)
                }
            }
            val rawText = item.text?.trim()
            if (!rawText.isNullOrEmpty() && text == null) {
                text = rawText
            }
        }

        if (imageUri != null) {
            // An image clip must not also land in the text store as a URI or caption.
            text = null
        }

        val sourceKey = imageUri
        return InspectedClip(
            text = text,
            imageUri = imageUri,
            imageMime = imageMime,
            sourceKey = sourceKey,
        )
    }

    fun fromClip(
        clip: ClipData,
        resolveType: (Uri) -> String?,
    ): InspectedClip {
        val desc = clip.description
        val mimeTypes =
            if (desc != null) {
                (0 until desc.mimeTypeCount).map { desc.getMimeType(it) }
            } else {
                emptyList()
            }
        val items =
            (0 until clip.itemCount).map { index ->
                val item = clip.getItemAt(index)
                val uri = itemUri(item)
                ItemSnapshot(
                    uriString = uri?.toString(),
                    text = item.text?.toString(),
                )
            }
        return inspect(mimeTypes, items) { uriString ->
            val uri = uriString.toUriOrNull() ?: return@inspect null
            try {
                resolveType(uri)
            } catch (_: SecurityException) {
                null
            }
        }
    }

    fun isUriLikeText(
        text: String,
        imageUri: String? = null,
    ): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return true
        if (imageUri != null && trimmed == imageUri) return true
        val lower = trimmed.lowercase()
        return lower.startsWith("content://") ||
            lower.startsWith("file://") ||
            lower.startsWith("android.resource://")
    }

    private fun itemUri(item: ClipData.Item): Uri? {
        item.uri?.let { return it }
        val intent = item.intent ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    private fun String.toUriOrNull(): Uri? =
        try {
            Uri.parse(this)
        } catch (_: Exception) {
            null
        }
}

data class LiveClipboardImage(
    val uri: Uri,
    val mimeType: String,
    val sourceKey: String,
)
