package com.suave.s12.ui.engine

import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.lifecycle.MutableLiveData
import com.suave.s12.IMEService
import com.suave.s12.MainActivity
import com.suave.s12.db.AppSettings
import com.suave.s12.db.ClipboardItem
import com.suave.s12.db.ClipboardRepository
import com.suave.s12.db.DEFAULT_ALT_AS_MODIFIER
import com.suave.s12.db.DEFAULT_ANIMATION_LETTER_DROP
import com.suave.s12.db.DEFAULT_ANIMATION_PRESS_HIGHLIGHT
import com.suave.s12.db.DEFAULT_ANIMATION_RELEASE_FLASH
import com.suave.s12.db.DEFAULT_BACKDROP_ENABLED
import com.suave.s12.db.DEFAULT_CLIPBOARD_HISTORY_ENABLED
import com.suave.s12.db.DEFAULT_CTRL_AS_MODIFIER
import com.suave.s12.db.DEFAULT_ESC_AS_MODIFIER
import com.suave.s12.db.DEFAULT_HIDE_EDITING
import com.suave.s12.db.DEFAULT_HIDE_LAYER_SWITCHES
import com.suave.s12.db.DEFAULT_HIDE_LETTERS
import com.suave.s12.db.DEFAULT_HIDE_MODIFIERS
import com.suave.s12.db.DEFAULT_HIDE_NAVIGATION
import com.suave.s12.db.DEFAULT_HIDE_NUMBERS
import com.suave.s12.db.DEFAULT_HIDE_SPECIALS
import com.suave.s12.db.DEFAULT_HIDE_SYMBOLS
import com.suave.s12.db.DEFAULT_IGNORE_BOTTOM_PADDING
import com.suave.s12.db.DEFAULT_KEY_BORDER_WIDTH
import com.suave.s12.db.DEFAULT_KEY_HEIGHT
import com.suave.s12.db.DEFAULT_KEY_PADDING
import com.suave.s12.db.DEFAULT_KEY_PADDING_VERTICAL
import com.suave.s12.db.DEFAULT_KEY_RADIUS
import com.suave.s12.db.DEFAULT_MIN_SWIPE_LENGTH
import com.suave.s12.db.DEFAULT_POSITION
import com.suave.s12.db.DEFAULT_PUSHUP_SIZE
import com.suave.s12.db.DEFAULT_SHIFT_AS_MODIFIER
import com.suave.s12.db.DEFAULT_SHOW_DEBUG_BAR
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
import com.suave.s12.ui.components.keyboard.ClipboardHistoryScreen
import com.suave.s12.utils.KeyboardPosition
import com.suave.s12.utils.isPasswordField
import com.suave.s12.utils.toBool
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders the selected [NamedLayout] on the new engine end to end. Owns the two pieces of
 * state every key on the keyboard shares: [ModifierState] (modifiers are not a layout mode)
 * and [LayoutLayer] (numeric/emoji/clipboard are layout switches, not modifiers). Both survive Dual's
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
    clipboardRepository: ClipboardRepository? = null,
) {
    val ctx = LocalContext.current
    val ime = ctx as IMEService
    val view = LocalView.current

    val modifierState = remember { mutableStateOf(ModifierState()) }
    var layer by remember { mutableStateOf(LayoutLayer.MAIN) }
    var clipboardOrigin by remember { mutableStateOf(LayoutLayer.MAIN) }
    val clipboardScope = rememberCoroutineScope()
    val emptyClipboardItems = remember { MutableLiveData(emptyList<ClipboardItem>()) }
    val clipboardItems by
        (clipboardRepository?.allClipboardItems ?: emptyClipboardItems).observeAsState(emptyList())

    val vibrateOnTap = (settings?.vibrateOnTap ?: DEFAULT_VIBRATE_ON_TAP).toBool()
    val vibrateOnSlide = (settings?.vibrateOnSlide ?: DEFAULT_VIBRATE_ON_SLIDE).toBool()
    val legendVisibility =
        LegendVisibility(
            hideLetters = (settings?.hideLetters ?: DEFAULT_HIDE_LETTERS).toBool(),
            hideSymbols = (settings?.hideSymbols ?: DEFAULT_HIDE_SYMBOLS).toBool(),
            hideNumbers = (settings?.hideNumbers ?: DEFAULT_HIDE_NUMBERS).toBool(),
            hideModifiers = (settings?.hideModifiers ?: DEFAULT_HIDE_MODIFIERS).toBool(),
            hideLayerSwitches = (settings?.hideLayerSwitches ?: DEFAULT_HIDE_LAYER_SWITCHES).toBool(),
            hideSpecials = (settings?.hideSpecials ?: DEFAULT_HIDE_SPECIALS).toBool(),
            hideNavigation = (settings?.hideNavigation ?: DEFAULT_HIDE_NAVIGATION).toBool(),
            hideEditing = (settings?.hideEditing ?: DEFAULT_HIDE_EDITING).toBool(),
        )
    val minSwipeDistancePx = (settings?.minSwipeLength ?: DEFAULT_MIN_SWIPE_LENGTH).toFloat()
    val ignoreBottomPadding = (settings?.ignoreBottomPadding ?: DEFAULT_IGNORE_BOTTOM_PADDING).toBool()
    val showDebugBar = (settings?.showDebugBar ?: DEFAULT_SHOW_DEBUG_BAR).toBool()
    val backdropEnabled = (settings?.backdropEnabled ?: DEFAULT_BACKDROP_ENABLED).toBool()
    val keyPadding = settings?.keyPadding ?: DEFAULT_KEY_PADDING
    val keyPaddingVertical = settings?.keyPaddingVertical ?: DEFAULT_KEY_PADDING_VERTICAL
    val keyBorderWidthDp = (settings?.keyBorderWidth ?: DEFAULT_KEY_BORDER_WIDTH) / 10f
    val keyRadiusPercent = settings?.keyRadius ?: DEFAULT_KEY_RADIUS
    val pushupSize = (settings?.pushupSize ?: DEFAULT_PUSHUP_SIZE).dp
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
    val keyCornerRadius = keyHeight * (keyRadiusPercent / 200f)
    val layerHeightOverrides = parseLayerHeightOverrides(settings?.layerHeights ?: DEFAULT_LAYER_HEIGHTS)
    val animations =
        KeyAnimationSettings(
            pressHighlight = (settings?.animationPressHighlight ?: DEFAULT_ANIMATION_PRESS_HIGHLIGHT).toBool(),
            releaseFlash = (settings?.animationReleaseFlash ?: DEFAULT_ANIMATION_RELEASE_FLASH).toBool(),
            letterDrop = (settings?.animationLetterDrop ?: DEFAULT_ANIMATION_LETTER_DROP).toBool(),
        )
    val passwordField = remember { isPasswordField(ime) }

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

    LaunchedEffect(namedLayout.id) {
        layer = LayoutLayer.MAIN
        clipboardOrigin = LayoutLayer.MAIN
    }
    LaunchedEffect(layer) {
        if (layer == LayoutLayer.CLIPBOARD) {
            clipboardRepository?.clearExpired()
        }
    }

    val clipboardHistoryEnabled =
        (settings?.clipboardHistoryEnabled ?: DEFAULT_CLIPBOARD_HISTORY_ENABLED).toBool()
    val clipboardSession =
        ClipboardLayerSession(
            items = clipboardItems,
            enabled = clipboardHistoryEnabled && clipboardRepository != null,
            onPasteAndLeave = { item ->
                ime.currentInputConnection?.commitText(item.text, 1)
                layer = clipboardOrigin
            },
            onPasteAndStay = { item ->
                ime.currentInputConnection?.commitText(item.text, 1)
            },
            onDelete = { item ->
                clipboardScope.launch { clipboardRepository?.deleteItem(item) }
            },
            onPin = { item ->
                clipboardScope.launch { clipboardRepository?.togglePin(item) }
            },
            onBack = { layer = clipboardOrigin },
            onClearAll = {
                clipboardScope.launch { clipboardRepository?.clearUnpinned() }
            },
            onOpenSettings = {
                val intent = Intent(ime, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra("startRoute", "clipboardSettings")
                ime.startActivity(intent)
            },
        )

    val onToggleHideLettersState = rememberUpdatedState(onToggleHideLetters)
    val onSwitchLanguageState = rememberUpdatedState(onSwitchLanguage)
    val onChangePositionState = rememberUpdatedState(onChangePosition)
    val namedLayoutState = rememberUpdatedState(namedLayout)
    val appHost =
        remember {
            AppCommandHost(
                onToggleHideLetters = { onToggleHideLettersState.value() },
                onSwitchLanguage = {
                    layer = LayoutLayer.MAIN
                    clipboardOrigin = LayoutLayer.MAIN
                    onSwitchLanguageState.value()
                },
                onChangePosition = { f -> onChangePositionState.value(f) },
                onSelectLayer = { requested ->
                    val current = namedLayoutState.value
                    layer =
                        when (requested) {
                            LayoutLayer.NUMERIC -> if (current.numericLayout != null) LayoutLayer.NUMERIC else layer
                            LayoutLayer.EMOJI -> if (current.emojiBottomRow != null) LayoutLayer.EMOJI else layer
                            LayoutLayer.MAIN -> LayoutLayer.MAIN
                            LayoutLayer.CLIPBOARD ->
                                if (current.layerContent[LayoutLayer.CLIPBOARD] != null) {
                                    if (layer != LayoutLayer.CLIPBOARD) {
                                        clipboardOrigin =
                                            if (layer == LayoutLayer.NUMERIC) {
                                                LayoutLayer.NUMERIC
                                            } else {
                                                LayoutLayer.MAIN
                                            }
                                    }
                                    LayoutLayer.CLIPBOARD
                                } else {
                                    layer
                                }
                        }
                },
                onToggleEmojiLayer = {
                    val current = namedLayoutState.value
                    layer =
                        when {
                            layer == LayoutLayer.EMOJI -> LayoutLayer.MAIN
                            current.emojiBottomRow != null -> LayoutLayer.EMOJI
                            else -> layer
                        }
                },
                onToggleClipboardHistory = {
                    val current = namedLayoutState.value
                    if (layer == LayoutLayer.CLIPBOARD) {
                        layer = clipboardOrigin
                    } else if (current.layerContent[LayoutLayer.CLIPBOARD] != null) {
                        clipboardOrigin =
                            if (layer == LayoutLayer.NUMERIC) LayoutLayer.NUMERIC else LayoutLayer.MAIN
                        layer = LayoutLayer.CLIPBOARD
                    }
                },
            )
        }
        val onExecute =
            remember(capabilities, ime, appHost) {
                { action: SemanticAction ->
                    ActionExecutor.execute(
                        action = action,
                        capabilities = capabilities,
                        ime = ime,
                        host = appHost,
                    )
                }
            }
        val onFeedback =
            remember(feedbackSettings, hapticPlayer) {
                { event: FeedbackEvent -> FeedbackDispatcher.dispatch(event, feedbackSettings, hapticPlayer) }
            }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showDebugBar) {
            // Shows the APK's actual install timestamp (read from PackageManager at runtime,
            // not baked in at Gradle configuration time - this project's Gradle configuration
            // cache gets reused whenever only source files change, which skips re-running the
            // build script and any Date() call in it, so a config-time timestamp went stale
            // exactly when it mattered most: confirming a fresh `adb install` actually took
            // effect), plus which app the IME thinks it's connected to and how its editor was
            // classified.
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
                clipboardOrigin = clipboardOrigin,
                clipboardSession = clipboardSession,
                keyHeight = keyHeight,
                layerHeightOverrides = layerHeightOverrides,
                modifierState = modifierState,
                onExecute = onExecute,
                onFeedback = onFeedback,
                minSwipeDistancePx = minSwipeDistancePx,
                legendVisibility = legendVisibility,
                modifierBehaviors = behaviors,
                keyPadding = keyPadding,
                keyPaddingVertical = keyPaddingVertical,
                keyBorderWidthDp = keyBorderWidthDp,
                keyCornerRadius = keyCornerRadius,
                vibrateOnTap = vibrateOnTap,
                capabilities = capabilities,
                ime = ime,
                animations = animations,
                isPasswordField = passwordField,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (backdropEnabled) {
                            Modifier.background(MaterialTheme.colorScheme.background)
                        } else {
                            Modifier
                        },
                    ),
        ) {
            if (backdropEnabled) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(if (!ignoreBottomPadding) Modifier.safeDrawingPadding() else Modifier)
                        .padding(bottom = pushupSize)
                        .then(if (backdropEnabled) Modifier.padding(top = 6.dp) else Modifier),
            ) {
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
    }
}

