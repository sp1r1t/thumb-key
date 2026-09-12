package com.suave.s12.engine.output

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MimeTypeMatcherTest {
    @Test
    fun `image wildcard accepts png and jpeg`() {
        val accepted = arrayOf("image/*")
        assertTrue(MimeTypeMatcher.editorAccepts(accepted, "image/png"))
        assertTrue(MimeTypeMatcher.editorAccepts(accepted, "image/jpeg"))
        assertTrue(MimeTypeMatcher.editorAccepts(accepted, "IMAGE/PNG"))
        assertFalse(MimeTypeMatcher.editorAccepts(accepted, "text/plain"))
        assertFalse(MimeTypeMatcher.editorAccepts(accepted, "video/mp4"))
    }

    @Test
    fun `concrete editor type rejects a different image subtype`() {
        val accepted = arrayOf("image/png")
        assertTrue(MimeTypeMatcher.editorAccepts(accepted, "image/png"))
        assertFalse(MimeTypeMatcher.editorAccepts(accepted, "image/jpeg"))
        assertFalse(MimeTypeMatcher.editorAccepts(accepted, "image/*"))
    }

    @Test
    fun `star star accepts anything well formed`() {
        val accepted = arrayOf("*/*")
        assertTrue(MimeTypeMatcher.editorAccepts(accepted, "image/png"))
        assertTrue(MimeTypeMatcher.editorAccepts(accepted, "text/plain"))
    }

    @Test
    fun `null or empty accepted types never match`() {
        assertFalse(MimeTypeMatcher.editorAccepts(null, "image/png"))
        assertFalse(MimeTypeMatcher.editorAccepts(emptyArray(), "image/png"))
        assertFalse(MimeTypeMatcher.editorAccepts(arrayOf("image/*"), ""))
        assertFalse(MimeTypeMatcher.editorAccepts(arrayOf("image/*"), "image"))
        assertFalse(MimeTypeMatcher.editorAccepts(arrayOf("image/*"), "image/"))
    }

    @Test
    fun `strips mime parameters`() {
        assertTrue(MimeTypeMatcher.editorAccepts(arrayOf("image/*"), "image/png; charset=binary"))
        assertTrue(MimeTypeMatcher.editorAccepts(arrayOf("image/jpeg"), "image/jpeg;foo=bar"))
    }

    @Test
    fun `firstMatching returns the clipboard type the editor accepts`() {
        val accepted = arrayOf("image/png", "image/gif")
        assertEquals(
            "image/png",
            MimeTypeMatcher.firstMatching(accepted, listOf("text/plain", "image/png")),
        )
        assertNull(MimeTypeMatcher.firstMatching(accepted, listOf("image/jpeg", "text/plain")))
    }

    @Test
    fun `isImage is the image wildcard`() {
        assertTrue(MimeTypeMatcher.isImage("image/png"))
        assertTrue(MimeTypeMatcher.isImage("image/webp"))
        assertFalse(MimeTypeMatcher.isImage("text/uri-list"))
        assertFalse(MimeTypeMatcher.isImage("application/octet-stream"))
    }

    @Test
    fun `concreteType fills an image wildcard`() {
        assertEquals("image/png", MimeTypeMatcher.concreteType("image/*"))
        assertEquals("image/jpeg", MimeTypeMatcher.concreteType("image/jpeg"))
        assertEquals("image/png", MimeTypeMatcher.concreteType("IMAGE/PNG"))
        assertTrue(MimeTypeMatcher.isWildcard("image/*"))
        assertFalse(MimeTypeMatcher.isWildcard("image/png"))
    }

    @Test
    fun `chooseOfferedMime commits when types match and refuses a concrete mismatch`() {
        val imageStar = arrayOf("image/*")
        assertEquals("image/png", MimeTypeMatcher.chooseOfferedMime(imageStar, listOf("image/png")))
        assertEquals("image/jpeg", MimeTypeMatcher.chooseOfferedMime(imageStar, listOf("image/jpeg")))
        assertEquals(
            "image/png",
            MimeTypeMatcher.chooseOfferedMime(arrayOf("image/png"), listOf("image/*")),
        )
        assertNull(
            MimeTypeMatcher.chooseOfferedMime(arrayOf("image/png"), listOf("image/jpeg")),
        )
        assertNull(
            MimeTypeMatcher.chooseOfferedMime(arrayOf("text/plain"), listOf("image/png")),
        )
        assertNull(MimeTypeMatcher.chooseOfferedMime(emptyArray(), listOf("image/png")))
    }
}
