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
    fun `debug token nests chip state only when the field has Autofill`() {
        assertEquals(
            "af=na",
            formatAutofillDebug(
                sdkAtLeastR = false,
                inlineEnabled = true,
                hasAutofillId = true,
                status = "6",
            ),
        )
        assertEquals(
            "af=off",
            formatAutofillDebug(
                sdkAtLeastR = true,
                inlineEnabled = false,
                hasAutofillId = true,
                status = "6",
            ),
        )
        assertEquals(
            "af=n",
            formatAutofillDebug(
                sdkAtLeastR = true,
                inlineEnabled = true,
                hasAutofillId = false,
                status = "wait",
            ),
        )
        assertEquals(
            "af=y [6]",
            formatAutofillDebug(
                sdkAtLeastR = true,
                inlineEnabled = true,
                hasAutofillId = true,
                status = "6",
            ),
        )
        assertEquals(
            "af=y [wait]",
            formatAutofillDebug(
                sdkAtLeastR = true,
                inlineEnabled = true,
                hasAutofillId = true,
                status = INLINE_STATUS_WAIT,
            ),
        )
        assertEquals(
            "af=y [-]",
            formatAutofillDebug(
                sdkAtLeastR = true,
                inlineEnabled = true,
                hasAutofillId = true,
                status = "",
            ),
        )
    }

    @Test
    fun `empty ping does not clear wait until a real fill is missing`() {
        val host = InlineAutofillHost()
        host.markWaiting(40)
        assertEquals(INLINE_STATUS_WAIT, host.status.value)
        host.clear()
        assertEquals(INLINE_STATUS_IDLE, host.status.value)
    }
}
