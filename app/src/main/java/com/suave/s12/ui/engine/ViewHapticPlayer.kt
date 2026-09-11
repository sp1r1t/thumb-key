package com.suave.s12.ui.engine

import android.view.HapticFeedbackConstants
import android.view.View
import com.suave.s12.engine.feedback.HapticPattern
import com.suave.s12.engine.feedback.HapticPlayer

/**
 * Adapts the engine's duration/amplitude-based [HapticPattern] onto Android's fixed
 * [HapticFeedbackConstants] vocabulary. This branch doesn't request the VIBRATE permission or
 * build custom `VibrationEffect`s (that's the haptics-settings work on a different branch) - a
 * stronger/longer pattern just maps to a stronger built-in constant instead.
 */
class ViewHapticPlayer(
    private val view: View,
) : HapticPlayer {
    override fun play(pattern: HapticPattern) {
        val constant =
            when {
                pattern.durationMs >= 40 -> HapticFeedbackConstants.LONG_PRESS
                pattern.durationMs >= 15 -> HapticFeedbackConstants.KEYBOARD_TAP
                else -> HapticFeedbackConstants.CLOCK_TICK
            }
        view.performHapticFeedback(constant)
    }
}
