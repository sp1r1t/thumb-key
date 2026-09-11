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
import com.suave.s12.db.DEFAULT_ALT_AS_MODIFIER
import com.suave.s12.db.DEFAULT_CTRL_AS_MODIFIER
import com.suave.s12.db.DEFAULT_ESC_AS_MODIFIER
import com.suave.s12.db.DEFAULT_HIDE_LETTERS
import com.suave.s12.db.DEFAULT_IGNORE_BOTTOM_PADDING
import com.suave.s12.db.DEFAULT_KEY_HEIGHT
import com.suave.s12.db.DEFAULT_MIN_SWIPE_LENGTH
import com.suave.s12.db.DEFAULT_SHIFT_AS_MODIFIER
import com.suave.s12.db.DEFAULT_VIBRATE_ON_SLIDE
import com.suave.s12.db.DEFAULT_VIBRATE_ON_TAP
import com.suave.s12.engine.capability.EditorCapabilityResolver
import com.suave.s12.engine.feedback.FeedbackDispatcher
import com.suave.s12.engine.feedback.FeedbackSettings
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.engine.intent.layoutRows
import com.suave.s12.engine.modifier.ModifierState
import com.suave.s12.engine.modifier.modifierBehaviors
import com.suave.s12.engine.output.OutputExecutor
import com.suave.s12.layout.BuiltinLayouts
import com.suave.s12.utils.KeyboardPosition
import com.suave.s12.utils.toBool
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders the selected [com.suave.s12.layout.NamedLayout] on the new engine end to end. Owns
 * the one piece of state every key on the keyboard shares - [ModifierState] - since modifiers
 * are no longer scoped to layout rendering (see `engine/modifier`). A single instance of this
 * state, read and written by whichever key's gesture touches it, is what makes
 * Ctrl/Alt/Esc/Shift correct across the whole keyboard without needing to swap what any other
 * key renders - there is no `mode` enum here at all, deliberately.
 *
 * The grid is derived from the layout data ([layoutRows]), not a hardcoded 4x5. Suave is one
 * [com.suave.s12.layout.BuiltinLayouts] entry; switching [AppSettings.keyboardLayout] selects
 * another.
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
    val namedLayout = BuiltinLayouts.byIndex(settings?.keyboardLayout ?: 0)
    val behaviors =
        remember(
            settings?.ctrlAsModifier,
            settings?.altAsModifier,
            settings?.shiftAsModifier,
            settings?.escAsModifier,
        ) {
            modifierBehaviors(
                mapOf(
                    ModifierId.CTRL to (settings?.ctrlAsModifier ?: DEFAULT_CTRL_AS_MODIFIER).toBool(),
                    ModifierId.ALT to (settings?.altAsModifier ?: DEFAULT_ALT_AS_MODIFIER).toBool(),
                    ModifierId.SHIFT to (settings?.shiftAsModifier ?: DEFAULT_SHIFT_AS_MODIFIER).toBool(),
                    ModifierId.ESC to (settings?.escAsModifier ?: DEFAULT_ESC_AS_MODIFIER).toBool(),
                ),
            )
        }
    // Unlike the old engine, key width here is always auto-fit (Modifier.weight(1f)) - there's
    // no manual-width/square-vs-non-square distinction to gate this behind, so keyHeight always
    // applies directly as each row's height.
    val keyHeight = (settings?.keyHeight ?: DEFAULT_KEY_HEIGHT).dp

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
            // Shows the APK's actual install timestamp (read from PackageManager at runtime,
            // not baked in at Gradle configuration time - this project's Gradle configuration
            // cache gets reused whenever only source files change, which skips re-running the
            // build script and any Date() call in it, so a config-time timestamp went stale
            // exactly when it mattered most: confirming a fresh `adb install` actually took
            // effect), plus which app the IME thinks it's connected to and how its editor was
            // classified. Debug builds only.
            val installTime =
                remember {
                    val info = ime.packageManager.getPackageInfo(ime.packageName, 0)
                    SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(info.lastUpdateTime))
                }
            val targetApp = ime.currentInputEditorInfo?.packageName ?: "?"
            Text(
                text = "$installTime | $targetApp (${capabilities.level})",
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
        for (row in layoutRows(namedLayout.layout)) {
            Row(modifier = Modifier.fillMaxWidth().height(keyHeight)) {
                for (position in row) {
                    val mapping = namedLayout.layout[position] ?: continue
                    EngineKeyboardKey(
                        mapping = mapping,
                        modifierState = modifierState,
                        onModifierStateChange = { modifierState = it },
                        onExecute = { action -> OutputExecutor.execute(action, capabilities, ime.currentInputConnection) },
                        onLegacyAction = { action ->
                            dispatchLegacyAction(
                                action = action,
                                ime = ime,
                                capabilities = capabilities,
                                onToggleHideLetters = onToggleHideLetters,
                                onToggleEmojiMode = onToggleEmojiMode,
                                onToggleNumericMode = onToggleNumericMode,
                                onSwitchLanguage = onSwitchLanguage,
                                onChangePosition = onChangePosition,
                            )
                        },
                        onFeedback = { event -> FeedbackDispatcher.dispatch(event, feedbackSettings, hapticPlayer) },
                        shiftMappings = namedLayout.shiftMappings,
                        minSwipeDistancePx = minSwipeDistancePx,
                        hideLetters = hideLetters,
                        modifierBehaviors = behaviors,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}
