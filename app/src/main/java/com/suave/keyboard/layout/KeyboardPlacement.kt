package com.suave.keyboard.layout

import com.suave.keyboard.utils.KeyboardPosition

/**
 * Arrangements the user can enable for Move keyboard. Center parks a capped board in the
 * middle; Left / Right / Dual / Split park against the edges with a flexible gap.
 */
val TOGGLEABLE_KEYBOARD_POSITIONS: List<KeyboardPosition> =
    listOf(
        KeyboardPosition.Center,
        KeyboardPosition.Left,
        KeyboardPosition.Right,
        KeyboardPosition.Dual,
        KeyboardPosition.Split,
    )

/** Dual keys narrower than this (dp) count as cramped. Android's minimum touch target. */
const val MIN_DUAL_CELL_WIDTH_DP = 48

fun parseKeyboardPositions(stored: String?): List<KeyboardPosition> {
    val parsed =
        stored
            ?.split(",")
            ?.map { it.trim() }
            ?.mapNotNull { name -> TOGGLEABLE_KEYBOARD_POSITIONS.find { it.name == name } }
            ?.toSet()
            .orEmpty()
    return TOGGLEABLE_KEYBOARD_POSITIONS.filter { it in parsed }.ifEmpty { TOGGLEABLE_KEYBOARD_POSITIONS }
}

fun formatKeyboardPositions(selected: Collection<KeyboardPosition>): String {
    val enabled = selected.filter { it in TOGGLEABLE_KEYBOARD_POSITIONS }.distinct()
    val ordered =
        TOGGLEABLE_KEYBOARD_POSITIONS.filter { it in enabled }.ifEmpty {
            listOf(KeyboardPosition.Center)
        }
    return ordered.joinToString(",") { it.name }
}

fun toggleKeyboardPositionSelection(
    selected: Collection<KeyboardPosition>,
    position: KeyboardPosition,
): List<KeyboardPosition> {
    if (position !in TOGGLEABLE_KEYBOARD_POSITIONS) {
        return parseKeyboardPositions(formatKeyboardPositions(selected))
    }
    val set = selected.toSet()
    val next = if (position in set) set - position else set + position
    return parseKeyboardPositions(formatKeyboardPositions(next))
}

/**
 * Split cuts the grid in half. Odd column counts duplicate the middle column so each half
 * still has a center key (Suave's 5 columns: 0-1-2 | 2-3-4). Even counts do not overlap.
 */
fun splitColumnRanges(columnCount: Int): Pair<IntRange, IntRange> {
    val count = columnCount.coerceAtLeast(1)
    val mid = count / 2
    return if (count % 2 == 0) {
        (0 until mid) to (mid until count)
    } else {
        (0..mid) to (mid until count)
    }
}

fun isDualCramped(
    screenWidthDp: Int,
    columnCount: Int,
    minCellWidthDp: Int = MIN_DUAL_CELL_WIDTH_DP,
): Boolean {
    val columns = columnCount.coerceAtLeast(1)
    return screenWidthDp / (2f * columns) < minCellWidthDp
}

fun splitHalfColumnCount(columnCount: Int): Int {
    val (left, right) = splitColumnRanges(columnCount)
    return maxOf(left.count(), right.count()).coerceAtLeast(1)
}

/**
 * Cap for one column-unit of key width (dp). Keys may be [columnSpan] times this wide, but
 * never grow past it just because the screen is wide - that is what made landscape unusable.
 */
fun maxCellWidthDp(keyHeightDp: Int): Int = keyHeightDp.coerceAtLeast(MIN_DUAL_CELL_WIDTH_DP)

/** Ideal full-board width for [columnCount] columns at the capped cell size. */
fun boardWidthDp(
    columnCount: Int,
    maxCellWidthDp: Int,
): Int = columnCount.coerceAtLeast(1) * maxCellWidthDp

/**
 * Width of one Split/Dual half. Ideal is half-column-count * cell cap; never more than half
 * the screen so two parked halves always fit with a center gap when there is spare room.
 */
fun parkedHalfWidthDp(
    columnCount: Int,
    maxCellWidthDp: Int,
    screenWidthDp: Int,
): Int {
    val ideal = splitHalfColumnCount(columnCount) * maxCellWidthDp
    val room = (screenWidthDp / 2).coerceAtLeast(1)
    return minOf(ideal, room)
}

fun isSplitCramped(
    screenWidthDp: Int,
    columnCount: Int,
    minCellWidthDp: Int = MIN_DUAL_CELL_WIDTH_DP,
): Boolean = screenWidthDp / (2f * splitHalfColumnCount(columnCount)) < minCellWidthDp

/**
 * Split parks two halves on the left and right. That only pays off when the screen is
 * wider than it is tall. Dual cramped-or-not is a separate question.
 */
fun splitMakesSense(
    screenWidthDp: Int,
    screenHeightDp: Int,
    columnCount: Int,
    minCellWidthDp: Int = MIN_DUAL_CELL_WIDTH_DP,
): Boolean =
    screenWidthDp > screenHeightDp &&
        !isSplitCramped(screenWidthDp, columnCount, minCellWidthDp)

fun reachableKeyboardPositions(
    enabled: Collection<KeyboardPosition>,
    preventCrampedDual: Boolean,
    preventNeedlessSplit: Boolean,
    screenWidthDp: Int,
    screenHeightDp: Int,
    columnCount: Int,
): List<KeyboardPosition> {
    val skipDual = preventCrampedDual && isDualCramped(screenWidthDp, columnCount)
    val skipSplit =
        preventNeedlessSplit && !splitMakesSense(screenWidthDp, screenHeightDp, columnCount)
    val reachable =
        TOGGLEABLE_KEYBOARD_POSITIONS.filter { it in enabled }.filter { position ->
            when (position) {
                KeyboardPosition.Dual -> !skipDual
                KeyboardPosition.Split -> !skipSplit
                else -> true
            }
        }
    return reachable.ifEmpty { listOf(KeyboardPosition.Center) }
}

fun coerceDisplayedPosition(
    stored: KeyboardPosition,
    reachable: List<KeyboardPosition>,
): KeyboardPosition = if (stored in reachable) stored else reachable.first()

fun nextKeyboardPosition(
    current: KeyboardPosition,
    reachable: List<KeyboardPosition>,
): KeyboardPosition {
    if (reachable.size < 2) return coerceDisplayedPosition(current, reachable)
    val displayed = coerceDisplayedPosition(current, reachable)
    val index = reachable.indexOf(displayed)
    return reachable[(index + 1) % reachable.size]
}

fun canCycleKeyboardPosition(reachable: List<KeyboardPosition>): Boolean = reachable.size > 1

/**
 * Effective key height in dp: layout override, then Appearance (portrait or landscape), then
 * built-in defaults.
 */
fun resolveKeyHeightDp(
    landscape: Boolean,
    layoutKeyHeight: Int?,
    layoutLandscapeKeyHeight: Int?,
    settingsKeyHeight: Int?,
    settingsLandscapeKeyHeight: Int?,
    defaultKeyHeight: Int = 64,
    defaultLandscapeKeyHeight: Int = 48,
): Int {
    val fromLayout = if (landscape) layoutLandscapeKeyHeight else layoutKeyHeight
    if (fromLayout != null) return fromLayout.coerceIn(10, 200)
    val fromSettings = if (landscape) settingsLandscapeKeyHeight else settingsKeyHeight
    if (fromSettings != null) return fromSettings.coerceIn(10, 200)
    return (if (landscape) defaultLandscapeKeyHeight else defaultKeyHeight).coerceIn(10, 200)
}
