package com.suave.keyboard.layout

import com.suave.keyboard.engine.intent.KeyPosition
import com.suave.keyboard.engine.intent.layoutRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutEditorHelpersTest {
    @Test
    fun moveKeyToGap_sameRow_shiftsIntoGap() {
        val layout = blankLayout(listOf(4))
        // [0,1,2,3] move col1 to after last -> [0,2,3,1]
        val moved =
            layout.moveKeyToGap(
                from = KeyPosition(0, 1),
                gap = KeyInsertGap(row = 0, col = 4),
            )
        val row = layoutRows(moved).single()
        assertEquals(
            listOf(
                KeyPosition(0, 0),
                KeyPosition(0, 1),
                KeyPosition(0, 2),
                KeyPosition(0, 3),
            ),
            row,
        )
        assertEquals(layout.getValue(KeyPosition(0, 0)), moved.getValue(KeyPosition(0, 0)))
        assertEquals(layout.getValue(KeyPosition(0, 2)), moved.getValue(KeyPosition(0, 1)))
        assertEquals(layout.getValue(KeyPosition(0, 3)), moved.getValue(KeyPosition(0, 2)))
        assertEquals(layout.getValue(KeyPosition(0, 1)), moved.getValue(KeyPosition(0, 3)))
    }

    @Test
    fun moveKeyToGap_sameRow_adjacentGapsAreNoop() {
        val layout = blankLayout(listOf(3))
        val from = KeyPosition(0, 1)
        assertEquals(layout, layout.moveKeyToGap(from, KeyInsertGap(0, 1)))
        assertEquals(layout, layout.moveKeyToGap(from, KeyInsertGap(0, 2)))
    }

    @Test
    fun moveKeyToGap_crossRow_compactsSourceAndInserts() {
        val layout = blankLayout(listOf(3, 2))
        val movedKey = layout.getValue(KeyPosition(0, 1))
        val moved =
            layout.moveKeyToGap(
                from = KeyPosition(0, 1),
                gap = KeyInsertGap(row = 1, col = 1),
            )
        val rows = layoutRows(moved)
        assertEquals(2, rows.size)
        assertEquals(2, rows[0].size)
        assertEquals(3, rows[1].size)
        assertEquals(movedKey, moved.getValue(KeyPosition(1, 1)))
        assertEquals(layout.getValue(KeyPosition(0, 0)), moved.getValue(KeyPosition(0, 0)))
        assertEquals(layout.getValue(KeyPosition(0, 2)), moved.getValue(KeyPosition(0, 1)))
    }

    @Test
    fun moveKeyToGap_lastKeyInRow_deletesSourceRow() {
        val layout = blankLayout(listOf(1, 2))
        val moved =
            layout.moveKeyToGap(
                from = KeyPosition(0, 0),
                gap = KeyInsertGap(row = 1, col = 0),
            )
        val rows = layoutRows(moved)
        assertEquals(1, rows.size)
        assertEquals(3, rows[0].size)
        assertEquals(layout.getValue(KeyPosition(0, 0)), moved.getValue(KeyPosition(0, 0)))
    }

    @Test
    fun moveKeyToGap_respectsMaxKeysPerRow() {
        val layout = blankLayout(listOf(2, 2))
        val blocked =
            layout.moveKeyToGap(
                from = KeyPosition(0, 0),
                gap = KeyInsertGap(row = 1, col = 1),
                maxKeysPerRow = 2,
            )
        assertTrue(blocked === layout || blocked == layout)
        assertEquals(layout, blocked)
    }
}
