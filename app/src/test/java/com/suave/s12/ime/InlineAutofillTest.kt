package com.suave.s12.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InlineAutofillTest {
    private data class Slot(
        val id: String,
        val pinned: Boolean,
    )

    @Test
    fun `top fillable skips pinned attribution chips`() {
        val fill = Slot("login", pinned = false)
        val logo = Slot("logo", pinned = true)
        assertEquals(fill, pickTopFillable(listOf(logo, fill)) { it.pinned })
        assertEquals(fill, pickTopFillable(listOf(fill, logo)) { it.pinned })
        assertEquals(logo, pickTopFillable(listOf(logo)) { it.pinned })
        assertNull(pickTopFillable(emptyList<Slot>()) { it.pinned })
    }
}
