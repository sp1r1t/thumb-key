package com.suave.s12.engine.feedback

import androidx.annotation.StringRes
import com.suave.s12.R

/**
 * Named [android.view.HapticFeedbackConstants] flavors the user can assign to a vibration
 * channel. Labels are how they feel (Soft, Tactile, Strong), not the platform constant names.
 * Ordinals are stored in settings, so new values append only.
 */
enum class HapticType(
    @param:StringRes val resId: Int,
) {
    KEYBOARD_TAP(R.string.haptic_type_keyboard_tap),
    CLOCK_TICK(R.string.haptic_type_clock_tick),
    CONTEXT_CLICK(R.string.haptic_type_context_click),
    VIRTUAL_KEY(R.string.haptic_type_virtual_key),
    LONG_PRESS(R.string.haptic_type_long_press),
    TEXT_HANDLE_MOVE(R.string.haptic_type_text_handle_move),
    CONFIRM(R.string.haptic_type_confirm),
    REJECT(R.string.haptic_type_reject),
}

/** Lightest to heaviest, then the two characterful confirm/reject buzzes. */
val HAPTIC_TYPE_BY_FEEL: List<HapticType> =
    listOf(
        HapticType.CLOCK_TICK,
        HapticType.TEXT_HANDLE_MOVE,
        HapticType.KEYBOARD_TAP,
        HapticType.CONTEXT_CLICK,
        HapticType.VIRTUAL_KEY,
        HapticType.LONG_PRESS,
        HapticType.CONFIRM,
        HapticType.REJECT,
    )

fun hapticTypeFromDb(ordinal: Int): HapticType =
    HapticType.entries.getOrElse(ordinal) { HapticType.KEYBOARD_TAP }
