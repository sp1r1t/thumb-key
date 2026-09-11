package com.suave.s12.ui.engine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.suave.s12.IMEService
import com.suave.s12.db.AppSettings
import com.suave.s12.db.DEFAULT_HIDE_LETTERS
import com.suave.s12.db.DEFAULT_IGNORE_BOTTOM_PADDING
import com.suave.s12.db.DEFAULT_MIN_SWIPE_LENGTH
import com.suave.s12.db.DEFAULT_VIBRATE_ON_SLIDE
import com.suave.s12.db.DEFAULT_VIBRATE_ON_TAP
import com.suave.s12.engine.capability.EditorCapabilityResolver
import com.suave.s12.engine.feedback.FeedbackDispatcher
import com.suave.s12.engine.feedback.FeedbackSettings
import com.suave.s12.engine.intent.KeyPosition
import com.suave.s12.engine.modifier.ModifierState
import com.suave.s12.engine.output.OutputExecutor
import com.suave.s12.layout.SUAVE_LAYOUT
import com.suave.s12.layout.SUAVE_SHIFT_MAPPINGS
import com.suave.s12.utils.KeyboardPosition
import com.suave.s12.utils.toBool

/**
 * Renders [SUAVE_LAYOUT] on the new engine end to end. Owns the one piece of state every key on
 * the keyboard shares - [ModifierState] - since modifiers are no longer scoped to layout
 * rendering (see `engine/modifier`). A single instance of this state, read and written by
 * whichever key's gesture touches it, is what makes Ctrl/Alt/Esc/Shift correct across the whole
 * keyboard without needing to swap what any other key renders - there is no `mode` enum here at
 * all, deliberately.
 *
 * Phase 1 scope, deliberately not attempted here: the "Dual" split-both-hands position mode
 * (this always renders a single instance regardless of the position setting), ENTER's
 * double-width sizing, emoji/numeric-mode's own screens (those keys bridge to the old KeyAction
 * pipeline and fire correctly, but have no new screen to switch to yet), and per-key icons (see
 * [EngineKeyboardKey]'s plain-glyph labels).
 */
@Composable
fun EngineKeyboardScreen(
    settings: AppSettings?,
    onToggleHideLetters: () -> Unit,
    onToggleEmojiMode: (enable: Boolean) -> Unit,
    onToggleNumericMode: (enable: Boolean) -> Unit,
    onSwitchLanguage: () -> Unit,
    onChangePosition: ((old: KeyboardPosition) -> KeyboardPosition) -> Unit,
) {
    val ctx = LocalContext.current
    val ime = ctx as IMEService
    val view = LocalView.current

    var modifierState by remember { mutableStateOf(ModifierState()) }

    val vibrateOnTap = (settings?.vibrateOnTap ?: DEFAULT_VIBRATE_ON_TAP).toBool()
    val vibrateOnSlide = (settings?.vibrateOnSlide ?: DEFAULT_VIBRATE_ON_SLIDE).toBool()
    val hideLetters = (settings?.hideLetters ?: DEFAULT_HIDE_LETTERS).toBool()
    val minSwipeDistancePx = (settings?.minSwipeLength ?: DEFAULT_MIN_SWIPE_LENGTH).toFloat()
    val ignoreBottomPadding = (settings?.ignoreBottomPadding ?: DEFAULT_IGNORE_BOTTOM_PADDING).toBool()

    val feedbackSettings =
        remember(vibrateOnTap, vibrateOnSlide) {
            // No per-user duration/amplitude setting exists on this branch (that's the separate
            // haptics-settings branch) - these are reasonable fixed defaults for Phase 1.
            FeedbackSettings(vibrationEnabled = vibrateOnTap || vibrateOnSlide, baseDurationMs = 20L, baseAmplitude = 40)
        }
    val hapticPlayer = remember(view) { ViewHapticPlayer(view) }
    // Resolved once per IME session (onStartInput recreates this whole screen on every new
    // input focus), matching how the old engine treated editor capability too.
    val capabilities = remember { EditorCapabilityResolver.resolve(ime.currentInputEditorInfo) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (!ignoreBottomPadding) Modifier.safeDrawingPadding() else Modifier),
    ) {
        for (row in 0..3) {
            val columns = if (row == 3) 0..3 else 0..4
            Row(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                for (col in columns) {
                    val mapping = SUAVE_LAYOUT[KeyPosition(row, col)] ?: continue
                    EngineKeyboardKey(
                        mapping = mapping,
                        modifierState = modifierState,
                        onModifierStateChange = { modifierState = it },
                        onExecute = { action -> OutputExecutor.execute(action, capabilities, ime.currentInputConnection) },
                        onLegacyAction = { action ->
                            dispatchLegacyAction(
                                action = action,
                                ime = ime,
                                onToggleHideLetters = onToggleHideLetters,
                                onToggleEmojiMode = onToggleEmojiMode,
                                onToggleNumericMode = onToggleNumericMode,
                                onSwitchLanguage = onSwitchLanguage,
                                onChangePosition = onChangePosition,
                            )
                        },
                        onFeedback = { event -> FeedbackDispatcher.dispatch(event, feedbackSettings, hapticPlayer) },
                        shiftMappings = SUAVE_SHIFT_MAPPINGS,
                        minSwipeDistancePx = minSwipeDistancePx,
                        hideLetters = hideLetters,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}
