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

    @Test
    fun `chip status reports empty fail and count`() {
        assertEquals(INLINE_STATUS_EMPTY, inlineChipStatus(0, 0))
        assertEquals(INLINE_STATUS_FAIL, inlineChipStatus(3, 0))
        assertEquals("2", inlineChipStatus(3, 2))
        assertEquals("3", inlineChipStatus(3, 3))
    }

    @Test
    fun `inflate wrap content matches Android WRAP_CONTENT`() {
        assertEquals(-2, INLINE_INFLATE_WRAP)
        assertEquals(0, INLINE_PRESENTATION_MIN_PX)
        assertEquals(Int.MAX_VALUE, INLINE_PRESENTATION_MAX_PX)
    }

    @Test
    fun `empty response is ignored unless waiting`() {
        val host = InlineAutofillHost()
        assertEquals(false, host.offerEmptyResponse())
        assertEquals(INLINE_STATUS_IDLE, host.status.value)
        host.markWaiting()
        assertEquals(INLINE_STATUS_WAIT, host.status.value)
        assertEquals(true, host.offerEmptyResponse())
        assertEquals(INLINE_STATUS_EMPTY, host.status.value)
        assertEquals(false, host.offerEmptyResponse())
    }

    @Test
    fun `clear after wait makes the AOSP empty ping a no-op`() {
        val host = InlineAutofillHost()
        host.markWaiting()
        host.clear()
        assertEquals(false, host.offerEmptyResponse())
        assertEquals(INLINE_STATUS_IDLE, host.status.value)
    }
}
