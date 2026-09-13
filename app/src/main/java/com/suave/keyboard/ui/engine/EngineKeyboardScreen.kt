package com.suave.keyboard.ui.engine

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.MutableLiveData
import com.suave.keyboard.IMEService
import com.suave.keyboard.MainActivity
import com.suave.keyboard.R
import com.suave.keyboard.SettingsSession
import com.suave.keyboard.SuaveApplication
import com.suave.keyboard.db.AppSettings
import com.suave.keyboard.db.ClipboardItem
import kotlin.math.roundToInt
import com.suave.keyboard.db.ClipboardRepository
import com.suave.keyboard.db.DEFAULT_ALT_AS_MODIFIER
import com.suave.keyboard.db.DEFAULT_ANIMATION_LETTER_DROP
import com.suave.keyboard.db.DEFAULT_ANIMATION_PRESS_HIGHLIGHT
import com.suave.keyboard.db.DEFAULT_ANIMATION_RELEASE_FLASH
import com.suave.keyboard.db.DEFAULT_AUTO_CAPITALIZE
import com.suave.keyboard.db.DEFAULT_BACKDROP_ENABLED
import com.suave.keyboard.db.DEFAULT_CLIPBOARD_HISTORY_ENABLED
import com.suave.keyboard.db.DEFAULT_CLIPBOARD_IMAGES_ENABLED
import com.suave.keyboard.db.DEFAULT_CTRL_AS_MODIFIER
import com.suave.keyboard.db.DEFAULT_DISTINCT_LETTER_CONTROL_COLORS
import com.suave.keyboard.db.DEFAULT_ESC_AS_MODIFIER
import com.suave.keyboard.db.DEFAULT_HIDE_EDITING
import com.suave.keyboard.db.DEFAULT_HIDE_LAYER_SWITCHES
import com.suave.keyboard.db.DEFAULT_HIDE_LETTERS
import com.suave.keyboard.db.DEFAULT_HIDE_MODIFIERS
import com.suave.keyboard.db.DEFAULT_HIDE_NAVIGATION
import com.suave.keyboard.db.DEFAULT_HIDE_NUMBERS
import com.suave.keyboard.db.DEFAULT_HIDE_SPECIALS
import com.suave.keyboard.db.DEFAULT_HIDE_SYMBOLS
import com.suave.keyboard.db.DEFAULT_IGNORE_BOTTOM_PADDING
import com.suave.keyboard.db.DEFAULT_INLINE_SUGGESTIONS
import com.suave.keyboard.db.DEFAULT_INLINE_SUGGESTION_HEIGHT
import com.suave.keyboard.db.DEFAULT_KEYBOARD_POSITIONS
import com.suave.keyboard.db.DEFAULT_KEY_BORDER_WIDTH
import com.suave.keyboard.db.DEFAULT_KEY_HEIGHT
import com.suave.keyboard.db.DEFAULT_LANDSCAPE_KEY_HEIGHT
import com.suave.keyboard.db.DEFAULT_KEY_PADDING
import com.suave.keyboard.db.DEFAULT_KEY_PADDING_VERTICAL
import com.suave.keyboard.db.DEFAULT_KEY_RADIUS
import com.suave.keyboard.db.DEFAULT_MIN_SWIPE_LENGTH
import com.suave.keyboard.db.DEFAULT_POSITION
import com.suave.keyboard.db.DEFAULT_PREVENT_CRAMPED_DUAL
import com.suave.keyboard.db.DEFAULT_PREVENT_NEEDLESS_SPLIT
import com.suave.keyboard.db.DEFAULT_PUSHUP_SIZE
import com.suave.keyboard.db.DEFAULT_SHIFT_AS_MODIFIER
import com.suave.keyboard.db.DEFAULT_SHOW_DEBUG_BAR
import com.suave.keyboard.db.DEFAULT_SPACEBAR_MULTITAPS
import com.suave.keyboard.db.DEFAULT_VIBRATE_HOLD_REPEAT_TYPE
import com.suave.keyboard.db.DEFAULT_VIBRATE_MODIFIER_TYPE
import com.suave.keyboard.db.DEFAULT_VIBRATE_ON_HOLD_REPEAT
import com.suave.keyboard.db.DEFAULT_VIBRATE_ON_MODIFIER
import com.suave.keyboard.db.DEFAULT_VIBRATE_ON_SLIDE
import com.suave.keyboard.db.DEFAULT_VIBRATE_ON_SWIPE
import com.suave.keyboard.db.DEFAULT_VIBRATE_ON_TAP
import com.suave.keyboard.db.DEFAULT_VIBRATE_SLIDE_TYPE
import com.suave.keyboard.db.DEFAULT_VIBRATE_SWIPE_TYPE
import com.suave.keyboard.db.DEFAULT_VIBRATE_TAP_TYPE
import com.suave.keyboard.engine.action.SemanticAction
import com.suave.keyboard.engine.action.SpacebarMultitapTracker
import com.suave.keyboard.engine.capability.EditorCapabilities
import com.suave.keyboard.engine.capability.EditorCapabilityResolver
import com.suave.keyboard.engine.capability.EditorInfoDebug
import com.suave.keyboard.engine.feedback.FeedbackDispatcher
import com.suave.keyboard.engine.feedback.FeedbackEvent
import com.suave.keyboard.engine.feedback.FeedbackSettings
import com.suave.keyboard.engine.feedback.HapticChannel
import com.suave.keyboard.engine.feedback.HapticType
import com.suave.keyboard.engine.feedback.hapticTypeFromDb
import com.suave.keyboard.engine.intent.CommandId
import com.suave.keyboard.engine.intent.KeyFillRole
import com.suave.keyboard.engine.intent.KeyPosition
import com.suave.keyboard.engine.intent.Layout
import com.suave.keyboard.engine.intent.ModifierId
import com.suave.keyboard.engine.intent.columnCount
import com.suave.keyboard.engine.intent.layoutRows
import com.suave.keyboard.engine.modifier.ModifierBehavior
import com.suave.keyboard.engine.modifier.ModifierState
import com.suave.keyboard.engine.modifier.applyAutoCapitalize
import com.suave.keyboard.engine.modifier.initialAutoCapitalizeState
import com.suave.keyboard.engine.modifier.modifierBehaviors
import com.suave.keyboard.engine.output.ClipboardPaste
import com.suave.keyboard.engine.output.LiveClipboardImage
import com.suave.keyboard.engine.output.OutputExecutor
import com.suave.keyboard.ime.formatAutofillDebug
import com.suave.keyboard.layout.ActiveLayer
import com.suave.keyboard.layout.DEFAULT_LAYER_HEIGHTS
import com.suave.keyboard.layout.LayerContent
import com.suave.keyboard.layout.LayerSession
import com.suave.keyboard.layout.LayoutPreviewSession
import com.suave.keyboard.layout.LayoutRegistry
import com.suave.keyboard.layout.NamedLayout
import com.suave.keyboard.layout.boardWidthDp
import com.suave.keyboard.layout.canCycleKeyboardPosition
import com.suave.keyboard.layout.coerceDisplayedPosition
import com.suave.keyboard.layout.leaveOverlay
import com.suave.keyboard.layout.maxCellWidthDp
import com.suave.keyboard.layout.nextKeyboardPosition
import com.suave.keyboard.layout.parkedHalfWidthDp
import com.suave.keyboard.layout.parseKeyboardPositions
import com.suave.keyboard.layout.parseLayerHeightOverrides
import com.suave.keyboard.layout.parseLayerId
import com.suave.keyboard.layout.reachableKeyboardPositions
import com.suave.keyboard.layout.resolveKeyHeightDp
import com.suave.keyboard.layout.selectBase
import com.suave.keyboard.layout.splitColumnRanges
import com.suave.keyboard.layout.switchTo
import com.suave.keyboard.layout.toggleClipboard
import com.suave.keyboard.layout.toggleEmoji
import com.suave.keyboard.ui.components.clipboard.ClipboardHistoryScreen
import com.suave.keyboard.utils.KeyboardPosition
import com.suave.keyboard.utils.isPasswordField
import com.suave.keyboard.utils.toBool
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders the selected [NamedLayout] on the new engine end to end. Owns the two pieces of
 * state every key on the keyboard shares: [ModifierState] (modifiers are not a layout mode)
 * and active layer id (numeric/emoji/clipboard are layout switches, not modifiers). Both survive Dual's
 * second copy of the grid, so Ctrl held on the left half still applies on the right.
 *
 * The grid is derived from the layout data ([layoutRows]), not a hardcoded 4x5. Suave is one
 * [LayoutRegistry] entry; switching [AppSettings.keyboardLayout] selects another.
 * [AppSettings.position] Dual draws two full copies that share modifier and layer state. Split
 * keeps one content slot and cuts the key grid in half, duplicating the middle column when the
 * count is odd. Key cell width is capped at key height so landscape does not stretch keys into
 * paddles; Center parks a capped board in the middle, Dual and Split park halves on the left and
 * right with a flexible gap between them.
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
    val spacebarMultitap = remember { SpacebarMultitapTracker() }
    val layerSessionState = remember { mutableStateOf(LayerSession()) }
    val activeLayer = layerSessionState.value.current
    val clipboardScope = rememberCoroutineScope()
    val emptyClipboardItems = remember { MutableLiveData(emptyList<ClipboardItem>()) }
    val clipboardItems by
        (clipboardRepository?.allClipboardItems ?: emptyClipboardItems).observeAsState(emptyList())
    val liveClipboardImage by ime.clipboardLiveImage().collectAsState()

    val canSwitchLayout = LayoutRegistry.canSwitch(ctx, settings?.keyboardLayouts)
    val previewLayout by LayoutPreviewSession.layout.collectAsState()
    val useEditedLayout by LayoutPreviewSession.useEdited.collectAsState()
    val settingsOpen by SettingsSession.open.collectAsState()
    var layoutTick by remember { mutableIntStateOf(0) }
    val selectedLayout =
        LayoutRegistry.byId(ctx, settings?.keyboardLayout ?: LayoutRegistry.DEFAULT_ID)
    // layoutTick forces a re-read after per-app float overrides update the registry.
    val namedLayout =
        run {
            layoutTick
            LayoutPreviewSession.resolve(selectedLayout)
        }
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
            columnCount = namedLayout.homeLayer().keyGrid.columnCount(),
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
    val autoCapitalize = (settings?.autoCapitalize ?: DEFAULT_AUTO_CAPITALIZE).toBool()
    val spacebarMultitapEnabled =
        (settings?.spacebarMultitaps ?: DEFAULT_SPACEBAR_MULTITAPS).toBool()
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
    // weight inside a board whose cell width is capped at keyHeight, so landscape never
    // stretches keys into paddles. Layout can override Appearance heights.
    val landscape = screenWidthDp > screenHeightDp
    val keyHeightDp =
        resolveKeyHeightDp(
            landscape = landscape,
            layoutKeyHeight = namedLayout.keyHeight,
            layoutLandscapeKeyHeight = namedLayout.landscapeKeyHeight,
            settingsKeyHeight = settings?.keyHeight,
            settingsLandscapeKeyHeight = settings?.landscapeKeyHeight,
            defaultKeyHeight = DEFAULT_KEY_HEIGHT,
            defaultLandscapeKeyHeight = DEFAULT_LANDSCAPE_KEY_HEIGHT,
        )
    val keyHeight = keyHeightDp.dp
    val maxCellWidthDpValue = maxCellWidthDp(keyHeightDp)
    val keyCornerRadius = keyHeight * (keyRadiusPercent / 200f)
    val parkHalves =
        keyboardPosition == KeyboardPosition.Dual || keyboardPosition == KeyboardPosition.Split
    val hostPackageName = ime.currentInputEditorInfo?.packageName
    val floatingLandscape =
        landscape && namedLayout.effectiveLandscapeFloating(hostPackageName)
    val density = LocalDensity.current
    val columnCount = namedLayout.gridFor(activeLayer).columnCount()
    val parkedHalfWidthPx =
        remember(columnCount, maxCellWidthDpValue, screenWidthDp, density) {
            with(density) {
                parkedHalfWidthDp(
                    columnCount = columnCount,
                    maxCellWidthDp = maxCellWidthDpValue,
                    screenWidthDp = screenWidthDp,
                ).dp.toPx()
            }
        }
    val boardWidthPx =
        remember(columnCount, maxCellWidthDpValue, screenWidthDp, density) {
            with(density) {
                minOf(
                    screenWidthDp,
                    boardWidthDp(columnCount, maxCellWidthDpValue),
                ).dp.toPx()
            }
        }

    DisposableEffect(floatingLandscape) {
        ime.setLandscapeFloating(floatingLandscape)
        onDispose { ime.setLandscapeFloating(false) }
    }

    val layerHeightOverrides = parseLayerHeightOverrides(settings?.layerHeights ?: DEFAULT_LAYER_HEIGHTS)
    val animations =
        KeyAnimationSettings(
            pressHighlight = (settings?.animationPressHighlight ?: DEFAULT_ANIMATION_PRESS_HIGHLIGHT).toBool(),
            releaseFlash = (settings?.animationReleaseFlash ?: DEFAULT_ANIMATION_RELEASE_FLASH).toBool(),
            letterDrop = (settings?.animationLetterDrop ?: DEFAULT_ANIMATION_LETTER_DROP).toBool(),
        )
    val distinctLetterControlColors =
        (settings?.distinctLetterControlColors ?: DEFAULT_DISTINCT_LETTER_CONTROL_COLORS).toBool()

    val imeNotice = rememberImeNoticeDraw(ime)
    val imeNoticeMeasurer = rememberTextMeasurer()
    val hapticPlayer = remember(view) { HapticFeedbackPlayer(view) }
    val inputEpoch by ime.inputEpoch.collectAsState()
    val autofillStatus by ime.inlineAutofill.status.collectAsState()
    val capabilities = remember(inputEpoch) { EditorCapabilityResolver.resolve(ime.currentInputEditorInfo) }
    val passwordField =
        remember(inputEpoch) {
            ime.currentInputEditorInfo?.let { isPasswordField(ime) } ?: false
        }

    LaunchedEffect(namedLayout.id) {
        layerSessionState.value = LayerSession()
    }
    LaunchedEffect(inputEpoch, autoCapitalize) {
        spacebarMultitap.reset()
        modifierState.value = initialAutoCapitalizeState(ime, autoCapitalize)
    }
    LaunchedEffect(activeLayer) {
        if (activeLayer == ActiveLayer.Clipboard) {
            ime.clipboardIngestPrimary()
            clipboardRepository?.clearExpired()
        }
    }

    val clipboardHistoryEnabled =
        (settings?.clipboardHistoryEnabled ?: DEFAULT_CLIPBOARD_HISTORY_ENABLED).toBool()
    val clipboardImagesEnabled =
        (settings?.clipboardImagesEnabled ?: DEFAULT_CLIPBOARD_IMAGES_ENABLED).toBool()
    val clipboardSession =
        ClipboardLayerSession(
            items = clipboardItems,
            enabled = clipboardHistoryEnabled && clipboardRepository != null,
            liveImage = liveClipboardImage,
            imagesEnabled = clipboardImagesEnabled,
            onPasteAndLeave = { item ->
                if (ClipboardPaste.pasteHistoryItem(ime, item)) {
                    layerSessionState.value = layerSessionState.value.leaveOverlay()
                }
            },
            onPasteAndStay = { item ->
                ClipboardPaste.pasteHistoryItem(ime, item)
            },
            onPasteLiveAndLeave = {
                liveClipboardImage?.let { live ->
                    if (ClipboardPaste.pasteLiveImage(ime, live)) {
                        layerSessionState.value = layerSessionState.value.leaveOverlay()
                    }
                }
            },
            onPasteLiveAndStay = {
                liveClipboardImage?.let { live -> ClipboardPaste.pasteLiveImage(ime, live) }
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
    val bumpLayoutTickState = rememberUpdatedState { layoutTick++ }
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
                        if (current.layer(requested) != null) {
                            session.switchTo(requested, current)
                        } else {
                            session
                        }
                },
                onSwitchLayer = { layerId ->
                    val current = namedLayoutState.value
                    val session = layerSessionState.value
                    val target = parseLayerId(layerId)
                    layerSessionState.value =
                        if (current.layer(target) != null) {
                            // Toggle: same overlay/base again returns home / leaves overlay.
                            when {
                                session.current == target && current.isOverlay(target) ->
                                    session.leaveOverlay()
                                session.current == target ->
                                    session.selectBase(current.homeActive())
                                else -> session.switchTo(target, current)
                            }
                        } else {
                            session
                        }
                },
                onToggleEmojiLayer = {
                    val current = namedLayoutState.value
                    layerSessionState.value =
                        layerSessionState.value.toggleEmoji(
                            available = current.layer(ActiveLayer.Emoji) != null,
                            layout = current,
                        )
                },
                onToggleClipboardHistory = {
                    val current = namedLayoutState.value
                    layerSessionState.value =
                        layerSessionState.value.toggleClipboard(
                            available = current.layer(ActiveLayer.Clipboard) != null,
                            layout = current,
                        )
                },
                onToggleLandscapeFloating = {
                    val pkg = ime.currentInputEditorInfo?.packageName
                    if (pkg.isNullOrBlank()) {
                        ime.showNotice(ime.getString(R.string.landscape_floating_need_app))
                    } else {
                        val layoutId = namedLayoutState.value.id
                        clipboardScope.launch {
                            try {
                                val store = (ime.application as SuaveApplication).userLayoutStore
                                val updated = store.toggleLandscapeFloatingForApp(layoutId, pkg)
                                LayoutPreviewSession.updateIfActive(updated)
                                bumpLayoutTickState.value()
                                val label =
                                    try {
                                        val pm = ime.packageManager
                                        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                                    } catch (_: Exception) {
                                        pkg
                                    }
                                val on = updated.effectiveLandscapeFloating(pkg)
                                val metrics = ime.resources.displayMetrics
                                val inLandscape = metrics.widthPixels > metrics.heightPixels
                                ime.showNotice(
                                    ime.getString(
                                        when {
                                            !inLandscape && on ->
                                                R.string.landscape_floating_portrait_on
                                            !inLandscape && !on ->
                                                R.string.landscape_floating_portrait_off
                                            on -> R.string.landscape_floating_on_for_app
                                            else -> R.string.landscape_floating_off_for_app
                                        },
                                        label,
                                    ),
                                )
                            } catch (e: Exception) {
                                ime.showNotice(e.message ?: "Failed")
                            }
                        }
                    }
                },
            )
        }
    val onExecute =
        remember(capabilities, ime, appHost, autoCapitalize) {
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
                    if (shouldApplyAutoCapitalizeAfter(action)) {
                        modifierState.value =
                            applyAutoCapitalize(
                                modifierState.value,
                                ime,
                                autoCapitalize,
                                committed = action,
                            )
                    }
                }
            }
        }
    val onFeedback =
        remember(feedbackSettings, hapticPlayer) {
            { event: FeedbackEvent -> FeedbackDispatcher.dispatch(event, feedbackSettings, hapticPlayer) }
        }

    val imeAction =
        remember(inputEpoch) {
            val options = ime.currentInputEditorInfo?.imeOptions ?: 0
            options and EditorInfo.IME_MASK_ACTION
        }
    CompositionLocalProvider(LocalImeAction provides imeAction) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (settingsOpen) {
            SettingsGarageBar(
                showingTitle = namedLayout.title,
                canToggle = previewLayout != null,
                useEdited = useEditedLayout,
                onUseEditedChange = { LayoutPreviewSession.setUseEdited(it) },
            )
        }
        if (showDebugBar) {
            // Install timestamp comes from PackageManager at runtime (Gradle config-time
            // Date() went stale whenever the configuration cache reused a previous run).
            // The second line lists orthogonal EditorInfo facts, not a fake field class.
            val installTime =
                remember {
                    val info = ime.packageManager.getPackageInfo(ime.packageName, 0)
                    SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(info.lastUpdateTime))
                }
            val targetApp = remember(inputEpoch) { ime.currentInputEditorInfo?.packageName ?: "?" }
            val editorDebug = remember(inputEpoch) { EditorInfoDebug.label(ime.currentInputEditorInfo) }
            val inlineEnabled = (settings?.inlineSuggestions ?: DEFAULT_INLINE_SUGGESTIONS).toBool()
            val hasAutofillId =
                remember(inputEpoch) {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                        ime.currentInputEditorInfo?.autofillId != null
                }
            val af =
                formatAutofillDebug(
                    sdkAtLeastR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
                    inlineEnabled = inlineEnabled,
                    hasAutofillId = hasAutofillId,
                    status = autofillStatus,
                )
            EditorDebugBar(
                meta = "$installTime | $targetApp | $af",
                compact = editorDebug.compact,
                verbose = editorDebug.verbose,
                onCopy = { text ->
                    val clipboard = ime.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("editor", text))
                    ime.showNotice("Copied")
                },
            )
        }
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
                activeLayer = activeLayer,
                clipboardSession = clipboardSession,
                keyHeight = keyHeight,
                maxCellWidthDp = maxCellWidthDpValue,
                screenWidthDp = screenWidthDp,
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
                spacebarMultitap = spacebarMultitap,
                spacebarMultitapEnabled = spacebarMultitapEnabled,
                splitHalves = splitHalves,
                paintBoardBackground = true,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        // Dual/Split park opaque halves; never fill the gap between them.
                        // Floating landscape also skips a full-width strip so hosts show through.
                        if (backdropEnabled && !parkHalves && !floatingLandscape) {
                            Modifier.background(MaterialTheme.colorScheme.background)
                        } else {
                            Modifier
                        },
                    ).drawImeNotice(imeNotice, imeNoticeMeasurer),
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
                        .then(if (backdropEnabled) Modifier.padding(top = 6.dp) else Modifier)
                        .onGloballyPositioned { coords ->
                            if (!floatingLandscape) {
                                ime.setFloatingTouchableRects(view, emptyList())
                                return@onGloballyPositioned
                            }
                            val bounds = coords.boundsInRoot()
                            val left = bounds.left.roundToInt()
                            val top = bounds.top.roundToInt()
                            val right = bounds.right.roundToInt()
                            val bottom = bounds.bottom.roundToInt()
                            val width = (right - left).coerceAtLeast(0)
                            val rects = ArrayList<Rect>(3)
                            // Garage / debug / suggestion chrome above the key grid.
                            if (top > 0) {
                                rects.add(Rect(0, 0, view.width.coerceAtLeast(right), top))
                            }
                            when (keyboardPosition) {
                                KeyboardPosition.Dual,
                                KeyboardPosition.Split,
                                -> {
                                    val half = parkedHalfWidthPx.roundToInt().coerceIn(0, width)
                                    rects.add(Rect(left, top, left + half, bottom))
                                    rects.add(Rect(right - half, top, right, bottom))
                                }
                                KeyboardPosition.Left -> {
                                    val board = boardWidthPx.roundToInt().coerceIn(0, width)
                                    rects.add(Rect(left, top, left + board, bottom))
                                }
                                KeyboardPosition.Right -> {
                                    val board = boardWidthPx.roundToInt().coerceIn(0, width)
                                    rects.add(Rect(right - board, top, right, bottom))
                                }
                                KeyboardPosition.Center -> {
                                    val board = boardWidthPx.roundToInt().coerceIn(0, width)
                                    val start = left + ((width - board) / 2)
                                    rects.add(Rect(start, top, start + board, bottom))
                                }
                            }
                            ime.setFloatingTouchableRects(view, rects)
                        },
            ) {
                when (keyboardPosition) {
                    KeyboardPosition.Dual -> {
                        val halfWidth =
                            parkedHalfWidthDp(
                                columnCount = namedLayout.gridFor(activeLayer).columnCount(),
                                maxCellWidthDp = maxCellWidthDpValue,
                                screenWidthDp = screenWidthDp,
                            ).dp
                        Row(modifier = Modifier.fillMaxWidth()) {
                            renderPanel(Modifier.width(halfWidth), false)
                            Spacer(modifier = Modifier.weight(1f))
                            renderPanel(Modifier.width(halfWidth), false)
                        }
                    }

                    KeyboardPosition.Split -> {
                        renderPanel(Modifier.fillMaxWidth(), true)
                    }

                    KeyboardPosition.Left -> {
                        val boardWidth =
                            minOf(
                                screenWidthDp,
                                boardWidthDp(
                                    namedLayout.gridFor(activeLayer).columnCount(),
                                    maxCellWidthDpValue,
                                ),
                            ).dp
                        Row(modifier = Modifier.fillMaxWidth()) {
                            renderPanel(Modifier.width(boardWidth), false)
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    KeyboardPosition.Right -> {
                        val boardWidth =
                            minOf(
                                screenWidthDp,
                                boardWidthDp(
                                    namedLayout.gridFor(activeLayer).columnCount(),
                                    maxCellWidthDpValue,
                                ),
                            ).dp
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.weight(1f))
                            renderPanel(Modifier.width(boardWidth), false)
                        }
                    }

                    KeyboardPosition.Center -> {
                        val boardWidth =
                            minOf(
                                screenWidthDp,
                                boardWidthDp(
                                    namedLayout.gridFor(activeLayer).columnCount(),
                                    maxCellWidthDpValue,
                                ),
                            ).dp
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            renderPanel(Modifier.width(boardWidth), false)
                        }
                    }
                }
            }
        }
    }
    }
}

