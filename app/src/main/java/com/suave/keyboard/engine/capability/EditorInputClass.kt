package com.suave.keyboard.engine.capability

import android.text.InputType

/**
 * Whether this [EditorInfo.inputType] should open the numeric layer. Number, phone, and
 * datetime fields are numeric classes; text, raw TYPE_NULL, and unknown stay on the home layer.
 */
fun prefersNumericLayer(inputType: Int): Boolean =
    when (inputType and InputType.TYPE_MASK_CLASS) {
        InputType.TYPE_CLASS_NUMBER,
        InputType.TYPE_CLASS_PHONE,
        InputType.TYPE_CLASS_DATETIME,
        -> true
        else -> false
    }
