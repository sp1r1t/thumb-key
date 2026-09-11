package com.suave.s12.ui.engine

import android.view.HapticFeedbackConstants
import android.view.View
import com.suave.s12.engine.feedback.HapticPattern
import com.suave.s12.engine.feedback.HapticPlayer

/**
 * Fires [View.performHapticFeedback] with `KEYBOARD_TAP` - the one primitive that actually felt
 * strong on real hardware when this was tested on the `feat/haptics-and-gesture-fixes` branch
 * (see that branch's `vibrateDevice()` and its docstring): several attempts at replicating this
 * via raw `Vibrator`/`VibrationEffect` calls (amplitude control, predefined effects, multi-pulse
 * waveforms) never came close in strength, and *stacking or repeating* the call made it worse,
 * not better - Android (Samsung's OneUI in particular) throttles haptic feedback under rapid
 * repeated calls, so doubling the call rate just made that throttling kick in more often.
 * [HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING] matters too - without it this can be
 * silently suppressed under conditions the plain constant alone doesn't guard against.
 *
 * Deliberately a single call, every time, regardless of [HapticPattern] - the primitive doesn't
 * expose duration/amplitude, so [HapticPattern]'s fields are currently inert here (kept in the
 * engine's feedback model so call sites don't need touching if a real second lever ever turns
 * up, same reasoning `vibrateDevice()` documents for its own unused parameters).
 */
class HapticFeedbackPlayer(
    private val view: View,
) : HapticPlayer {
    override fun play(pattern: HapticPattern) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
    }
}
