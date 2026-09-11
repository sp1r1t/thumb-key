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
}
