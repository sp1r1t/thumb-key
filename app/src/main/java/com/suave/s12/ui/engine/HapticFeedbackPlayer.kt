package com.suave.s12.ui.engine

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import com.suave.s12.db.AppSettings
import com.suave.s12.engine.feedback.FeedbackSettings
import com.suave.s12.engine.feedback.HapticChannel
import com.suave.s12.engine.feedback.HapticPattern
import com.suave.s12.engine.feedback.HapticPlayer
import com.suave.s12.engine.feedback.HapticType
import com.suave.s12.engine.feedback.hapticTypeFromDb
import com.suave.s12.utils.toBool

/**
 * Fires [View.performHapticFeedback] with [HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING]
 * so a channel the user turned on is not silently dropped by the view's haptic flag.
 *
 * Types map to platform constants. Confirm/Reject need API 30 and fall back to keyboard tap
 * on older devices. Strength still comes from the OS/OEM profile for that constant, not from
 * a duration/amplitude we control: raw [android.os.VibrationEffect] never matched keyboard-tap
 * on Samsung OneUI when this was last measured.
 */
class HapticFeedbackPlayer(
    private val view: View,
) : HapticPlayer {
    override fun play(pattern: HapticPattern) {
        view.playHaptic(pattern.type)
    }
}

fun View.playHaptic(type: HapticType) {
    performHapticFeedback(type.feedbackConstant(), HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
}

fun HapticType.feedbackConstant(): Int =
    when (this) {
        HapticType.KEYBOARD_TAP -> HapticFeedbackConstants.KEYBOARD_TAP
        HapticType.CLOCK_TICK -> HapticFeedbackConstants.CLOCK_TICK
        HapticType.CONTEXT_CLICK -> HapticFeedbackConstants.CONTEXT_CLICK
        HapticType.VIRTUAL_KEY -> HapticFeedbackConstants.VIRTUAL_KEY
        HapticType.LONG_PRESS -> HapticFeedbackConstants.LONG_PRESS
        HapticType.TEXT_HANDLE_MOVE -> HapticFeedbackConstants.TEXT_HANDLE_MOVE
        HapticType.CONFIRM ->
            if (Build.VERSION.SDK_INT >= 30) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
        HapticType.REJECT ->
            if (Build.VERSION.SDK_INT >= 30) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
    }

fun AppSettings.toFeedbackSettings(): FeedbackSettings =
    FeedbackSettings(
        tap = HapticChannel(vibrateOnTap.toBool(), hapticTypeFromDb(vibrateTapType)),
        swipe = HapticChannel(vibrateOnSwipe.toBool(), hapticTypeFromDb(vibrateSwipeType)),
        slide = HapticChannel(vibrateOnSlide.toBool(), hapticTypeFromDb(vibrateSlideType)),
        repeat = HapticChannel(vibrateOnHoldRepeat.toBool(), hapticTypeFromDb(vibrateHoldRepeatType)),
        modifier = HapticChannel(vibrateOnModifier.toBool(), hapticTypeFromDb(vibrateModifierType)),
    )
