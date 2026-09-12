package com.suave.s12.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `presentation sizes keep a height range Bitwarden can fill`() {
        assertEquals(32, INLINE_PRESENTATION_MIN_WIDTH_DP)
        assertEquals(8, INLINE_PRESENTATION_MIN_HEIGHT_DP)
        assertEquals(48, INLINE_PRESENTATION_MAX_HEIGHT_DP)
        assertEquals(INLINE_PRESENTATION_MAX_WIDTH_PX, inlinePresentationMaxWidthPx(1080))
        assertTrue(inlinePresentationMaxWidthPx(2640) > INLINE_PRESENTATION_MAX_WIDTH_PX)
        assertEquals(6, INLINE_SUGGESTION_MAX_COUNT)
        assertEquals(6, INLINE_SUGGESTION_SPEC_COUNT)
    }

    @Test
    fun `empty ping while waiting stays wait so a later fill can arrive`() {
        val host = InlineAutofillHost()
        assertEquals(true, host.offerEmptyResponse())
        assertEquals(INLINE_STATUS_IDLE, host.status.value)
        host.markWaiting(40)
        assertEquals(INLINE_STATUS_WAIT, host.status.value)
        assertEquals(true, host.offerEmptyResponse())
        assertEquals(INLINE_STATUS_WAIT, host.status.value)
    }

    @Test
    fun `clear after wait makes a stale empty timeout a no-op`() {
        val host = InlineAutofillHost()
        host.markWaiting(40)
        host.clear()
        assertEquals(true, host.offerEmptyResponse())
        assertEquals(INLINE_STATUS_IDLE, host.status.value)
    }
}
