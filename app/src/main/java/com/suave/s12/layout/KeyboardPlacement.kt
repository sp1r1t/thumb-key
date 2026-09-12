package com.suave.s12.layout

import com.suave.s12.utils.KeyboardPosition

/**
 * Arrangements the user can enable for Move keyboard. Left and Right still render as full-width
 * Center, so they stay out of this list until they actually park a narrower board.
 */
val TOGGLEABLE_KEYBOARD_POSITIONS: List<KeyboardPosition> =
    listOf(
        KeyboardPosition.Center,
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

fun reachableKeyboardPositions(
    enabled: Collection<KeyboardPosition>,
    preventCrampedDual: Boolean,
    screenWidthDp: Int,
    columnCount: Int,
): List<KeyboardPosition> {
    val skipDual = preventCrampedDual && isDualCramped(screenWidthDp, columnCount)
    val reachable =
        TOGGLEABLE_KEYBOARD_POSITIONS.filter { it in enabled }.filter {
            it != KeyboardPosition.Dual || !skipDual
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
