package com.suave.s12.engine.output

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipInspectorTest {
    @Test
    fun `image-only clip is not turned into text`() {
        val inspected =
            ClipInspector.inspect(
                mimeTypes = listOf("image/png"),
                items =
                    listOf(
                        ClipInspector.ItemSnapshot(
                            uriString = "content://media/external/images/media/12",
                            text = null,
                        ),
                    ),
            )
        assertTrue(inspected.hasImage)
        assertEquals("content://media/external/images/media/12", inspected.imageUri)
        assertEquals("image/png", inspected.imageMime)
        assertNull(inspected.text)
    }

    @Test
    fun `image plus uri-as-text drops the bogus string`() {
        val uri = "content://media/external/images/media/12"
        val inspected =
            ClipInspector.inspect(
                mimeTypes = listOf("image/jpeg", "text/plain", "text/uri-list"),
                items =
                    listOf(
                        ClipInspector.ItemSnapshot(uriString = uri, text = uri),
                    ),
            )
        assertTrue(inspected.hasImage)
        assertEquals("image/jpeg", inspected.imageMime)
        assertNull(inspected.text)
    }

    @Test
    fun `plain text clip stays text`() {
        val inspected =
            ClipInspector.inspect(
                mimeTypes = listOf("text/plain"),
                items = listOf(ClipInspector.ItemSnapshot(text = "hello")),
            )
        assertFalse(inspected.hasImage)
        assertEquals("hello", inspected.text)
        assertNull(inspected.imageUri)
    }

    @Test
    fun `resolved content type can identify an image without an image mime on the description`() {
        val uri = "content://media/external/images/media/99"
        val inspected =
            ClipInspector.inspect(
                mimeTypes = listOf("text/uri-list"),
                items = listOf(ClipInspector.ItemSnapshot(uriString = uri)),
                resolveType = { if (it == uri) "image/webp" else null },
            )
        assertTrue(inspected.hasImage)
        assertEquals("image/webp", inspected.imageMime)
        assertNull(inspected.text)
    }

    @Test
    fun `image wildcard becomes a concrete mime`() {
        val inspected =
            ClipInspector.inspect(
                mimeTypes = listOf("image/*"),
                items =
                    listOf(
                        ClipInspector.ItemSnapshot(uriString = "content://clipboard/1"),
                    ),
            )
        assertEquals("image/png", inspected.imageMime)
    }

    @Test
    fun `uri-like text without an image is still text`() {
        val inspected =
            ClipInspector.inspect(
                mimeTypes = listOf("text/plain"),
                items =
                    listOf(
                        ClipInspector.ItemSnapshot(text = "content://settings/system"),
                    ),
            )
        assertFalse(inspected.hasImage)
        assertEquals("content://settings/system", inspected.text)
    }

    @Test
    fun `isUriLikeText matches clipboard uri strings`() {
        assertTrue(ClipInspector.isUriLikeText("content://media/12"))
        assertTrue(ClipInspector.isUriLikeText("file:///data/img.png"))
        assertTrue(
            ClipInspector.isUriLikeText(
                "content://media/12",
                imageUri = "content://media/12",
            ),
        )
        assertFalse(ClipInspector.isUriLikeText("hello"))
    }
}
