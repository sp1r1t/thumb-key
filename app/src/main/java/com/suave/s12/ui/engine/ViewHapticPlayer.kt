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
 *
 * Deliberately only uses two constants, not a finer-grained ladder: [HapticFeedbackConstants]
 * that map to very short/weak system vibrations (e.g. `CLOCK_TICK`) are inaudible or outright
 * silent on some devices/OEM haptic drivers, even with the same "touch feedback" system setting
 * that makes [HapticFeedbackConstants.KEYBOARD_TAP] work fine - so every pattern maps to a
 * constant known to actually produce a felt buzz, at the cost of losing some of the intensity
 * differentiation [FeedbackDispatcher] otherwise encodes (repeat ticks and slide steps feel the
 * same as a tap for now, rather than distinctly quieter).
 */
class ViewHapticPlayer(
    private val view: View,
) : HapticPlayer {
    override fun play(pattern: HapticPattern) {
        val constant =
            if (pattern.durationMs >= 40) {
                HapticFeedbackConstants.LONG_PRESS
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
        view.performHapticFeedback(constant)
    }
}
