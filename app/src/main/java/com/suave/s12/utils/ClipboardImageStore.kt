package com.suave.s12.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.suave.s12.engine.output.MimeTypeMatcher
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

object ClipboardImageStore {
    const val DIR_NAME = "clipboard-images"
    private const val MAX_BYTES = 20L * 1024L * 1024L

    fun dir(context: Context): File = File(context.filesDir, DIR_NAME)

    fun fileFor(
        context: Context,
        fileName: String,
    ): File = File(dir(context), fileName)

    fun copyFromUri(
        context: Context,
        source: Uri,
        mimeType: String,
    ): String? {
        val directory = dir(context)
        if (!directory.exists() && !directory.mkdirs()) return null
        val ext =
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                ?: if (MimeTypeMatcher.isImage(mimeType)) "png" else "bin"
        val fileName = "clip_${System.currentTimeMillis()}.$ext"
        val dest = File(directory, fileName)
        return try {
            openStream(context, source)?.use { input ->
                dest.outputStream().use { output ->
                    val copied = input.copyTo(output, MAX_BYTES)
                    if (copied < 0L) {
                        dest.delete()
                        return null
                    }
                }
            } ?: return null
            if (dest.length() == 0L) {
                dest.delete()
                null
            } else {
                fileName
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy clipboard image into history", e)
            dest.delete()
            null
        }
    }

    fun openStream(
        context: Context,
        uri: Uri,
    ): InputStream? {
        if (uri.scheme == ContentResolver.SCHEME_FILE || uri.scheme == "file") {
            val path = uri.path ?: return null
            return FileInputStream(File(path))
        }
        return try {
            context.contentResolver.openInputStream(uri)
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to read clipboard image", e)
            null
        }
    }

    fun prune(
        context: Context,
        keepFileNames: Collection<String>,
    ) {
        val directory = dir(context)
        if (!directory.exists()) return
        val keep = keepFileNames.toSet()
        directory.listFiles()?.forEach { file ->
            if (file.name !in keep) file.delete()
        }
    }

    private fun InputStream.copyTo(
        out: java.io.OutputStream,
        maxBytes: Long,
    ): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) return -1L
            out.write(buffer, 0, read)
        }
        return total
    }
}
