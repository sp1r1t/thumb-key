package com.suave.s12.ui.components.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntStepperPreferenceTest {
    @Test
    fun `nextStepperValue steps inside the range`() {
        assertEquals(11, nextStepperValue(10, 1, 0..20))
        assertEquals(9, nextStepperValue(10, -1, 0..20))
        assertEquals(15, nextStepperValue(10, 5, 0..20))
    }

    @Test
    fun `nextStepperValue is null at the ends so holding a button does not rewrite the same value`() {
        assertNull(nextStepperValue(0, -1, 0..20))
        assertNull(nextStepperValue(20, 1, 0..20))
        assertEquals(0, nextStepperValue(2, -5, 0..20))
        assertEquals(20, nextStepperValue(18, 5, 0..20))
    }
}