private data class ClipboardLayerSession(
    val items: List<ClipboardItem>,
    val enabled: Boolean,
    val onPasteAndLeave: (ClipboardItem) -> Unit,
    val onPasteAndStay: (ClipboardItem) -> Unit,
    val onDelete: (ClipboardItem) -> Unit,
    val onPin: (ClipboardItem) -> Unit,
    val onBack: () -> Unit,
    val onClearAll: () -> Unit,
    val onOpenSettings: () -> Unit,
)

@Composable
private fun EngineKeyboardPanel(
    modifier: Modifier,
    namedLayout: NamedLayout,
    layer: LayoutLayer,
    clipboardOrigin: LayoutLayer,
    clipboardSession: ClipboardLayerSession,
    keyHeight: Dp,
    layerHeightOverrides: Map<LayoutLayer, Int>,
    modifierState: MutableState<ModifierState>,
    onExecute: (SemanticAction) -> Unit,
    onFeedback: (FeedbackEvent) -> Unit,
    minSwipeDistancePx: Float,
    legendVisibility: LegendVisibility,
    modifierBehaviors: Map<ModifierId, ModifierBehavior>,
    keyPadding: Int,
    keyPaddingVertical: Int,
    keyBorderWidthDp: Float,
    keyCornerRadius: Dp,
    vibrateOnTap: Boolean,
    capabilities: EditorCapabilities,
    ime: IMEService,
    animations: KeyAnimationSettings,
    isPasswordField: Boolean,
) {
    val grid =
        if (layer == LayoutLayer.CLIPBOARD) {
            namedLayout.gridForClipboard(clipboardOrigin)
        } else {
            namedLayout.gridFor(layer)
        }
    val overrideRows = layerHeightOverrides[layer] ?: 0
    val contentRows = namedLayout.contentRows(layer, overrideRows)
    Column(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        if (contentRows > 0) {
            LayerContentSlot(
                content = namedLayout.contentFor(layer),
                height = keyHeight * contentRows,
                vibrateOnTap = vibrateOnTap,
                capabilities = capabilities,
                ime = ime,
                clipboardSession = clipboardSession,
                keyHeight = keyHeight,
                keyPadding = keyPadding,
                keyCornerRadius = keyCornerRadius,
            )
        }
        LayoutGrid(
            layout = grid,
            namedLayout = namedLayout,
            keyHeight = keyHeight,
            modifierState = modifierState,
            onExecute = onExecute,
            onFeedback = onFeedback,
            minSwipeDistancePx = minSwipeDistancePx,
            legendVisibility = legendVisibility,
            modifierBehaviors = modifierBehaviors,
            keyPadding = keyPadding,
            keyPaddingVertical = keyPaddingVertical,
            keyBorderWidthDp = keyBorderWidthDp,
            keyCornerRadius = keyCornerRadius,
            animations = animations,
            isPasswordField = isPasswordField,
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
    clipboardSession: ClipboardLayerSession,
    keyHeight: Dp,
    keyPadding: Int,
    keyCornerRadius: Dp,
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
        LayerContent.ClipboardHistory -> {
            ClipboardHistoryScreen(
                clipboardItems = clipboardSession.items,
                isEnabled = clipboardSession.enabled,
                onItemClick = clipboardSession.onPasteAndLeave,
                onItemPaste = clipboardSession.onPasteAndStay,
                onItemDelete = clipboardSession.onDelete,
                onItemTogglePin = clipboardSession.onPin,
                onBack = clipboardSession.onBack,
                onClearAll = clipboardSession.onClearAll,
                onGoToClipboardSettings = clipboardSession.onOpenSettings,
                keyHeight = keyHeight.value,
                keyPadding = keyPadding,
                cornerRadius = keyCornerRadius.value,
                vibrateOnTap = vibrateOnTap,
                modifier = Modifier.fillMaxWidth().height(height),
            )
        }
    }
}

@Composable
private fun LayoutGrid(
    layout: Layout,
    namedLayout: NamedLayout,
    keyHeight: Dp,
    modifierState: MutableState<ModifierState>,
    onExecute: (SemanticAction) -> Unit,
    onFeedback: (FeedbackEvent) -> Unit,
    minSwipeDistancePx: Float,
    legendVisibility: LegendVisibility,
    modifierBehaviors: Map<ModifierId, ModifierBehavior>,
    keyPadding: Int,
    keyPaddingVertical: Int,
    keyBorderWidthDp: Float,
    keyCornerRadius: Dp,
    animations: KeyAnimationSettings,
    isPasswordField: Boolean,
) {
    val rows = remember(layout) { layoutRows(layout) }
    val shiftActive = modifierState.value.isActive(ModifierId.SHIFT)
    for (row in rows) {
        Row(modifier = Modifier.fillMaxWidth().height(keyHeight)) {
            for (position in row) {
                val mapping = layout[position] ?: continue
                key(position) {
                    EngineKeyboardKey(
                        mapping = mapping,
                        modifierState = modifierState,
                        shiftActive = shiftActive,
                        onExecute = onExecute,
                        onFeedback = onFeedback,
                        shiftMappings = namedLayout.shiftMappings,
                        minSwipeDistancePx = minSwipeDistancePx,
                        legendVisibility = legendVisibility,
                        modifierBehaviors = modifierBehaviors,
                        keyHeight = keyHeight,
                        keyPadding = keyPadding,
                        keyPaddingVertical = keyPaddingVertical,
                        keyBorderWidthDp = keyBorderWidthDp,
                        keyCornerRadius = keyCornerRadius,
                        animations = animations,
                        isPasswordField = isPasswordField,
                        modifier = Modifier.weight(mapping.columnSpan.toFloat()).fillMaxHeight(),
                    )
                }
            }
        }
    }
}
