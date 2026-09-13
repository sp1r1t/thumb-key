package com.suave.keyboard.layout

import com.suave.keyboard.engine.gesture.GestureConfig
import com.suave.keyboard.engine.gesture.Zone
import com.suave.keyboard.engine.gesture.CARDINAL_SWIPE_MASK
import com.suave.keyboard.engine.intent.KeyIntent
import com.suave.keyboard.engine.intent.KeyMapping
import com.suave.keyboard.engine.intent.KeyPosition
import com.suave.keyboard.engine.intent.Layout
import com.suave.keyboard.engine.intent.columnCount
import com.suave.keyboard.engine.intent.filterColumns
import com.suave.keyboard.utils.KeyboardPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardPlacementTest {
    private val all = TOGGLEABLE_KEYBOARD_POSITIONS
    private val phonePortraitW = 360
    private val phonePortraitH = 800
    private val phoneLandscapeW = 800
    private val phoneLandscapeH = 360
    private val tabletLandscapeW = 960
    private val tabletLandscapeH = 600

    private fun reachable(
        width: Int,
        height: Int,
        preventCrampedDual: Boolean = true,
        preventNeedlessSplit: Boolean = true,
        enabled: Collection<KeyboardPosition> = all,
        columnCount: Int = 5,
    ): List<KeyboardPosition> =
        reachableKeyboardPositions(
            enabled = enabled,
            preventCrampedDual = preventCrampedDual,
            preventNeedlessSplit = preventNeedlessSplit,
            screenWidthDp = width,
            screenHeightDp = height,
            columnCount = columnCount,
        )

    @Test
    fun `split ranges duplicate the middle column only when the count is odd`() {
        assertEquals((0 until 2) to (2 until 4), splitColumnRanges(4))
        assertEquals((0..2) to (2 until 5), splitColumnRanges(5))
        assertEquals((0..0) to (0 until 1), splitColumnRanges(1))
    }

    @Test
    fun `suave split duplicates only the center column`() {
        val s12 = com.suave.keyboard.layout.json.loadS12Asset()
        val grid = s12.homeLayer().keyGrid
        assertEquals(5, grid.columnCount())
        val (left, right) = splitColumnRanges(grid.columnCount())
        assertEquals(0..2, left)
        assertEquals(2 until 5, right)
        val leftKeys = grid.filterColumns(left)
        val rightKeys = grid.filterColumns(right)
        assertTrue(leftKeys.keys.all { it.col in 0..2 })
        assertTrue(rightKeys.keys.all { it.col in 2..4 })
        assertEquals(
            leftKeys.keys.filter { it.col == 2 }.toSet(),
            rightKeys.keys.filter { it.col == 2 }.toSet(),
        )
        assertTrue(leftKeys.keys.none { it.col == 3 || it.col == 4 })
        assertTrue(rightKeys.keys.none { it.col == 0 || it.col == 1 })
    }

    @Test
    fun `columnCount counts span so Enter still fills five Suave columns`() {
        val config = GestureConfig(minSwipeDistancePx = 64f, occupiedDirections = CARDINAL_SWIPE_MASK)
        val layout: Layout =
            mapOf(
                KeyPosition(0, 0) to KeyMapping(config, mapOf(Zone.Center to KeyIntent.Text("a"))),
                KeyPosition(0, 3) to
                    KeyMapping(config, mapOf(Zone.Center to KeyIntent.Text("enter")), columnSpan = 2f),
            )
        assertEquals(5, layout.columnCount())
    }

    @Test
    fun `board and half widths cap cells and leave a landscape gap`() {
        val cell = maxCellWidthDp(keyHeightDp = 64)
        assertEquals(64, cell)
        assertEquals(320, boardWidthDp(columnCount = 5, maxCellWidthDp = cell))
        // Dual copies use the full Center board width, not Split's 3-column half.
        assertEquals(
            320,
            parkedBoardWidthDp(columnCount = 5, maxCellWidthDp = cell, screenWidthDp = phoneLandscapeW),
        )
        assertTrue(
            parkedBoardWidthDp(columnCount = 5, maxCellWidthDp = cell, screenWidthDp = phoneLandscapeW) >
                parkedHalfWidthDp(columnCount = 5, maxCellWidthDp = cell, screenWidthDp = phoneLandscapeW),
        )
        // Phone landscape: ideal Split half is 3*64=192, half screen is 400 -> park at 192, gap remains.
        assertEquals(
            192,
            parkedHalfWidthDp(columnCount = 5, maxCellWidthDp = cell, screenWidthDp = phoneLandscapeW),
        )
        // Narrow screen: Split never exceeds half the screen; Dual still keeps key width up to the screen.
        assertEquals(
            100,
            parkedHalfWidthDp(columnCount = 5, maxCellWidthDp = cell, screenWidthDp = 200),
        )
        assertEquals(
            200,
            parkedBoardWidthDp(columnCount = 5, maxCellWidthDp = cell, screenWidthDp = 200),
        )
        // Portrait phone: Split half 192 vs half screen 180 -> 180. Dual stays 320 (full keys).
        assertEquals(
            180,
            parkedHalfWidthDp(columnCount = 5, maxCellWidthDp = cell, screenWidthDp = phonePortraitW),
        )
        assertEquals(
            320,
            parkedBoardWidthDp(columnCount = 5, maxCellWidthDp = cell, screenWidthDp = phonePortraitW),
        )
    }

    @Test
    fun `parse and format keep toggleable order including Left Right`() {
        assertEquals(all, parseKeyboardPositions(null))
        assertEquals(all, parseKeyboardPositions(""))
        assertEquals(
            listOf(KeyboardPosition.Center, KeyboardPosition.Dual, KeyboardPosition.Split),
            parseKeyboardPositions("Center,Dual,Split,Nope"),
        )
        assertEquals(
            listOf(KeyboardPosition.Center, KeyboardPosition.Left, KeyboardPosition.Split),
            parseKeyboardPositions("Split,Center,Left"),
        )
        assertEquals(
            "Center,Left,Right,Dual,Split",
            formatKeyboardPositions(
                listOf(
                    KeyboardPosition.Split,
                    KeyboardPosition.Dual,
                    KeyboardPosition.Center,
                    KeyboardPosition.Left,
                    KeyboardPosition.Right,
                ),
            ),
        )
    }

    @Test
    fun `toggle keeps at least one position`() {
        val onlyCenter = listOf(KeyboardPosition.Center)
        assertEquals(onlyCenter, toggleKeyboardPositionSelection(onlyCenter, KeyboardPosition.Center))
        assertEquals(
            listOf(KeyboardPosition.Center, KeyboardPosition.Split),
            toggleKeyboardPositionSelection(onlyCenter, KeyboardPosition.Split),
        )
        assertEquals(
            onlyCenter,
            toggleKeyboardPositionSelection(
                listOf(KeyboardPosition.Center, KeyboardPosition.Split),
                KeyboardPosition.Split,
            ),
        )
    }

    @Test
    fun `dual is cramped on a phone-width 5 column board and not on a tablet`() {
        assertTrue(isDualCramped(phonePortraitW, columnCount = 5))
        assertFalse(isDualCramped(tabletLandscapeW, columnCount = 5))
        assertFalse(isDualCramped(phonePortraitW, columnCount = 2))
    }

    @Test
    fun `reachable drops Dual when prevent-cramped is on and the board is tight`() {
        val reachable =
            reachable(
                width = phonePortraitW,
                height = phonePortraitH,
            )
        assertEquals(
            listOf(KeyboardPosition.Center, KeyboardPosition.Left, KeyboardPosition.Right),
            reachable,
        )
        assertTrue(canCycleKeyboardPosition(reachable))
    }

    @Test
    fun `reachable keeps Dual when prevent-cramped is off or there is room`() {
        assertEquals(
            all,
            reachable(
                width = phonePortraitW,
                height = phonePortraitH,
                preventCrampedDual = false,
                preventNeedlessSplit = false,
            ),
        )
        assertEquals(
            all,
            reachable(
                width = tabletLandscapeW,
                height = tabletLandscapeH,
                preventNeedlessSplit = false,
            ),
        )
    }

    @Test
    fun `split makes sense in landscape not in portrait`() {
        assertFalse(splitMakesSense(phonePortraitW, phonePortraitH, columnCount = 5))
        assertTrue(splitMakesSense(phoneLandscapeW, phoneLandscapeH, columnCount = 5))
        assertTrue(splitMakesSense(tabletLandscapeW, tabletLandscapeH, columnCount = 5))
        assertTrue(isSplitCramped(200, columnCount = 5))
        assertFalse(splitMakesSense(200, 120, columnCount = 5))
    }

    @Test
    fun `reachable drops Split in portrait even when Dual is cramped`() {
        assertEquals(
            listOf(KeyboardPosition.Center, KeyboardPosition.Left, KeyboardPosition.Right),
            reachable(width = phonePortraitW, height = phonePortraitH),
        )
        assertEquals(
            all,
            reachable(width = phoneLandscapeW, height = phoneLandscapeH),
        )
        assertEquals(
            all,
            reachable(width = tabletLandscapeW, height = tabletLandscapeH),
        )
        assertEquals(
            listOf(
                KeyboardPosition.Center,
                KeyboardPosition.Left,
                KeyboardPosition.Right,
                KeyboardPosition.Dual,
            ),
            reachable(width = 600, height = 960),
        )
        assertEquals(
            all,
            reachable(
                width = phonePortraitW,
                height = phonePortraitH,
                preventNeedlessSplit = false,
                preventCrampedDual = false,
            ),
        )
    }

    @Test
    fun `cycle skips Dual when it is not reachable and no-ops with a single stop`() {
        val reachable = listOf(KeyboardPosition.Center, KeyboardPosition.Split)
        assertEquals(KeyboardPosition.Split, nextKeyboardPosition(KeyboardPosition.Center, reachable))
        assertEquals(KeyboardPosition.Center, nextKeyboardPosition(KeyboardPosition.Split, reachable))
        assertEquals(KeyboardPosition.Split, nextKeyboardPosition(KeyboardPosition.Dual, reachable))
        assertEquals(KeyboardPosition.Split, nextKeyboardPosition(KeyboardPosition.Left, reachable))
        assertEquals(
            KeyboardPosition.Center,
            nextKeyboardPosition(KeyboardPosition.Dual, listOf(KeyboardPosition.Center)),
        )
    }

    @Test
    fun `cycle walks Center Left Right Dual Split when Dual fits`() {
        assertEquals(KeyboardPosition.Left, nextKeyboardPosition(KeyboardPosition.Center, all))
        assertEquals(KeyboardPosition.Right, nextKeyboardPosition(KeyboardPosition.Left, all))
        assertEquals(KeyboardPosition.Dual, nextKeyboardPosition(KeyboardPosition.Right, all))
        assertEquals(KeyboardPosition.Split, nextKeyboardPosition(KeyboardPosition.Dual, all))
        assertEquals(KeyboardPosition.Center, nextKeyboardPosition(KeyboardPosition.Split, all))
    }

    @Test
    fun `displayed position falls back to the first reachable stop`() {
        val reachable = listOf(KeyboardPosition.Center, KeyboardPosition.Split)
        assertEquals(KeyboardPosition.Dual, coerceDisplayedPosition(KeyboardPosition.Dual, all))
        assertEquals(KeyboardPosition.Center, coerceDisplayedPosition(KeyboardPosition.Dual, reachable))
        assertEquals(KeyboardPosition.Split, coerceDisplayedPosition(KeyboardPosition.Split, reachable))
    }

    @Test
    fun `resolveKeyHeight prefers layout then settings then defaults`() {
        assertEquals(
            72,
            resolveKeyHeightDp(
                landscape = false,
                layoutKeyHeight = 72,
                layoutLandscapeKeyHeight = 40,
                settingsKeyHeight = 64,
                settingsLandscapeKeyHeight = 48,
            ),
        )
        assertEquals(
            40,
            resolveKeyHeightDp(
                landscape = true,
                layoutKeyHeight = 72,
                layoutLandscapeKeyHeight = 40,
                settingsKeyHeight = 64,
                settingsLandscapeKeyHeight = 48,
            ),
        )
        assertEquals(
            64,
            resolveKeyHeightDp(
                landscape = false,
                layoutKeyHeight = null,
                layoutLandscapeKeyHeight = null,
                settingsKeyHeight = 64,
                settingsLandscapeKeyHeight = 48,
            ),
        )
        assertEquals(
            48,
            resolveKeyHeightDp(
                landscape = true,
                layoutKeyHeight = null,
                layoutLandscapeKeyHeight = null,
                settingsKeyHeight = 64,
                settingsLandscapeKeyHeight = 48,
            ),
        )
        assertEquals(
            64,
            resolveKeyHeightDp(
                landscape = false,
                layoutKeyHeight = null,
                layoutLandscapeKeyHeight = null,
                settingsKeyHeight = null,
                settingsLandscapeKeyHeight = null,
            ),
        )
        assertEquals(
            48,
            resolveKeyHeightDp(
                landscape = true,
                layoutKeyHeight = null,
                layoutLandscapeKeyHeight = null,
                settingsKeyHeight = null,
                settingsLandscapeKeyHeight = null,
            ),
        )
    }
}
