package com.suave.s12.engine.intent

import com.suave.s12.engine.gesture.GestureConfig
import com.suave.s12.engine.gesture.SwipeDirections
import com.suave.s12.engine.gesture.Zone
import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutRowsTest {
    private val config = GestureConfig(minSwipeDistancePx = 64f, directions = SwipeDirections.FOUR_WAY)

    private fun key(label: String) =
        KeyMapping(config, mapOf(Zone.Center to KeyIntent.Text(label)))

    @Test
    fun `layoutRows groups by row and sorts columns, including a shorter last row`() {
        val layout: Layout =
            mapOf(
                KeyPosition(0, 2) to key("c"),
                KeyPosition(0, 0) to key("a"),
                KeyPosition(1, 0) to key("d"),
                KeyPosition(0, 1) to key("b"),
                KeyPosition(1, 1) to key("e"),
            )

        assertEquals(
            listOf(
                listOf(KeyPosition(0, 0), KeyPosition(0, 1), KeyPosition(0, 2)),
                listOf(KeyPosition(1, 0), KeyPosition(1, 1)),
            ),
            layoutRows(layout),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `columnSpan below 1 is rejected as layout data, not silently clamped`() {
        KeyMapping(config, mapOf(Zone.Center to KeyIntent.Text("a")), columnSpan = 0)
    }

    @Test
    fun `bottomRow remaps the last row to row 0 and keeps mappings`() {
        val layout: Layout =
            mapOf(
                KeyPosition(0, 0) to key("a"),
                KeyPosition(1, 0) to key("b"),
                KeyPosition(1, 2) to key("c"),
            )
        val bottom = layout.bottomRow()
        assertEquals(setOf(KeyPosition(0, 0), KeyPosition(0, 2)), bottom.keys)
        assertEquals(KeyIntent.Text("b"), bottom.getValue(KeyPosition(0, 0)).intents[Zone.Center])
        assertEquals(KeyIntent.Text("c"), bottom.getValue(KeyPosition(0, 2)).intents[Zone.Center])
    }

    @Test
    fun `columnCount counts span past the last origin column`() {
        val layout: Layout =
            mapOf(
                KeyPosition(0, 0) to key("a"),
                KeyPosition(0, 3) to
                    KeyMapping(config, mapOf(Zone.Center to KeyIntent.Text("enter")), columnSpan = 2),
            )
        assertEquals(5, layout.columnCount())
        assertEquals(setOf(KeyPosition(0, 0)), layout.filterColumns(0..2).keys)
        assertEquals(setOf(KeyPosition(0, 3)), layout.filterColumns(2 until 5).keys)
    }

    @Test
    fun `bottomRow of an empty layout is empty`() {
        val empty: Layout = emptyMap()
        assertEquals(emptyMap<KeyPosition, KeyMapping>(), empty.bottomRow())
    }
}
