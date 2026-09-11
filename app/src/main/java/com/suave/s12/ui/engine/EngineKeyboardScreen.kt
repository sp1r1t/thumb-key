package com.suave.s12.ui.engine

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.suave.s12.db.DEFAULT_POSITION
import com.suave.s12.db.DEFAULT_SHIFT_AS_MODIFIER
import com.suave.s12.db.DEFAULT_VIBRATE_ON_SLIDE
import com.suave.s12.db.DEFAULT_VIBRATE_ON_TAP
import com.suave.s12.engine.action.SemanticAction
import com.suave.s12.engine.capability.EditorCapabilities
import com.suave.s12.engine.capability.EditorCapabilityResolver
import com.suave.s12.engine.feedback.FeedbackDispatcher
import com.suave.s12.engine.feedback.FeedbackEvent
import com.suave.s12.engine.feedback.FeedbackSettings
import com.suave.s12.engine.intent.Layout
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.engine.intent.layoutRows
import com.suave.s12.engine.modifier.ModifierBehavior
import com.suave.s12.engine.modifier.ModifierState
import com.suave.s12.engine.modifier.modifierBehaviors
import com.suave.s12.engine.output.OutputExecutor
import com.suave.s12.layout.BuiltinLayouts
import com.suave.s12.layout.DEFAULT_LAYER_HEIGHTS
import com.suave.s12.layout.LayerContent
import com.suave.s12.layout.LayoutLayer
import com.suave.s12.layout.NamedLayout
import com.suave.s12.layout.parseLayerHeightOverrides
import com.suave.s12.utils.KeyboardPosition
import com.suave.s12.utils.toBool
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders the selected [NamedLayout] on the new engine end to end. Owns the two pieces of
 * state every key on the keyboard shares: [ModifierState] (modifiers are not a layout mode)
 * and [LayoutLayer] (numeric/emoji are layout switches, not modifiers). Both survive Dual's
 * second copy of the grid, so Ctrl held on the left half still applies on the right.
 *
 * The grid is derived from the layout data ([layoutRows]), not a hardcoded 4x5. Suave is one
 * [BuiltinLayouts] entry; switching [AppSettings.keyboardLayout] selects another.
 * [AppSettings.position] Dual draws two copies that share modifier and layer state; Left, Right,
 * and Center are all full width until the layout has a real (narrower) key width to park.
 * Key width comes from [com.suave.s12.engine.intent.KeyMapping.columnSpan].
 */
