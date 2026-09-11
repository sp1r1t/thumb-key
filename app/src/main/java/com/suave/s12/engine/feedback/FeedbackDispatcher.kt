package com.suave.s12.engine.feedback

/** One haptic buzz: how long, how strong. */
data class HapticPattern(
    val durationMs: Long,
    val amplitude: Int,
)

/**
 * Minimal Phase 1 feedback settings - not a port of the old app's full settings surface, just
 * enough to drive [FeedbackDispatcher]. [tapVibrationEnabled] and [slideVibrationEnabled] are
 * kept independent, matching the pre-rewrite app's two separate settings ("Vibrate on tap" vs.
 * "Vibrate for slide gestures", the latter scoped specifically to the continuous spacebar/
 * backspace slide) - [FeedbackEvent.SlideStep] is gated by [slideVibrationEnabled], every other
 * event by [tapVibrationEnabled]. [baseAmplitude] is 1-255 (Android's `VibrationEffect`
 * amplitude range).
 */
data class FeedbackSettings(
    val tapVibrationEnabled: Boolean,
    val slideVibrationEnabled: Boolean,
    val baseDurationMs: Long,
    val baseAmplitude: Int,
)

/** Plays a [HapticPattern]. Implemented by the UI layer (Step 5) over the real Vibrator/View
 *  haptic APIs - kept as an interface here so the dispatch logic below stays testable without
 *  Android. */
fun interface HapticPlayer {
    fun play(pattern: HapticPattern)
}

object FeedbackDispatcher {
    fun dispatch(
        event: FeedbackEvent,
        settings: FeedbackSettings,
        player: HapticPlayer,
    ) {
        val enabled = if (event is FeedbackEvent.SlideStep) settings.slideVibrationEnabled else settings.tapVibrationEnabled
        if (!enabled) return
        player.play(patternFor(event, settings))
    }

    private fun patternFor(
        event: FeedbackEvent,
        settings: FeedbackSettings,
    ): HapticPattern =
        when (event) {
            // A held/locked modifier engaging gets a longer, deliberate buzz - distinct from a
            // plain tap - so activating Ctrl/caps-lock is felt, not just seen.
            is FeedbackEvent.ModifierActivated -> {
                HapticPattern(settings.baseDurationMs * 2, settings.baseAmplitude)
            }

            is FeedbackEvent.ModifierDeactivated -> {
                HapticPattern((settings.baseDurationMs * 3) / 2, (settings.baseAmplitude * 7) / 10)
            }

            // Repeat ticks and slide steps fire often while held - a full-strength buzz on each
            // one is fatiguing, so they're quieter than a single deliberate tap/swipe.
            FeedbackEvent.RepeatTick -> {
                HapticPattern((settings.baseDurationMs / 2).coerceAtLeast(1), (settings.baseAmplitude / 2).coerceAtLeast(1))
            }

            FeedbackEvent.SlideStep -> {
                HapticPattern((settings.baseDurationMs / 2).coerceAtLeast(1), (settings.baseAmplitude / 2).coerceAtLeast(1))
            }

            FeedbackEvent.TapRecognized, is FeedbackEvent.SwipeLocked -> {
                HapticPattern(settings.baseDurationMs, settings.baseAmplitude)
            }
        }
}
