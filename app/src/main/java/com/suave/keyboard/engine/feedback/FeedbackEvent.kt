package com.suave.keyboard.engine.feedback

import com.suave.keyboard.engine.gesture.Direction
import com.suave.keyboard.engine.intent.ModifierId
import com.suave.keyboard.engine.modifier.ActivationMode

/**
 * A semantic event worth giving the user tactile feedback about. Haptics are an information
 * channel here, not decoration - gesture recognition and the modifier engine emit these, a
 * single [FeedbackDispatcher] decides what they feel like, so the vocabulary can grow (distinct
 * feedback per gesture class, one-shot vs. locked modifier feedback, threshold/cancellation
 * feedback) without any of the emitting code needing to know what "feels like a Tab" means.
 */
sealed class FeedbackEvent {
    object TapRecognized : FeedbackEvent()

    data class SwipeLocked(
        val direction: Direction,
    ) : FeedbackEvent()

    data class ModifierActivated(
        val modifier: ModifierId,
        val mode: ActivationMode,
    ) : FeedbackEvent()

    data class ModifierDeactivated(
        val modifier: ModifierId,
    ) : FeedbackEvent()

    object RepeatTick : FeedbackEvent()

    object SlideStep : FeedbackEvent()
}
