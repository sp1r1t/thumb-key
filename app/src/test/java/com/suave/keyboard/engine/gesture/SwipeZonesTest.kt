package com.suave.keyboard.engine.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwipeZonesTest {
    @Test
    fun `cardinals only expand to 90deg so a diagonal angle resolves to the adjacent cardinal`() {
        // Same vector as the n-cluster regression: slightly high left -> LEFT, not UP_LEFT.
        assertEquals(
            Direction.LEFT,
            resolveSwipeDirection(dx = -40f, dy = -20f, occupied = CARDINAL_SWIPE_MASK),
        )
    }

    @Test
    fun `all eight occupied keeps 45deg wedges`() {
        assertEquals(
            Direction.UP_LEFT,
            resolveSwipeDirection(dx = -40f, dy = -20f, occupied = ALL_SWIPE_MASK),
        )
    }

    @Test
    fun `only L and R leave pure down unclaimed`() {
        val mask = swipeMask(Direction.LEFT, Direction.RIGHT)
        assertNull(resolveSwipeDirection(dx = 0f, dy = 50f, occupied = mask))
        assertEquals(Direction.LEFT, resolveSwipeDirection(dx = -50f, dy = 0f, occupied = mask))
        assertEquals(Direction.RIGHT, resolveSwipeDirection(dx = 50f, dy = 0f, occupied = mask))
    }

    @Test
    fun `missing DL gives its L-owned half to LEFT and leaves the D-owned half unclaimed when D is empty`() {
        val mask = swipeMask(Direction.LEFT, Direction.RIGHT)
        // Toward left side of former DL (~210deg): left and slightly down.
        assertEquals(Direction.LEFT, resolveSwipeDirection(dx = -35f, dy = 20f, occupied = mask))
        // Toward down side of former DL (~240deg): down and slightly left - D missing -> unclaimed.
        assertNull(resolveSwipeDirection(dx = -20f, dy = 35f, occupied = mask))
    }

    @Test
    fun `occupiedSwipeMask ignores Center`() {
        val intents =
            mapOf(
                Zone.Center to "n",
                Zone.Directional(Direction.LEFT) to "g",
                Zone.Directional(Direction.RIGHT) to "k",
            )
        assertEquals(swipeMask(Direction.LEFT, Direction.RIGHT), occupiedSwipeMask(intents))
    }
}
