package com.suave.s12.engine.feedback

/** One haptic buzz: which [HapticType] to fire. */
data class HapticPattern(
    val type: HapticType,
)

/** On/off plus a type for one interaction (tap, swipe, slide, repeat, modifier). */
data class HapticChannel(
    val enabled: Boolean,
    val type: HapticType,
)

/**
 * Per-event haptic assignment. Each [FeedbackEvent] maps to one channel so tap, swipe, slide,
 * hold-repeat, and modifiers can be switched and typed independently.
 */
data class FeedbackSettings(
    val tap: HapticChannel,
    val swipe: HapticChannel,
    val slide: HapticChannel,
    val repeat: HapticChannel,
    val modifier: HapticChannel,
)

fun FeedbackSettings.channelFor(event: FeedbackEvent): HapticChannel =
    when (event) {
        FeedbackEvent.TapRecognized -> tap
        is FeedbackEvent.SwipeLocked -> swipe
        FeedbackEvent.SlideStep -> slide
        FeedbackEvent.RepeatTick -> repeat
        is FeedbackEvent.ModifierActivated,
        is FeedbackEvent.ModifierDeactivated,
        -> modifier
    }

/** Plays a [HapticPattern]. Implemented by the UI layer over View haptic APIs. */
fun interface HapticPlayer {
    fun play(pattern: HapticPattern)
}

object FeedbackDispatcher {
    fun dispatch(
        event: FeedbackEvent,
        settings: FeedbackSettings,
        player: HapticPlayer,
    ) {
        val channel = settings.channelFor(event)
        if (!channel.enabled) return
        player.play(HapticPattern(channel.type))
    }
}
