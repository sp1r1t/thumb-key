package com.suave.keyboard.engine.capability

import android.text.InputType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorInputClassTest {
    @Test
    fun `number phone and datetime prefer the numeric layer`() {
        assertTrue(prefersNumericLayer(InputType.TYPE_CLASS_NUMBER))
        assertTrue(
            prefersNumericLayer(
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
            ),
        )
        assertTrue(prefersNumericLayer(InputType.TYPE_CLASS_PHONE))
        assertTrue(prefersNumericLayer(InputType.TYPE_CLASS_DATETIME))
        assertTrue(
            prefersNumericLayer(
                InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE,
            ),
        )
    }

    @Test
    fun `text and raw fields stay on the home layer`() {
        assertFalse(prefersNumericLayer(InputType.TYPE_CLASS_TEXT))
        assertFalse(
            prefersNumericLayer(
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            ),
        )
        assertFalse(prefersNumericLayer(InputType.TYPE_NULL))
        assertFalse(prefersNumericLayer(0))
    }
}
