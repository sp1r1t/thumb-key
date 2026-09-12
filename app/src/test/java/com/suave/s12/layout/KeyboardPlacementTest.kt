package com.suave.s12.layout

import com.suave.s12.engine.gesture.GestureConfig
import com.suave.s12.engine.gesture.SwipeDirections
import com.suave.s12.engine.gesture.Zone
import com.suave.s12.engine.intent.KeyIntent
import com.suave.s12.engine.intent.KeyMapping
import com.suave.s12.engine.intent.KeyPosition
import com.suave.s12.engine.intent.Layout
import com.suave.s12.engine.intent.columnCount
import com.suave.s12.engine.intent.filterColumns
import com.suave.s12.utils.KeyboardPosition
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
        assertEquals(5, BuiltinLayouts.SUAVE.layout.columnCount())
        val (left, right) = splitColumnRanges(BuiltinLayouts.SUAVE.layout.columnCount())
        assertEquals(0..2, left)
        assertEquals(2 until 5, right)
        val leftKeys = BuiltinLayouts.SUAVE.layout.filterColumns(left)
        val rightKeys = BuiltinLayouts.SUAVE.layout.filterColumns(right)
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
        val config = GestureConfig(minSwipeDistancePx = 64f, directions = SwipeDirections.FOUR_WAY)
        val layout: Layout =
            mapOf(
                KeyPosition(0, 0) to KeyMapping(config, mapOf(Zone.Center to KeyIntent.Text("a"))),
                KeyPosition(0, 3) to
                    KeyMapping(config, mapOf(Zone.Center to KeyIntent.Text("enter")), columnSpan = 2),
            )
        assertEquals(5, layout.columnCount())
    }

    @Test
    fun `parse and format keep toggleable order and drop Left Right`() {
        assertEquals(all, parseKeyboardPositions(null))
        assertEquals(all, parseKeyboardPositions(""))
        assertEquals(all, parseKeyboardPositions("Left,Right,Nope"))
        assertEquals(
            listOf(KeyboardPosition.Center, KeyboardPosition.Split),
            parseKeyboardPositions("Split,Center,Left"),
        )
        assertEquals(
            "Center,Dual,Split",
            formatKeyboardPositions(
                listOf(KeyboardPosition.Split, KeyboardPosition.Dual, KeyboardPosition.Center, KeyboardPosition.Left),
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
        assertEquals(listOf(KeyboardPosition.Center), reachable)
        assertFalse(canCycleKeyboardPosition(reachable))
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
            listOf(KeyboardPosition.Center),
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
            listOf(KeyboardPosition.Center, KeyboardPosition.Dual),
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
    fun `cycle walks Center Dual Split when Dual fits`() {
        assertEquals(KeyboardPosition.Dual, nextKeyboardPosition(KeyboardPosition.Center, all))
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
}
