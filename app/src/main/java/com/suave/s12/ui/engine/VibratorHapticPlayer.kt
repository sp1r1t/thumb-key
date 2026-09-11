package com.suave.s12.ui.engine

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.suave.s12.engine.feedback.HapticPattern
import com.suave.s12.engine.feedback.HapticPlayer

/**
 * Plays real, duration/amplitude-controlled vibration via the platform [Vibrator], rather than
 * Android's fixed `HapticFeedbackConstants` (the first attempt at this, `View.
 * performHapticFeedback`). Two real problems with that approach, not just weak-feeling
 * constants: some devices/OEM haptic drivers make the weaker constants outright silent even
 * with the system "touch feedback" setting on, AND `performHapticFeedback` is known to silently
 * throttle/coalesce calls made in rapid succession - which is exactly what a fast slide gesture
 * does (many [com.suave.s12.engine.gesture.Gesture.SlideStep] events per second). An isolated
 * tap got through; a rapid slide mostly didn't. `Vibrator.vibrate()` has no such debounce.
 *
 * Requires the VIBRATE permission (normal, granted at install - see AndroidManifest.xml).
 */
class VibratorHapticPlayer(
    context: Context,
) : HapticPlayer {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    override fun play(pattern: HapticPattern) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        val durationMs = pattern.durationMs.coerceAtLeast(1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(durationMs, pattern.amplitude.coerceIn(1, 255)))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(durationMs)
        }
    }
}
