package com.suave.s12.ui.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionNoticeTest {
    @Test
    fun `notice shows only when enabled and the action succeeded`() {
        assertTrue(shouldShowActionNotice(enabled = true, succeeded = true))
        assertFalse(shouldShowActionNotice(enabled = true, succeeded = false))
        assertFalse(shouldShowActionNotice(enabled = false, succeeded = true))
        assertFalse(shouldShowActionNotice(enabled = false, succeeded = false))
    }
}
