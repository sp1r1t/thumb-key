package com.suave.s12.ui.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HideKeyGroupsTest {
    @Test
    fun `empty or unknown storage falls back to letters`() {
        assertEquals(setOf(LegendCategory.LETTER), parseHideKeyCategories(""))
        assertEquals(setOf(LegendCategory.LETTER), parseHideKeyCategories("   "))
        assertEquals(setOf(LegendCategory.LETTER), parseHideKeyCategories("FUTURE,Nope"))
        assertEquals(DEFAULT_FORMATTED_LETTERS, formatHideKeyCategories(emptySet()))
    }

    @Test
    fun `parse keeps known names and format uses settings order`() {
        assertEquals(
            setOf(LegendCategory.LETTER, LegendCategory.SYMBOL, LegendCategory.NUMBER),
            parseHideKeyCategories("NUMBER, LETTER, SYMBOL"),
        )
        assertEquals(
            "LETTER,SYMBOL,NUMBER",
            formatHideKeyCategories(
                setOf(LegendCategory.NUMBER, LegendCategory.LETTER, LegendCategory.SYMBOL),
            ),
        )
        assertEquals(
            setOf(LegendCategory.LETTER, LegendCategory.EDITING),
            parseHideKeyCategories("LETTER,FUTURE,EDITING"),
        )
    }

    @Test
    fun `round trip of every group`() {
        val all = LegendCategory.entries.toSet()
        val stored = formatHideKeyCategories(all)
        assertEquals("LETTER,SYMBOL,NUMBER,MODIFIER,LAYER_SWITCH,SPECIAL,NAVIGATION,EDITING", stored)
        assertEquals(all, parseHideKeyCategories(stored))
    }

    @Test
    fun `unchecking the last group keeps letters so the key still does something`() {
        assertEquals(
            setOf(LegendCategory.LETTER),
            toggleHideKeyGroupSelection(setOf(LegendCategory.LETTER), LegendCategory.LETTER),
        )
        assertEquals(
            setOf(LegendCategory.LETTER, LegendCategory.SYMBOL),
            toggleHideKeyGroupSelection(setOf(LegendCategory.LETTER), LegendCategory.SYMBOL),
        )
        assertEquals(
            setOf(LegendCategory.LETTER),
            toggleHideKeyGroupSelection(
                setOf(LegendCategory.LETTER, LegendCategory.SYMBOL),
                LegendCategory.SYMBOL,
            ),
        )
    }

    @Test
    fun `toggle hides selected groups when any of them is visible`() {
        val mixed =
            LegendVisibility(
                hideLetters = true,
                hideSymbols = false,
                hideNumbers = true,
            )
        val next =
            toggleHideKeyGroups(
                setOf(LegendCategory.LETTER, LegendCategory.SYMBOL, LegendCategory.NUMBER),
                mixed,
            )
        assertTrue(next.hideLetters)
        assertTrue(next.hideSymbols)
        assertTrue(next.hideNumbers)
        assertFalse(next.hideModifiers)
    }

    @Test
    fun `toggle shows selected groups when they are all already hidden`() {
        val hidden =
            LegendVisibility(
                hideLetters = true,
                hideSymbols = true,
            )
        val next =
            toggleHideKeyGroups(
                setOf(LegendCategory.LETTER, LegendCategory.SYMBOL),
                hidden,
            )
        assertFalse(next.hideLetters)
        assertFalse(next.hideSymbols)
        assertFalse(next.hideNumbers)
    }

    @Test
    fun `toggle leaves unselected groups alone`() {
        val visibility =
            LegendVisibility(
                hideLetters = false,
                hideSymbols = true,
                hideEditing = true,
            )
        val next = toggleHideKeyGroups(setOf(LegendCategory.LETTER), visibility)
        assertTrue(next.hideLetters)
        assertTrue(next.hideSymbols)
        assertTrue(next.hideEditing)
    }

    @Test
    fun `empty selection is treated as letters`() {
        val next = toggleHideKeyGroups(emptySet(), LegendVisibility())
        assertTrue(next.hideLetters)
        assertFalse(next.hideSymbols)
    }

    companion object {
        private const val DEFAULT_FORMATTED_LETTERS = "LETTER"
    }
}
