package com.suave.s12.ui.engine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suave.s12.BuildConfig
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            // baseDurationMs/baseAmplitude are currently inert - HapticFeedbackPlayer's
            // underlying primitive doesn't expose either (see its doc for why) - kept so this
            // doesn't need touching if a real second lever ever turns up.
            FeedbackSettings(
                tapVibrationEnabled = vibrateOnTap,
                slideVibrationEnabled = vibrateOnSlide,
                baseDurationMs = 25L,
                baseAmplitude = 130,
            )
        }
    val hapticPlayer = remember(view) { HapticFeedbackPlayer(view) }
    // Resolved once per IME session (onStartInput recreates this whole screen on every new
    // input focus), matching how the old engine treated editor capability too.
    val capabilities = remember { EditorCapabilityResolver.resolve(ime.currentInputEditorInfo) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (!ignoreBottomPadding) Modifier.safeDrawingPadding() else Modifier),
    ) {
        if (BuildConfig.DEBUG) {
            // Exists to make "which build is actually on the phone" a glance rather than an adb
            // round-trip. Reads the APK's actual install timestamp from PackageManager at
            // runtime rather than baking a timestamp in at Gradle configuration time - this
            // project's Gradle configuration cache gets reused whenever only source files
            // change, which skips re-running the build script (and any Date() call in it)
            // entirely, so a config-time timestamp went stale exactly when it mattered most:
            // confirming a fresh `adb install` actually took effect. PackageManager's
            // lastUpdateTime always reflects the real install, regardless of Gradle caching.
            // Debug builds only; never shows in a release build.
            val installTime =
                remember {
                    val info = ime.packageManager.getPackageInfo(ime.packageName, 0)
                    SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(info.lastUpdateTime))
                }
            val targetApp = ime.currentInputEditorInfo?.packageName ?: "?"
            Text(
                text = "installed $installTime | $targetApp (${capabilities.level})",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error)
                        .padding(vertical = 2.dp),
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onError,
            )
        }
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
