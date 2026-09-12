package com.suave.s12.ui.engine

import android.content.Intent
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
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
import com.suave.s12.db.DEFAULT_INLINE_SUGGESTIONS
import com.suave.s12.db.DEFAULT_INLINE_SUGGESTION_HEIGHT
import com.suave.s12.db.DEFAULT_KEY_BORDER_WIDTH
import com.suave.s12.db.DEFAULT_KEY_HEIGHT
import com.suave.s12.db.DEFAULT_KEY_PADDING
import com.suave.s12.db.DEFAULT_KEY_PADDING_VERTICAL
import com.suave.s12.db.DEFAULT_KEY_RADIUS
import com.suave.s12.db.DEFAULT_KEYBOARD_POSITIONS
import com.suave.s12.db.DEFAULT_MIN_SWIPE_LENGTH
import com.suave.s12.db.DEFAULT_POSITION
import com.suave.s12.db.DEFAULT_PREVENT_CRAMPED_DUAL
import com.suave.s12.db.DEFAULT_PREVENT_NEEDLESS_SPLIT
import com.suave.s12.db.DEFAULT_PUSHUP_SIZE
import com.suave.s12.db.DEFAULT_SHIFT_AS_MODIFIER
import com.suave.s12.db.DEFAULT_SHOW_DEBUG_BAR
import com.suave.s12.db.DEFAULT_DISTINCT_LETTER_CONTROL_COLORS
import com.suave.s12.db.DEFAULT_VIBRATE_HOLD_REPEAT_TYPE
import com.suave.s12.db.DEFAULT_VIBRATE_MODIFIER_TYPE
import com.suave.s12.db.DEFAULT_VIBRATE_ON_HOLD_REPEAT
import com.suave.s12.db.DEFAULT_VIBRATE_ON_MODIFIER
import com.suave.s12.db.DEFAULT_VIBRATE_ON_SLIDE
import com.suave.s12.db.DEFAULT_VIBRATE_ON_SWIPE
import com.suave.s12.db.DEFAULT_VIBRATE_ON_TAP
import com.suave.s12.db.DEFAULT_VIBRATE_SLIDE_TYPE
import com.suave.s12.db.DEFAULT_VIBRATE_SWIPE_TYPE
import com.suave.s12.db.DEFAULT_VIBRATE_TAP_TYPE
import com.suave.s12.engine.action.SemanticAction
import com.suave.s12.engine.intent.CommandId
import com.suave.s12.engine.capability.EditorCapabilities
import com.suave.s12.engine.capability.EditorCapabilityResolver
import com.suave.s12.engine.capability.EditorInfoDebug
import com.suave.s12.engine.feedback.FeedbackDispatcher
import com.suave.s12.engine.feedback.FeedbackEvent
import com.suave.s12.engine.feedback.FeedbackSettings
import com.suave.s12.engine.feedback.HapticChannel
import com.suave.s12.engine.feedback.HapticType
import com.suave.s12.engine.feedback.hapticTypeFromDb
import com.suave.s12.engine.intent.KeyPosition
import com.suave.s12.engine.intent.Layout
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.engine.intent.columnCount
import com.suave.s12.engine.intent.layoutRows
import com.suave.s12.engine.modifier.ModifierBehavior
import com.suave.s12.engine.modifier.ModifierState
import com.suave.s12.engine.modifier.modifierBehaviors
import com.suave.s12.engine.output.OutputExecutor
import com.suave.s12.ime.INLINE_STATUS_IDLE
import com.suave.s12.layout.BuiltinLayouts
import com.suave.s12.layout.DEFAULT_LAYER_HEIGHTS
import com.suave.s12.layout.LayerContent
import com.suave.s12.layout.LayerSession
import com.suave.s12.layout.LayoutLayer
import com.suave.s12.layout.NamedLayout
import com.suave.s12.layout.canCycleKeyboardPosition
import com.suave.s12.layout.coerceDisplayedPosition
import com.suave.s12.layout.enterOverlay
import com.suave.s12.layout.leaveOverlay
import com.suave.s12.layout.nextKeyboardPosition
import com.suave.s12.layout.parseKeyboardPositions
import com.suave.s12.layout.parseLayerHeightOverrides
import com.suave.s12.layout.reachableKeyboardPositions
import com.suave.s12.layout.selectBaseLayer
import com.suave.s12.layout.splitColumnRanges
import com.suave.s12.layout.toggleClipboard
import com.suave.s12.layout.toggleEmoji
import com.suave.s12.ui.components.keyboard.ClipboardHistoryScreen
import com.suave.s12.utils.KeyboardPosition
import com.suave.s12.utils.isPasswordField
import com.suave.s12.utils.toBool
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Renders the selected [NamedLayout] on the new engine end to end. Owns the two pieces of
 * state every key on the keyboard shares: [ModifierState] (modifiers are not a layout mode)
 * and [LayoutLayer] (numeric/emoji/clipboard are layout switches, not modifiers). Both survive Dual's
 * second copy of the grid, so Ctrl held on the left half still applies on the right.
 *
 * The grid is derived from the layout data ([layoutRows]), not a hardcoded 4x5. Suave is one
 * [BuiltinLayouts] entry; switching [AppSettings.keyboardLayout] selects another.
 * [AppSettings.position] Dual draws two full copies that share modifier and layer state. Split
 * keeps one content slot and cuts the key grid in half, duplicating the middle column when the
 * count is odd. Left, Right, and Center are all full width until the layout has a real (narrower)
 * key width to park. Key width comes from [com.suave.s12.engine.intent.KeyMapping.columnSpan].
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
    val layerSessionState = remember { mutableStateOf(LayerSession()) }
    val layer = layerSessionState.value.layer
    val clipboardScope = rememberCoroutineScope()
    val emptyClipboardItems = remember { MutableLiveData(emptyList<ClipboardItem>()) }
    val clipboardItems by
        (clipboardRepository?.allClipboardItems ?: emptyClipboardItems).observeAsState(emptyList())

    val canSwitchLayout = BuiltinLayouts.canSwitch(settings?.keyboardLayouts)
    val namedLayout = BuiltinLayouts.byIndex(settings?.keyboardLayout ?: 0)
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val reachablePositions =
        reachableKeyboardPositions(
            enabled = parseKeyboardPositions(settings?.keyboardPositions ?: DEFAULT_KEYBOARD_POSITIONS),
            preventCrampedDual = (settings?.preventCrampedDual ?: DEFAULT_PREVENT_CRAMPED_DUAL).toBool(),
            preventNeedlessSplit = (settings?.preventNeedlessSplit ?: DEFAULT_PREVENT_NEEDLESS_SPLIT).toBool(),
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            columnCount = namedLayout.layout.columnCount(),
        )
    val canMoveKeyboard = canCycleKeyboardPosition(reachablePositions)
    val feedbackSettings =
        remember(
            settings?.vibrateOnTap,
            settings?.vibrateOnSwipe,
            settings?.vibrateOnSlide,
            settings?.vibrateOnHoldRepeat,
            settings?.vibrateOnModifier,
            settings?.vibrateTapType,
            settings?.vibrateSwipeType,
            settings?.vibrateSlideType,
            settings?.vibrateHoldRepeatType,
            settings?.vibrateModifierType,
        ) {
            settings?.toFeedbackSettings()
                ?: FeedbackSettings(
                    tap =
                        HapticChannel(
                            DEFAULT_VIBRATE_ON_TAP.toBool(),
                            hapticTypeFromDb(DEFAULT_VIBRATE_TAP_TYPE),
                        ),
                    swipe =
                        HapticChannel(
                            DEFAULT_VIBRATE_ON_SWIPE.toBool(),
                            hapticTypeFromDb(DEFAULT_VIBRATE_SWIPE_TYPE),
                        ),
                    slide =
                        HapticChannel(
                            DEFAULT_VIBRATE_ON_SLIDE.toBool(),
                            hapticTypeFromDb(DEFAULT_VIBRATE_SLIDE_TYPE),
                        ),
                    repeat =
                        HapticChannel(
                            DEFAULT_VIBRATE_ON_HOLD_REPEAT.toBool(),
                            hapticTypeFromDb(DEFAULT_VIBRATE_HOLD_REPEAT_TYPE),
                        ),
                    modifier =
                        HapticChannel(
                            DEFAULT_VIBRATE_ON_MODIFIER.toBool(),
                            hapticTypeFromDb(DEFAULT_VIBRATE_MODIFIER_TYPE),
                        ),
                )
        }
    val vibrateOnTap = feedbackSettings.tap.enabled
    val tapHapticType = feedbackSettings.tap.type
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
            canSwitchLayout = canSwitchLayout,
            canMoveKeyboard = canMoveKeyboard,
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
    val keyboardPosition =
        coerceDisplayedPosition(
            KeyboardPosition.entries.getOrElse(settings?.position ?: DEFAULT_POSITION) { KeyboardPosition.Center },
            reachablePositions,
        )
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
    val distinctLetterControlColors =
        (settings?.distinctLetterControlColors ?: DEFAULT_DISTINCT_LETTER_CONTROL_COLORS).toBool()

    val hapticPlayer = remember(view) { HapticFeedbackPlayer(view) }
    val inputEpoch by ime.inputEpoch.collectAsState()
    val autofillStatus by ime.inlineAutofill.status.collectAsState()
    val capabilities = remember(inputEpoch) { EditorCapabilityResolver.resolve(ime.currentInputEditorInfo) }
    val passwordField = remember(inputEpoch) {
        ime.currentInputEditorInfo?.let { isPasswordField(ime) } ?: false
    }

    LaunchedEffect(namedLayout.id) {
        layerSessionState.value = LayerSession()
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
                layerSessionState.value = layerSessionState.value.leaveOverlay()
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
            onBack = { layerSessionState.value = layerSessionState.value.leaveOverlay() },
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
    val canSwitchLayoutState = rememberUpdatedState(canSwitchLayout)
    val canMoveKeyboardState = rememberUpdatedState(canMoveKeyboard)
    val reachablePositionsState = rememberUpdatedState(reachablePositions)
    val onChangePositionState = rememberUpdatedState(onChangePosition)
    val namedLayoutState = rememberUpdatedState(namedLayout)
    val appHost =
        remember {
            AppCommandHost(
                onToggleHideLetters = { onToggleHideLettersState.value() },
                onSwitchLanguage = {
                    if (canSwitchLayoutState.value) {
                        layerSessionState.value = LayerSession()
                        onSwitchLanguageState.value()
                    }
                },
                onChangePosition = { _ ->
                    if (canMoveKeyboardState.value) {
                        onChangePositionState.value { stored ->
                            nextKeyboardPosition(stored, reachablePositionsState.value)
                        }
                    }
                },
                onSelectLayer = { requested ->
                    val current = namedLayoutState.value
                    val session = layerSessionState.value
                    layerSessionState.value =
                        when (requested) {
                            LayoutLayer.NUMERIC ->
                                if (current.numericLayout != null) {
                                    session.selectBaseLayer(LayoutLayer.NUMERIC)
                                } else {
                                    session
                                }
                            LayoutLayer.MAIN -> session.selectBaseLayer(LayoutLayer.MAIN)
                            LayoutLayer.EMOJI ->
                                if (current.emojiBottomRow != null) {
                                    session.enterOverlay(LayoutLayer.EMOJI)
                                } else {
                                    session
                                }
                            LayoutLayer.CLIPBOARD ->
                                if (current.clipboardBottomRow != null ||
                                    current.layerContent[LayoutLayer.CLIPBOARD] != null
                                ) {
                                    session.enterOverlay(LayoutLayer.CLIPBOARD)
                                } else {
                                    session
                                }
                        }
                },
                onToggleEmojiLayer = {
                    val current = namedLayoutState.value
                    layerSessionState.value =
                        layerSessionState.value.toggleEmoji(current.emojiBottomRow != null)
                },
                onToggleClipboardHistory = {
                    val current = namedLayoutState.value
                    layerSessionState.value =
                        layerSessionState.value.toggleClipboard(
                            current.clipboardBottomRow != null ||
                                current.layerContent[LayoutLayer.CLIPBOARD] != null,
                        )
                },
            )
        }
        val onExecute =
            remember(capabilities, ime, appHost) {
                { action: SemanticAction ->
                    val filledTop =
                        action is SemanticAction.TypeCommand &&
                            action.id == CommandId.ARROW_RIGHT &&
                            action.modifiers.isEmpty() &&
                            ime.acceptTopInlineSuggestion()
                    if (!filledTop) {
                        ActionExecutor.execute(
                            action = action,
                            capabilities = capabilities,
                            ime = ime,
                            host = appHost,
                        )
                    }
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
            // effect), which app the IME is connected to, and orthogonal EditorInfo facts
            // (class / variation / flags / content mime) rather than a single fake field type.
            val installTime =
                remember {
                    val info = ime.packageManager.getPackageInfo(ime.packageName, 0)
                    SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(info.lastUpdateTime))
                }
            val targetApp = remember(inputEpoch) { ime.currentInputEditorInfo?.packageName ?: "?" }
            val editorDebug = remember(inputEpoch) { EditorInfoDebug.describe(ime.currentInputEditorInfo) }
            val inlineEnabled = (settings?.inlineSuggestions ?: DEFAULT_INLINE_SUGGESTIONS).toBool()
            val af =
                when {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> "na"
                    !inlineEnabled -> "off"
                    else -> autofillStatus.ifEmpty { INLINE_STATUS_IDLE }
                }
            Text(
                text = "$installTime | $targetApp | $editorDebug | af=$af",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onError,
            )
        }
        ImeNoticeBanner(ime = ime)
        if ((settings?.inlineSuggestions ?: DEFAULT_INLINE_SUGGESTIONS).toBool()) {
            InlineSuggestionStrip(
                ime = ime,
                heightDp = settings?.inlineSuggestionHeight ?: DEFAULT_INLINE_SUGGESTION_HEIGHT,
            )
        }
        val renderPanel: @Composable (Modifier, Boolean) -> Unit = { panelModifier, splitHalves ->
            EngineKeyboardPanel(
                modifier = panelModifier,
                namedLayout = namedLayout,
                layer = layer,
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
                tapHapticType = tapHapticType,
                capabilities = capabilities,
                ime = ime,
                animations = animations,
                isPasswordField = passwordField,
                distinctLetterControlColors = distinctLetterControlColors,
                splitHalves = splitHalves,
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
                when (keyboardPosition) {
                    KeyboardPosition.Dual -> {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            renderPanel(Modifier.weight(1f), false)
                            renderPanel(Modifier.weight(1f), false)
                        }
                    }
                    KeyboardPosition.Split -> renderPanel(Modifier.fillMaxWidth(), true)
                    else -> renderPanel(Modifier.fillMaxWidth(), false)
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
    tapHapticType: HapticType,
    capabilities: EditorCapabilities,
    ime: IMEService,
    animations: KeyAnimationSettings,
    isPasswordField: Boolean,
    distinctLetterControlColors: Boolean,
    splitHalves: Boolean,
) {
    val grid = namedLayout.gridFor(layer)
    val overrideRows = layerHeightOverrides[layer] ?: 0
    val contentRows = namedLayout.contentRows(layer, overrideRows)
    Column(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        if (contentRows > 0) {
            LayerContentSlot(
                content = namedLayout.contentFor(layer),
                height = keyHeight * contentRows,
                vibrateOnTap = vibrateOnTap,
                tapHapticType = tapHapticType,
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
            distinctLetterControlColors = distinctLetterControlColors,
            splitHalves = splitHalves,
        )
    }
}

@Composable
private fun LayerContentSlot(
    content: LayerContent,
    height: Dp,
    vibrateOnTap: Boolean,
    tapHapticType: HapticType,
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
            key(pickerText, pickerIcon, pickerAccent, darkKeyboard, vibrateOnTap, tapHapticType) {
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
                                    view.playHaptic(tapHapticType)
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
                tapHapticType = tapHapticType,
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
    distinctLetterControlColors: Boolean,
    splitHalves: Boolean,
) {
    val rows = remember(layout) { layoutRows(layout) }
    val splitRanges =
        remember(layout, splitHalves) {
            if (splitHalves) splitColumnRanges(layout.columnCount()) else null
        }
    val shiftActive = modifierState.value.isActive(ModifierId.SHIFT)
    for (row in rows) {
        Row(modifier = Modifier.fillMaxWidth().height(keyHeight)) {
            val ranges = splitRanges
            if (ranges == null) {
                LayoutRowKeys(
                    positions = row,
                    layout = layout,
                    namedLayout = namedLayout,
                    keyHeight = keyHeight,
                    modifierState = modifierState,
                    shiftActive = shiftActive,
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
                    distinctLetterControlColors = distinctLetterControlColors,
                    keyPrefix = "",
                )
            } else {
                val (leftCols, rightCols) = ranges
                Row(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    LayoutRowKeys(
                        positions = row.filter { it.col in leftCols },
                        layout = layout,
                        namedLayout = namedLayout,
                        keyHeight = keyHeight,
                        modifierState = modifierState,
                        shiftActive = shiftActive,
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
                        distinctLetterControlColors = distinctLetterControlColors,
                        keyPrefix = "L",
                    )
                }
                Row(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    LayoutRowKeys(
                        positions = row.filter { it.col in rightCols },
                        layout = layout,
                        namedLayout = namedLayout,
                        keyHeight = keyHeight,
                        modifierState = modifierState,
                        shiftActive = shiftActive,
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
                        distinctLetterControlColors = distinctLetterControlColors,
                        keyPrefix = "R",
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.LayoutRowKeys(
    positions: List<KeyPosition>,
    layout: Layout,
    namedLayout: NamedLayout,
    keyHeight: Dp,
    modifierState: MutableState<ModifierState>,
    shiftActive: Boolean,
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
    distinctLetterControlColors: Boolean,
    keyPrefix: String,
) {
    for (position in positions) {
        val mapping = layout[position] ?: continue
        key(keyPrefix, position) {
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
                distinctLetterControlColors = distinctLetterControlColors,
                modifier = Modifier.weight(mapping.columnSpan.toFloat()).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ImeNoticeBanner(ime: IMEService) {
    val notice by ime.notice.collectAsState()
    LaunchedEffect(notice) {
        val current = notice ?: return@LaunchedEffect
        delay(1600)
        ime.clearNotice(current.seq)
    }
    AnimatedVisibility(visible = notice != null) {
        Text(
            text = notice?.text.orEmpty(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.inverseSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.inverseOnSurface,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
        )
    }
}