private data class ClipboardLayerSession(
    val items: List<ClipboardItem>,
    val enabled: Boolean,
    val liveImage: LiveClipboardImage?,
    val imagesEnabled: Boolean,
    val onPasteAndLeave: (ClipboardItem) -> Unit,
    val onPasteAndStay: (ClipboardItem) -> Unit,
    val onPasteLiveAndLeave: () -> Unit,
    val onPasteLiveAndStay: () -> Unit,
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
    activeLayer: ActiveLayer,
    clipboardSession: ClipboardLayerSession,
    keyHeight: Dp,
    maxCellWidthDp: Int,
    screenWidthDp: Int,
    layerHeightOverrides: Map<String, Int>,
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
    spacebarMultitap: SpacebarMultitapTracker,
    spacebarMultitapEnabled: Boolean,
    splitHalves: Boolean,
    paintBoardBackground: Boolean,
) {
    val grid = namedLayout.gridFor(activeLayer)
    val overrideRows = layerHeightOverrides[activeLayer.id] ?: 0
    val contentRows = namedLayout.contentRows(activeLayer, overrideRows)
    val boardBg = MaterialTheme.colorScheme.background
    Column(
        modifier =
            modifier.then(
                if (paintBoardBackground && !splitHalves) {
                    Modifier.background(boardBg)
                } else {
                    Modifier
                },
            ),
    ) {
        if (contentRows > 0) {
            LayerContentSlot(
                content = namedLayout.contentFor(activeLayer),
                height = keyHeight * contentRows,
                vibrateOnTap = vibrateOnTap,
                tapHapticType = tapHapticType,
                capabilities = capabilities,
                ime = ime,
                clipboardSession = clipboardSession,
                keyHeight = keyHeight,
                keyPadding = keyPadding,
                keyCornerRadius = keyCornerRadius,
                modifier =
                    if (splitHalves) {
                        Modifier.fillMaxWidth().background(boardBg)
                    } else {
                        Modifier.fillMaxWidth()
                    },
            )
        }
        LayoutGrid(
            layout = grid,
            namedLayout = namedLayout,
            keyHeight = keyHeight,
            maxCellWidthDp = maxCellWidthDp,
            screenWidthDp = screenWidthDp,
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
            spacebarMultitap = spacebarMultitap,
            spacebarMultitapEnabled = spacebarMultitapEnabled,
            splitHalves = splitHalves,
            halfBackground = if (splitHalves) boardBg else null,
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
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val slotModifier = modifier.fillMaxWidth().height(height)
    when (content) {
        LayerContent.None -> {
            Spacer(modifier = slotModifier)
        }

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
                    modifier = slotModifier,
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
                liveImage = clipboardSession.liveImage,
                onLiveImageClick = clipboardSession.onPasteLiveAndLeave,
                onLiveImagePaste = clipboardSession.onPasteLiveAndStay,
                imagesEnabled = clipboardSession.imagesEnabled,
                modifier = slotModifier,
            )
        }
    }
}

