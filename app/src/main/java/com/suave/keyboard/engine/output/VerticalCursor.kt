package com.suave.keyboard.engine.output

import com.suave.keyboard.engine.action.CursorDirection

/**
 * Local (0-based within [text]) cursor index after moving one logical line up or down, keeping
 * the column when possible. Returns the same [cursor] when already on the first/last line so
 * callers can treat the move as handled without sending a DPAD KeyEvent.
 *
 * Returns null only for non-vertical [direction] values.
 */
internal fun verticalCursorOffset(
    text: CharSequence,
    cursor: Int,
    direction: CursorDirection,
): Int? {
    val clamped = cursor.coerceIn(0, text.length)
    val lineStart =
        if (clamped == 0) {
            0
        } else {
            val nl = text.lastIndexOf('\n', clamped - 1)
            if (nl < 0) 0 else nl + 1
        }
    val col = clamped - lineStart
    return when (direction) {
        CursorDirection.DOWN -> {
            val nl = text.indexOf('\n', clamped)
            if (nl < 0) return clamped
            val nextStart = nl + 1
            val nextEnd = text.indexOf('\n', nextStart).let { if (it < 0) text.length else it }
            (nextStart + col).coerceAtMost(nextEnd)
        }

        CursorDirection.UP -> {
            if (lineStart == 0) return clamped
            val prevEnd = lineStart - 1
            val prevStart =
                if (prevEnd <= 0) {
                    0
                } else {
                    val nl = text.lastIndexOf('\n', prevEnd - 1)
                    if (nl < 0) 0 else nl + 1
                }
            (prevStart + col).coerceAtMost(prevEnd)
        }

        CursorDirection.LEFT, CursorDirection.RIGHT -> null
    }
}
