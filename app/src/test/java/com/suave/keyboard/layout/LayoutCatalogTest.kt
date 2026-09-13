package com.suave.keyboard.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutCatalogTest {
    private val s12 =
        BuiltinLayouts.S12.copy(
            id = "s12",
            title = "Suave Layout",
            tags = listOf("en", "split", "suave", "thumbkey"),
        )
    private val simple =
        BuiltinLayouts.S12.copy(
            id = "simple",
            title = "Simple",
            tags = listOf("en", "thumbkey", "simple"),
        )

    @Test
    fun `blank layout is first in the start picker`() {
        val choices = startLayoutChoices(listOf(simple, s12), query = "")
        assertEquals(BLANK_LAYOUT_SOURCE_ID, choices.first().id)
        assertTrue(choices.first().isBlank)
        assertEquals(listOf("blank", "simple", "s12"), choices.map { it.id })
    }

    @Test
    fun `start picker search matches title id and tags`() {
        val byTag = startLayoutChoices(listOf(simple, s12), query = "split")
        assertEquals(listOf("s12"), byTag.map { it.id })
        val byTitle = startLayoutChoices(listOf(simple, s12), query = "simple")
        assertEquals(listOf("simple"), byTitle.map { it.id })
        val blank = startLayoutChoices(listOf(s12), query = "blank", blankTitle = "Leeres Layout")
        assertEquals(listOf(BLANK_LAYOUT_SOURCE_ID), blank.map { it.id })
    }

    @Test
    fun `normalizeTags trims lowercases and dedupes`() {
        assertEquals(
            listOf("en", "thumbkey"),
            normalizeTags(listOf(" EN ", "thumbkey", "en", "")),
        )
    }

    @Test
    fun `available list search and sort`() {
        val rows =
            listOf(
                UserLayoutIndex("user_b", "Beta", updatedAt = 1L, source = LAYOUT_SOURCE_USER, tags = "en"),
                UserLayoutIndex("user_a", "Alpha", updatedAt = 3L, source = LAYOUT_SOURCE_USER, tags = "de,thumbkey"),
                UserLayoutIndex("user_c", "Gamma", updatedAt = 2L, source = LAYOUT_SOURCE_USER, tags = "en,split"),
            )
        assertEquals(
            listOf("user_a", "user_b", "user_c"),
            filterAvailableLayouts(rows, query = "", sort = LayoutSort.TITLE).map { it.id },
        )
        assertEquals(
            listOf("user_a", "user_c", "user_b"),
            filterAvailableLayouts(rows, query = "", sort = LayoutSort.UPDATED).map { it.id },
        )
        assertEquals(
            listOf("user_c"),
            filterAvailableLayouts(rows, query = "split", sort = LayoutSort.TITLE).map { it.id },
        )
        assertEquals(
            listOf("user_a"),
            filterAvailableLayouts(rows, query = "de", sort = LayoutSort.TITLE).map { it.id },
        )
    }

    @Test
    fun `rewriteLayoutIds replaces enabled template ids`() {
        val rewritten =
            rewriteLayoutIds(
                activeId = "s12",
                enabledCsv = "s12,simple",
                replacements = mapOf("s12" to "user_aaa", "simple" to "user_bbb"),
            )
        assertEquals("user_aaa", rewritten.activeId)
        assertEquals("user_aaa,user_bbb", rewritten.enabledCsv)
    }
}