@Composable
private fun LayoutGrid(
    layout: Layout,
    namedLayout: NamedLayout,
    keyHeight: Dp,
    maxCellWidthDp: Int,
    screenWidthDp: Int,
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
    spacebarMultitap: SpacebarMultitapTracker,
    spacebarMultitapEnabled: Boolean,
    splitHalves: Boolean,
    halfBackground: Color? = null,
) {
    val rows = remember(layout) { layoutRows(layout) }
    val splitRanges =
        remember(layout, splitHalves) {
            if (splitHalves) splitColumnRanges(layout.columnCount()) else null
        }
    val halfWidth =
        remember(layout, maxCellWidthDp, screenWidthDp, splitHalves) {
            if (splitHalves) {
                parkedHalfWidthDp(
                    columnCount = layout.columnCount(),
                    maxCellWidthDp = maxCellWidthDp,
                    screenWidthDp = screenWidthDp,
                ).dp
            } else {
                0.dp
            }
        }
    val shiftLegendState = modifierState.value.forLetterLegends()
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
                    shiftLegendState = shiftLegendState,
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
                    spacebarMultitap = spacebarMultitap,
                    spacebarMultitapEnabled = spacebarMultitapEnabled,
                    keyPrefix = "",
                )
            } else {
                val (leftCols, rightCols) = ranges
                val halfMod =
                    Modifier
                        .width(halfWidth)
                        .fillMaxHeight()
                        .then(
                            if (halfBackground != null) {
                                Modifier.background(halfBackground)
                            } else {
                                Modifier
                            },
                        )
                Row(modifier = halfMod) {
                    LayoutRowKeys(
                        positions = row.filter { it.col in leftCols },
                        layout = layout,
                        namedLayout = namedLayout,
                        keyHeight = keyHeight,
                        modifierState = modifierState,
                        shiftLegendState = shiftLegendState,
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
                        spacebarMultitap = spacebarMultitap,
                        spacebarMultitapEnabled = spacebarMultitapEnabled,
                        keyPrefix = "L",
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(modifier = halfMod) {
                    LayoutRowKeys(
                        positions = row.filter { it.col in rightCols },
                        layout = layout,
                        namedLayout = namedLayout,
                        keyHeight = keyHeight,
                        modifierState = modifierState,
                        shiftLegendState = shiftLegendState,
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
                        spacebarMultitap = spacebarMultitap,
                        spacebarMultitapEnabled = spacebarMultitapEnabled,
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
    shiftLegendState: ModifierState,
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
    spacebarMultitap: SpacebarMultitapTracker,
    spacebarMultitapEnabled: Boolean,
    keyPrefix: String,
) {
    for (position in positions) {
        val mapping = layout[position] ?: continue
        key(keyPrefix, position) {
            if (mapping.fillRole == KeyFillRole.SPACER) {
                Spacer(
                    modifier =
                        Modifier
                            .weight(mapping.columnSpan)
                            .fillMaxHeight(),
                )
            } else {
                EngineKeyboardKey(
                    mapping = mapping,
                    modifierState = modifierState,
                    shiftLegendState = shiftLegendState,
                    onExecute = onExecute,
                    onFeedback = onFeedback,
                    shiftMappings = namedLayout.shiftMappings,
                    capsLockMappings = namedLayout.capsLockMappings,
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
                    spacebarMultitap = spacebarMultitap,
                    spacebarMultitapEnabled = spacebarMultitapEnabled,
                    spaceMultitapCycle = namedLayout.spaceMultitapCycle,
                    switchLayerIcons = namedLayout.switchLayerIconMap(),
                    modifier = Modifier.weight(mapping.columnSpan).fillMaxHeight(),
                )
            }
        }
    }
}

private fun shouldApplyAutoCapitalizeAfter(action: SemanticAction): Boolean =
    when (action) {
        is SemanticAction.TypeText,
        is SemanticAction.ReplaceLastText,
        -> true
        is SemanticAction.TypeCommand -> action.id == CommandId.SPACE
        else -> false
    }

@Composable
private fun SettingsGarageBar(
    showingTitle: String,
    canToggle: Boolean,
    useEdited: Boolean,
    onUseEditedChange: (Boolean) -> Unit,
) {
    val container = MaterialTheme.colorScheme.secondaryContainer
    val onContainer = MaterialTheme.colorScheme.onSecondaryContainer
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(container)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = showingTitle,
            color = onContainer,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (canToggle) {
            Text(
                text =
                    stringResource(
                        if (useEdited) {
                            R.string.settings_keyboard_bar_edited
                        } else {
                            R.string.settings_keyboard_bar_active
                        },
                    ),
                color = onContainer.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
            Switch(
                checked = useEdited,
                onCheckedChange = onUseEditedChange,
            )
        } else {
            Text(
                text = stringResource(R.string.settings_keyboard_bar_badge),
                color = onContainer.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EditorDebugBar(
    meta: String,
    compact: String,
    verbose: String,
    onCopy: (String) -> Unit,
) {
    val onError = MaterialTheme.colorScheme.onError
    val lineStyle =
        TextStyle(
            color = onError,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            textAlign = TextAlign.Center,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.error)
                .clickable { onCopy(verbose) }
                .padding(horizontal = 6.dp, vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = meta,
            style = lineStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = compact,
            style = lineStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