@Composable
fun EngineKeyboardScreen(
    settings: AppSettings?,
    onToggleHideLetters: () -> Unit,
    onSwitchLanguage: () -> Unit,
    onChangePosition: ((old: KeyboardPosition) -> KeyboardPosition) -> Unit,
) {
    val ctx = LocalContext.current
    val ime = ctx as IMEService
    val view = LocalView.current

    var modifierState by remember { mutableStateOf(ModifierState()) }
    var layer by remember { mutableStateOf(LayoutLayer.MAIN) }

    val vibrateOnTap = (settings?.vibrateOnTap ?: DEFAULT_VIBRATE_ON_TAP).toBool()
    val vibrateOnSlide = (settings?.vibrateOnSlide ?: DEFAULT_VIBRATE_ON_SLIDE).toBool()
    val hideLetters = (settings?.hideLetters ?: DEFAULT_HIDE_LETTERS).toBool()
    val minSwipeDistancePx = (settings?.minSwipeLength ?: DEFAULT_MIN_SWIPE_LENGTH).toFloat()
    val ignoreBottomPadding = (settings?.ignoreBottomPadding ?: DEFAULT_IGNORE_BOTTOM_PADDING).toBool()
    val namedLayout = BuiltinLayouts.byIndex(settings?.keyboardLayout ?: 0)
    val keyboardPosition =
        KeyboardPosition.entries.getOrElse(settings?.position ?: DEFAULT_POSITION) { KeyboardPosition.Center }
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
    // Row height is always keyHeight. Horizontal size is each key's columnSpan as a Row
    // weight, so a span-2 Enter fills two letter-columns without a separate width setting.
    val keyHeight = (settings?.keyHeight ?: DEFAULT_KEY_HEIGHT).dp
    val layerHeightOverrides = parseLayerHeightOverrides(settings?.layerHeights ?: DEFAULT_LAYER_HEIGHTS)

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

    LaunchedEffect(namedLayout.id) { layer = LayoutLayer.MAIN }

    val appHost =
        AppCommandHost(
            onToggleHideLetters = onToggleHideLetters,
            onSwitchLanguage = {
                layer = LayoutLayer.MAIN
                onSwitchLanguage()
            },
            onChangePosition = onChangePosition,
            onSelectLayer = { requested ->
                layer =
                    when (requested) {
                        LayoutLayer.NUMERIC -> if (namedLayout.numericLayout != null) LayoutLayer.NUMERIC else layer
                        LayoutLayer.EMOJI -> if (namedLayout.emojiBottomRow != null) LayoutLayer.EMOJI else layer
                        LayoutLayer.MAIN -> LayoutLayer.MAIN
                    }
            },
            onToggleEmojiLayer = {
                layer =
                    when {
                        layer == LayoutLayer.EMOJI -> LayoutLayer.MAIN
                        namedLayout.emojiBottomRow != null -> LayoutLayer.EMOJI
                        else -> layer
                    }
            },
        )

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
        val renderPanel: @Composable (Modifier) -> Unit = { panelModifier ->
            EngineKeyboardPanel(
                modifier = panelModifier,
                namedLayout = namedLayout,
                layer = layer,
                keyHeight = keyHeight,
                layerHeightOverrides = layerHeightOverrides,
                modifierState = modifierState,
                onModifierStateChange = { modifierState = it },
                onExecute = { action ->
                    ActionExecutor.execute(
                        action = action,
                        capabilities = capabilities,
                        ime = ime,
                        host = appHost,
                    )
                },
                onFeedback = { event -> FeedbackDispatcher.dispatch(event, feedbackSettings, hapticPlayer) },
                minSwipeDistancePx = minSwipeDistancePx,
                hideLetters = hideLetters,
                modifierBehaviors = behaviors,
                vibrateOnTap = vibrateOnTap,
                capabilities = capabilities,
                ime = ime,
            )
        }
        if (keyboardPosition == KeyboardPosition.Dual) {
            Row(modifier = Modifier.fillMaxWidth()) {
                renderPanel(Modifier.weight(1f))
                renderPanel(Modifier.weight(1f))
            }
        } else {
            renderPanel(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun EngineKeyboardPanel(
    modifier: Modifier,
    namedLayout: NamedLayout,
    layer: LayoutLayer,
    keyHeight: Dp,
    layerHeightOverrides: Map<LayoutLayer, Int>,
    modifierState: ModifierState,
    onModifierStateChange: (ModifierState) -> Unit,
    onExecute: (SemanticAction) -> Unit,
    onFeedback: (FeedbackEvent) -> Unit,
    minSwipeDistancePx: Float,
    hideLetters: Boolean,
    modifierBehaviors: Map<ModifierId, ModifierBehavior>,
    vibrateOnTap: Boolean,
    capabilities: EditorCapabilities,
    ime: IMEService,
) {
    val grid = namedLayout.gridFor(layer)
    val overrideRows = layerHeightOverrides[layer] ?: 0
    val contentRows = namedLayout.contentRows(layer, overrideRows)
    Column(modifier = modifier) {
        if (contentRows > 0) {
            LayerContentSlot(
                content = namedLayout.contentFor(layer),
                height = keyHeight * contentRows,
                vibrateOnTap = vibrateOnTap,
                capabilities = capabilities,
                ime = ime,
            )
        }
        LayoutGrid(
            layout = grid,
            namedLayout = namedLayout,
            keyHeight = keyHeight,
            modifierState = modifierState,
            onModifierStateChange = onModifierStateChange,
            onExecute = onExecute,
            onFeedback = onFeedback,
            minSwipeDistancePx = minSwipeDistancePx,
            hideLetters = hideLetters,
            modifierBehaviors = modifierBehaviors,
        )
    }
}

@Composable
private fun LayerContentSlot(
    content: LayerContent,
    height: Dp,
    vibrateOnTap: Boolean,
    capabilities: EditorCapabilities,
    ime: IMEService,
) {
    val view = LocalView.current
    when (content) {
        LayerContent.None -> Spacer(modifier = Modifier.fillMaxWidth().height(height))
        LayerContent.EmojiPicker -> {
            val colorScheme = MaterialTheme.colorScheme
            val pickerText = colorScheme.onSurface.toArgb()
            val pickerIcon = colorScheme.onSurfaceVariant.toArgb()
            val pickerAccent = colorScheme.primary.toArgb()
            val darkKeyboard = colorScheme.surface.luminance() < 0.5f
            key(pickerText, pickerIcon, pickerAccent, darkKeyboard) {
                AndroidView(
                    factory = { context ->
                        createThemedEmojiPicker(
                            context = context,
                            darkKeyboard = darkKeyboard,
                            text = pickerText,
                            icon = pickerIcon,
                            accent = pickerAccent,
                        ).apply {
                            setOnEmojiPickedListener { picked ->
                                if (vibrateOnTap) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                OutputExecutor.execute(
                                    SemanticAction.TypeText(picked.emoji),
                                    capabilities,
                                    ime.currentInputConnection,
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(height),
                )
            }
        }
    }
}

@Composable
private fun LayoutGrid(
    layout: Layout,
    namedLayout: NamedLayout,
    keyHeight: Dp,
    modifierState: ModifierState,
    onModifierStateChange: (ModifierState) -> Unit,
    onExecute: (SemanticAction) -> Unit,
    onFeedback: (FeedbackEvent) -> Unit,
    minSwipeDistancePx: Float,
    hideLetters: Boolean,
    modifierBehaviors: Map<ModifierId, ModifierBehavior>,
) {
    for (row in layoutRows(layout)) {
        Row(modifier = Modifier.fillMaxWidth().height(keyHeight)) {
            for (position in row) {
                val mapping = layout[position] ?: continue
                EngineKeyboardKey(
                    mapping = mapping,
                    modifierState = modifierState,
                    onModifierStateChange = onModifierStateChange,
                    onExecute = onExecute,
                    onFeedback = onFeedback,
                    shiftMappings = namedLayout.shiftMappings,
                    minSwipeDistancePx = minSwipeDistancePx,
                    hideLetters = hideLetters,
                    modifierBehaviors = modifierBehaviors,
                    modifier = Modifier.weight(mapping.columnSpan.toFloat()).fillMaxHeight(),
                )
            }
        }
    }
}
