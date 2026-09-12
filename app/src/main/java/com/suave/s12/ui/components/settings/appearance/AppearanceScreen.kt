package com.suave.s12.ui.components.settings.appearance

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardBackspace
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BorderBottom
import androidx.compose.material.icons.outlined.BorderOuter
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Crop75
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.Height
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardCapslock
import androidx.compose.material.icons.outlined.KeyboardControlKey
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Padding
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RoundedCorner
import androidx.compose.material.icons.outlined.South
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material.icons.outlined.WebAssetOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.suave.s12.R
import com.suave.s12.db.AppSettingsViewModel
import com.suave.s12.db.DEFAULT_ANIMATION_LETTER_DROP
import com.suave.s12.db.DEFAULT_ANIMATION_PRESS_HIGHLIGHT
import com.suave.s12.db.DEFAULT_ANIMATION_RELEASE_FLASH
import com.suave.s12.db.DEFAULT_BACKDROP_ENABLED
import com.suave.s12.db.DEFAULT_DISABLE_FULLSCREEN_EDITOR
import com.suave.s12.db.DEFAULT_DISTINCT_LETTER_CONTROL_COLORS
import com.suave.s12.db.DEFAULT_HIDE_EDITING
import com.suave.s12.db.DEFAULT_HIDE_KEY_CATEGORIES
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
import com.suave.s12.db.DEFAULT_PUSHUP_SIZE
import com.suave.s12.db.DEFAULT_THEME
import com.suave.s12.db.DEFAULT_THEME_COLOR
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
import com.suave.s12.db.AppearanceUpdate
import com.suave.s12.engine.feedback.HapticType
import com.suave.s12.engine.feedback.hapticTypeFromDb
import com.suave.s12.layout.BuiltinLayouts
import com.suave.s12.layout.DEFAULT_LAYER_HEIGHTS
import com.suave.s12.layout.LayoutLayer
import com.suave.s12.layout.MAX_LAYER_HEIGHT_ROWS
import com.suave.s12.layout.NamedLayout
import com.suave.s12.layout.formatLayerHeightOverrides
import com.suave.s12.layout.parseLayerHeightOverrides
import com.suave.s12.ui.components.common.IntStepperPreference
import com.suave.s12.ui.components.common.SettingRow
import com.suave.s12.ui.components.common.SettingTitle
import com.suave.s12.ui.components.common.SettingsSection
import com.suave.s12.ui.components.common.TestOutTextField
import com.suave.s12.ui.engine.HIDE_KEY_GROUP_ORDER
import com.suave.s12.ui.engine.LegendCategory
import com.suave.s12.ui.engine.formatHideKeyCategories
import com.suave.s12.ui.engine.parseHideKeyCategories
import com.suave.s12.ui.engine.toggleHideKeyGroupSelection
import com.suave.s12.utils.SimpleTopAppBar
import com.suave.s12.utils.TAG
import com.suave.s12.utils.ThemeColor
import com.suave.s12.utils.ThemeMode
import com.suave.s12.utils.toBool
import com.suave.s12.utils.toInt
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceTheme
import me.zhanghai.compose.preference.SwitchPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
) {
    Log.d(TAG, "Got to appearance activity")

    val resources = LocalResources.current
    val settings by appSettingsViewModel.appSettings.observeAsState()
    var themeState = ThemeMode.entries[settings?.theme ?: DEFAULT_THEME]
    var themeColorState = ThemeColor.entries[settings?.themeColor ?: DEFAULT_THEME_COLOR]
    var keyHeightState = settings?.keyHeight ?: DEFAULT_KEY_HEIGHT
    var distinctLetterControlColorsState =
        (settings?.distinctLetterControlColors ?: DEFAULT_DISTINCT_LETTER_CONTROL_COLORS).toBool()

    var vibrateOnTapState = (settings?.vibrateOnTap ?: DEFAULT_VIBRATE_ON_TAP).toBool()
    var vibrateOnSwipeState = (settings?.vibrateOnSwipe ?: DEFAULT_VIBRATE_ON_SWIPE).toBool()
    var vibrateOnSlideState = (settings?.vibrateOnSlide ?: DEFAULT_VIBRATE_ON_SLIDE).toBool()
    var vibrateOnHoldRepeatState =
        (settings?.vibrateOnHoldRepeat ?: DEFAULT_VIBRATE_ON_HOLD_REPEAT).toBool()
    var vibrateOnModifierState = (settings?.vibrateOnModifier ?: DEFAULT_VIBRATE_ON_MODIFIER).toBool()
    var vibrateTapTypeState = hapticTypeFromDb(settings?.vibrateTapType ?: DEFAULT_VIBRATE_TAP_TYPE)
    var vibrateSwipeTypeState = hapticTypeFromDb(settings?.vibrateSwipeType ?: DEFAULT_VIBRATE_SWIPE_TYPE)
    var vibrateSlideTypeState = hapticTypeFromDb(settings?.vibrateSlideType ?: DEFAULT_VIBRATE_SLIDE_TYPE)
    var vibrateHoldRepeatTypeState =
        hapticTypeFromDb(settings?.vibrateHoldRepeatType ?: DEFAULT_VIBRATE_HOLD_REPEAT_TYPE)
    var vibrateModifierTypeState =
        hapticTypeFromDb(settings?.vibrateModifierType ?: DEFAULT_VIBRATE_MODIFIER_TYPE)
    var animationPressHighlightState =
        (settings?.animationPressHighlight ?: DEFAULT_ANIMATION_PRESS_HIGHLIGHT).toBool()
    var animationReleaseFlashState =
        (settings?.animationReleaseFlash ?: DEFAULT_ANIMATION_RELEASE_FLASH).toBool()
    var animationLetterDropState =
        (settings?.animationLetterDrop ?: DEFAULT_ANIMATION_LETTER_DROP).toBool()
    var hideLettersState = (settings?.hideLetters ?: DEFAULT_HIDE_LETTERS).toBool()
    var hideSymbolsState = (settings?.hideSymbols ?: DEFAULT_HIDE_SYMBOLS).toBool()
    var hideNumbersState = (settings?.hideNumbers ?: DEFAULT_HIDE_NUMBERS).toBool()
    var hideModifiersState = (settings?.hideModifiers ?: DEFAULT_HIDE_MODIFIERS).toBool()
    var hideLayerSwitchesState = (settings?.hideLayerSwitches ?: DEFAULT_HIDE_LAYER_SWITCHES).toBool()
    var hideSpecialsState = (settings?.hideSpecials ?: DEFAULT_HIDE_SPECIALS).toBool()
    var hideNavigationState = (settings?.hideNavigation ?: DEFAULT_HIDE_NAVIGATION).toBool()
    var hideEditingState = (settings?.hideEditing ?: DEFAULT_HIDE_EDITING).toBool()
    var hideKeyCategoriesState = settings?.hideKeyCategories ?: DEFAULT_HIDE_KEY_CATEGORIES
    var ignoreBottomPaddingState = (settings?.ignoreBottomPadding ?: DEFAULT_IGNORE_BOTTOM_PADDING).toBool()
    var disableFullscreenEditorState = (settings?.disableFullscreenEditor ?: DEFAULT_DISABLE_FULLSCREEN_EDITOR).toBool()
    var backdropEnabledState = (settings?.backdropEnabled ?: DEFAULT_BACKDROP_ENABLED).toBool()
    var keyPaddingState = settings?.keyPadding ?: DEFAULT_KEY_PADDING
    var keyPaddingVerticalState = settings?.keyPaddingVertical ?: DEFAULT_KEY_PADDING_VERTICAL
    var keyBorderWidthState = settings?.keyBorderWidth ?: DEFAULT_KEY_BORDER_WIDTH
    var keyRadiusState = settings?.keyRadius ?: DEFAULT_KEY_RADIUS
    var pushupSizeState = settings?.pushupSize ?: DEFAULT_PUSHUP_SIZE
    var layerHeightsState = settings?.layerHeights ?: DEFAULT_LAYER_HEIGHTS
    val namedLayout = BuiltinLayouts.byIndex(settings?.keyboardLayout ?: 0)
    val layerHeightOverrides = parseLayerHeightOverrides(layerHeightsState)

    fun updateAppearance() {
        appSettingsViewModel.updateAppearance(
            AppearanceUpdate(
                id = 1,
                vibrateOnTap = vibrateOnTapState.toInt(),
                vibrateOnSlide = vibrateOnSlideState.toInt(),
                vibrateOnHoldRepeat = vibrateOnHoldRepeatState.toInt(),
                vibrateOnSwipe = vibrateOnSwipeState.toInt(),
                vibrateOnModifier = vibrateOnModifierState.toInt(),
                vibrateTapType = vibrateTapTypeState.ordinal,
                vibrateSwipeType = vibrateSwipeTypeState.ordinal,
                vibrateSlideType = vibrateSlideTypeState.ordinal,
                vibrateHoldRepeatType = vibrateHoldRepeatTypeState.ordinal,
                vibrateModifierType = vibrateModifierTypeState.ordinal,
                hideLetters = hideLettersState.toInt(),
                hideSymbols = hideSymbolsState.toInt(),
                hideNumbers = hideNumbersState.toInt(),
                hideModifiers = hideModifiersState.toInt(),
                hideLayerSwitches = hideLayerSwitchesState.toInt(),
                hideSpecials = hideSpecialsState.toInt(),
                hideNavigation = hideNavigationState.toInt(),
                hideEditing = hideEditingState.toInt(),
                hideKeyCategories = hideKeyCategoriesState,
                ignoreBottomPadding = ignoreBottomPaddingState.toInt(),
                theme = themeState.ordinal,
                themeColor = themeColorState.ordinal,
                keyHeight = keyHeightState,
                layerHeights = layerHeightsState,
                disableFullscreenEditor = disableFullscreenEditorState.toInt(),
                backdropEnabled = backdropEnabledState.toInt(),
                keyPadding = keyPaddingState,
                keyPaddingVertical = keyPaddingVerticalState,
                keyBorderWidth = keyBorderWidthState,
                keyRadius = keyRadiusState,
                pushupSize = pushupSizeState,
                animationPressHighlight = animationPressHighlightState.toInt(),
                animationReleaseFlash = animationReleaseFlashState.toInt(),
                animationLetterDrop = animationLetterDropState.toInt(),
                distinctLetterControlColors = distinctLetterControlColorsState.toInt(),
            ),
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleTopAppBar(text = stringResource(R.string.appearance), navController = navController)
        },
        content = { padding ->
            Column(
                modifier =
                    Modifier
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .background(color = MaterialTheme.colorScheme.surface)
                        .imePadding(),
            ) {
                ProvidePreferenceTheme {
                    SettingsSection(title = stringResource(R.string.theme)) {
                    SettingRow(onReset = {
                        themeState = ThemeMode.entries[DEFAULT_THEME]
                        updateAppearance()
                    }) {
                        ListPreference(
                            type = ListPreferenceType.DROPDOWN_MENU,
                            value = themeState,
                            onValueChange = {
                                themeState = it
                                updateAppearance()
                            },
                            values = ThemeMode.entries,
                            valueToText = {
                                AnnotatedString(resources.getString(it.resId))
                            },
                            title = {
                                Text(stringResource(R.string.theme))
                            },
                            summary = {
                                Text(stringResource(themeState.resId))
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Palette,
                                    contentDescription = null,
                                )
                            },
                        )
                    }

                    SettingRow(onReset = {
                        themeColorState = ThemeColor.entries[DEFAULT_THEME_COLOR]
                        updateAppearance()
                    }) {
                        ListPreference(
                            type = ListPreferenceType.DROPDOWN_MENU,
                            value = themeColorState,
                            onValueChange = {
                                themeColorState = it
                                updateAppearance()
                            },
                            values = ThemeColor.entries,
                            valueToText = {
                                AnnotatedString(resources.getString(it.resId))
                            },
                            title = {
                                Text(stringResource(R.string.theme_color))
                            },
                            summary = {
                                Text(stringResource(themeColorState.resId))
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Colorize,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                    }

                    SettingsSection(
                        title = stringResource(R.string.settings_section_labels)                    ) {
                    HideLabelSwitch(
                        value = hideLettersState,
                        onValueChange = {
                            hideLettersState = it
                            updateAppearance()
                        },
                        title = R.string.hide_letters,
                        onSummary = R.string.hide_letters_on,
                        offSummary = R.string.hide_letters_off,
                        icon = Icons.Outlined.HideImage,
                        infoText = stringResource(R.string.hide_labels_info),
                    )
                    HideLabelSwitch(
                        value = hideSymbolsState,
                        onValueChange = {
                            hideSymbolsState = it
                            updateAppearance()
                        },
                        title = R.string.hide_symbols,
                        onSummary = R.string.hide_symbols_on,
                        offSummary = R.string.hide_symbols_off,
                        icon = Icons.Outlined.Tag,
                    )
                    HideLabelSwitch(
                        value = hideNumbersState,
                        onValueChange = {
                            hideNumbersState = it
                            updateAppearance()
                        },
                        title = R.string.hide_numbers,
                        onSummary = R.string.hide_numbers_on,
                        offSummary = R.string.hide_numbers_off,
                        icon = Icons.Outlined.Numbers,
                    )
                    HideLabelSwitch(
                        value = hideModifiersState,
                        onValueChange = {
                            hideModifiersState = it
                            updateAppearance()
                        },
                        title = R.string.hide_modifiers,
                        onSummary = R.string.hide_modifiers_on,
                        offSummary = R.string.hide_modifiers_off,
                        icon = Icons.Outlined.KeyboardControlKey,
                    )
                    HideLabelSwitch(
                        value = hideLayerSwitchesState,
                        onValueChange = {
                            hideLayerSwitchesState = it
                            updateAppearance()
                        },
                        title = R.string.hide_layer_switches,
                        onSummary = R.string.hide_layer_switches_on,
                        offSummary = R.string.hide_layer_switches_off,
                        icon = Icons.Outlined.Layers,
                    )
                    HideLabelSwitch(
                        value = hideSpecialsState,
                        onValueChange = {
                            hideSpecialsState = it
                            updateAppearance()
                        },
                        title = R.string.hide_specials,
                        onSummary = R.string.hide_specials_on,
                        offSummary = R.string.hide_specials_off,
                        icon = Icons.Outlined.ContentCopy,
                    )
                    HideLabelSwitch(
                        value = hideNavigationState,
                        onValueChange = {
                            hideNavigationState = it
                            updateAppearance()
                        },
                        title = R.string.hide_navigation,
                        onSummary = R.string.hide_navigation_on,
                        offSummary = R.string.hide_navigation_off,
                        icon = Icons.Outlined.KeyboardArrowUp,
                    )
                    HideLabelSwitch(
                        value = hideEditingState,
                        onValueChange = {
                            hideEditingState = it
                            updateAppearance()
                        },
                        title = R.string.hide_editing,
                        onSummary = R.string.hide_editing_on,
                        offSummary = R.string.hide_editing_off,
                        icon = Icons.AutoMirrored.Outlined.KeyboardBackspace,
                    )
                    HideKeyGroupsPreference(
                        value = hideKeyCategoriesState,
                        onValueChange = {
                            hideKeyCategoriesState = it
                            updateAppearance()
                        },
                    )
                    }

                    SettingsSection(
                        title = stringResource(R.string.settings_section_keyboard)                    ) {
                    SettingRow {
                        SwitchPreference(
                            value = backdropEnabledState,
                            onValueChange = {
                                backdropEnabledState = it
                                updateAppearance()
                            },
                            title = {
                                Text(stringResource(R.string.backdrop))
                            },
                            summary = {
                                Text(stringResource(if (backdropEnabledState) R.string.backdrop_on else R.string.backdrop_off))
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.ViewDay,
                                    contentDescription = null,
                                )
                            },
                        )
                    }

                    SettingRow {
                        SwitchPreference(
                            value = ignoreBottomPaddingState,
                            onValueChange = {
                                ignoreBottomPaddingState = it
                                updateAppearance()
                            },
                            title = {
                                Text(stringResource(R.string.ignore_bottom_padding))
                            },
                            summary = {
                                Text(
                                    stringResource(
                                        if (ignoreBottomPaddingState) {
                                            R.string.ignore_bottom_padding_on
                                        } else {
                                            R.string.ignore_bottom_padding_off
                                        },
                                    ),
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.BorderBottom,
                                    contentDescription = null,
                                )
                            },
                        )
                    }

                    SettingRow(
                        onReset = {
                            pushupSizeState = DEFAULT_PUSHUP_SIZE
                            updateAppearance()
                        },
                    ) {
                        IntStepperPreference(
                            value = pushupSizeState,
                            onValueChange = {
                                pushupSizeState = it
                                updateAppearance()
                            },
                            valueRange = 0..250,
                            vibrateOnRepeat = vibrateOnHoldRepeatState,
                            repeatHapticType = vibrateHoldRepeatTypeState,
                            title = {
                                SettingTitle(
                                    text = stringResource(R.string.raise_from_bottom),
                                    infoText = stringResource(R.string.raise_from_bottom_info),
                                )
                            },
                            summary = {
                                Text(
                                    if (pushupSizeState == 0) {
                                        stringResource(R.string.raise_from_bottom_summary_none)
                                    } else {
                                        stringResource(R.string.raise_from_bottom_summary, pushupSizeState.toString())
                                    },
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.VerticalAlignTop,
                                    contentDescription = null,
                                )
                            },
                        )
                    }

                    SwitchPreference(
                        value = disableFullscreenEditorState,
                        onValueChange = {
                            disableFullscreenEditorState = it
                            updateAppearance()
                        },
                        title = {
                            SettingTitle(
                                text = stringResource(R.string.disable_fullscreen_editor),
                                infoText = stringResource(R.string.disable_fullscreen_editor_info),
                            )
                        },
                        summary = {
                            Text(
                                stringResource(
                                    if (disableFullscreenEditorState) {
                                        R.string.disable_fullscreen_editor_on
                                    } else {
                                        R.string.disable_fullscreen_editor_off
                                    },
                                ),
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.WebAssetOff,
                                contentDescription = null,
                            )
                        },
                    )
                    }

                    SettingsSection(title = stringResource(R.string.settings_section_keys)) {
                    SettingRow {
                        SwitchPreference(
                            value = distinctLetterControlColorsState,
                            onValueChange = {
                                distinctLetterControlColorsState = it
                                updateAppearance()
                            },
                            title = {
                                SettingTitle(
                                    text = stringResource(R.string.letter_and_control_colors),
                                    infoText = stringResource(R.string.letter_and_control_colors_info),
                                )
                            },
                            summary = {
                                Text(
                                    stringResource(
                                        if (distinctLetterControlColorsState) {
                                            R.string.letter_and_control_colors_on
                                        } else {
                                            R.string.letter_and_control_colors_off
                                        },
                                    ),
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.FormatColorFill,
                                    contentDescription = null,
                                )
                            },
                        )
                    }

                    SettingRow(
                        onReset = {
                            keyHeightState = DEFAULT_KEY_HEIGHT
                            updateAppearance()
                        },
                    ) {
                        IntStepperPreference(
                            value = keyHeightState,
                            onValueChange = {
                                keyHeightState = it
                                updateAppearance()
                            },
                            valueRange = 10..200,
                            vibrateOnRepeat = vibrateOnHoldRepeatState,
                            repeatHapticType = vibrateHoldRepeatTypeState,
                            title = {
                                Text(stringResource(R.string.key_height))
                            },
                            summary = {
                                Text(stringResource(R.string.key_height_summary, keyHeightState.toString()))
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Crop75,
                                    contentDescription = null,
                                )
                            },
                        )
                    }

                    SettingRow(
                        onReset = {
                            keyPaddingState = DEFAULT_KEY_PADDING
                            updateAppearance()
                        },
                    ) {
                        IntStepperPreference(
                            value = keyPaddingState,
                            onValueChange = {
                                keyPaddingState = it
                                updateAppearance()
                            },
                            valueRange = 0..10,
                            vibrateOnRepeat = vibrateOnHoldRepeatState,
                            repeatHapticType = vibrateHoldRepeatTypeState,
                            title = {
                                SettingTitle(
                                    text = stringResource(R.string.key_spacing_horizontal),
                                    infoText = stringResource(R.string.key_spacing_info),
                                )
                            },
                            summary = {
                                Text(
                                    if (keyPaddingState == 0) {
                                        stringResource(R.string.key_spacing_horizontal_summary_none)
                                    } else {
                                        stringResource(
                                            R.string.key_spacing_horizontal_summary,
                                            keyPaddingState.toString(),
                                        )
                                    },
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Padding,
                                    contentDescription = null,
                                )
                            },
                        )
                    }

                    SettingRow(
                        onReset = {
                            keyPaddingVerticalState = DEFAULT_KEY_PADDING_VERTICAL
                            updateAppearance()
                        },
                    ) {
                        IntStepperPreference(
                            value = keyPaddingVerticalState,
                            onValueChange = {
                                keyPaddingVerticalState = it
                                updateAppearance()
                            },
                            valueRange = 0..10,
                            vibrateOnRepeat = vibrateOnHoldRepeatState,
                            repeatHapticType = vibrateHoldRepeatTypeState,
                            title = {
                                SettingTitle(
                                    text = stringResource(R.string.key_spacing_vertical),
                                    infoText = stringResource(R.string.key_spacing_info),
                                )
                            },
                            summary = {
                                Text(
                                    if (keyPaddingVerticalState == 0) {
                                        stringResource(R.string.key_spacing_vertical_summary_none)
                                    } else {
                                        stringResource(
                                            R.string.key_spacing_vertical_summary,
                                            keyPaddingVerticalState.toString(),
                                        )
                                    },
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Height,
                                    contentDescription = null,
                                )
                            },
                        )
                    }

                    SettingRow(
                        onReset = {
                            keyBorderWidthState = DEFAULT_KEY_BORDER_WIDTH
                            updateAppearance()
                        },
                    ) {
                        IntStepperPreference(
                            value = keyBorderWidthState,
                            onValueChange = {
                                keyBorderWidthState = it
                                updateAppearance()
                            },
                            valueRange = 0..50,
                            vibrateOnRepeat = vibrateOnHoldRepeatState,
                            repeatHapticType = vibrateHoldRepeatTypeState,
                            title = {
                                SettingTitle(
                                    text = stringResource(R.string.border_thickness),
                                    infoText = stringResource(R.string.border_thickness_info),
                                )
                            },
                            summary = {
                                Text(
                                    if (keyBorderWidthState == 0) {
                                        stringResource(R.string.border_thickness_summary_none)
                                    } else {
                                        stringResource(
                                            R.string.border_thickness_summary,
                                            tenthsOfDpLabel(keyBorderWidthState),
                                        )
                                    },
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.BorderOuter,
                                    contentDescription = null,
                                )
                            },
                        )
                    }

                    SettingRow(
                        onReset = {
                            keyRadiusState = DEFAULT_KEY_RADIUS
                            updateAppearance()
                        },
                    ) {
                        IntStepperPreference(
                            value = keyRadiusState,
                            onValueChange = {
                                keyRadiusState = it
                                updateAppearance()
                            },
                            valueRange = 0..100,
                            vibrateOnRepeat = vibrateOnHoldRepeatState,
                            repeatHapticType = vibrateHoldRepeatTypeState,
                            title = {
                                Text(stringResource(R.string.corner_roundness))
                            },
                            summary = {
                                Text(
                                    if (keyRadiusState == 0) {
                                        stringResource(R.string.corner_roundness_summary_none)
                                    } else {
                                        stringResource(R.string.corner_roundness_summary, keyRadiusState.toString())
                                    },
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.RoundedCorner,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                    }

                    SettingsSection(
                        title = stringResource(R.string.settings_section_layers)                    ) {
                        namedLayout.availableLayers().forEachIndexed { index, layer ->
                            key(layer) {
                                LayerHeightRow(
                                    layer = layer,
                                    namedLayout = namedLayout,
                                    overrides = layerHeightOverrides,
                                    showInfo = index == 0,
                                    vibrateOnRepeat = vibrateOnHoldRepeatState,
                                    repeatHapticType = vibrateHoldRepeatTypeState,
                                    onOverridesChange = { next ->
                                        layerHeightsState = formatLayerHeightOverrides(next)
                                        updateAppearance()
                                    },
                                )
                            }
                        }
                    }

                    SettingsSection(
                        title = stringResource(R.string.settings_section_feedback)                    ) {
                    HapticChannelPreference(
                        enabled = vibrateOnTapState,
                        type = vibrateTapTypeState,
                        onEnabledChange = {
                            vibrateOnTapState = it
                            updateAppearance()
                        },
                        onTypeChange = {
                            vibrateTapTypeState = it
                            updateAppearance()
                        },
                        defaultType = hapticTypeFromDb(DEFAULT_VIBRATE_TAP_TYPE),
                        title = R.string.vibrate_on_tap,
                        onSummary = R.string.vibrate_on_tap_on,
                        offSummary = R.string.vibrate_on_tap_off,
                        info = R.string.vibrate_on_tap_info,
                        typeTitle = R.string.vibrate_tap_type,
                        typeSummary = R.string.vibrate_tap_type_summary,
                        icon = Icons.Outlined.Vibration,
                    )
                    HapticChannelPreference(
                        enabled = vibrateOnSwipeState,
                        type = vibrateSwipeTypeState,
                        onEnabledChange = {
                            vibrateOnSwipeState = it
                            updateAppearance()
                        },
                        onTypeChange = {
                            vibrateSwipeTypeState = it
                            updateAppearance()
                        },
                        defaultType = hapticTypeFromDb(DEFAULT_VIBRATE_SWIPE_TYPE),
                        title = R.string.vibrate_on_swipe,
                        onSummary = R.string.vibrate_on_swipe_on,
                        offSummary = R.string.vibrate_on_swipe_off,
                        info = R.string.vibrate_on_swipe_info,
                        typeTitle = R.string.vibrate_swipe_type,
                        typeSummary = R.string.vibrate_swipe_type_summary,
                        icon = Icons.Outlined.Swipe,
                    )
                    HapticChannelPreference(
                        enabled = vibrateOnSlideState,
                        type = vibrateSlideTypeState,
                        onEnabledChange = {
                            vibrateOnSlideState = it
                            updateAppearance()
                        },
                        onTypeChange = {
                            vibrateSlideTypeState = it
                            updateAppearance()
                        },
                        defaultType = hapticTypeFromDb(DEFAULT_VIBRATE_SLIDE_TYPE),
                        title = R.string.vibrate_on_slide,
                        onSummary = R.string.vibrate_on_slide_on,
                        offSummary = R.string.vibrate_on_slide_off,
                        info = R.string.vibrate_on_slide_info,
                        typeTitle = R.string.vibrate_slide_type,
                        typeSummary = R.string.vibrate_slide_type_summary,
                        icon = Icons.Outlined.LinearScale,
                    )
                    HapticChannelPreference(
                        enabled = vibrateOnHoldRepeatState,
                        type = vibrateHoldRepeatTypeState,
                        onEnabledChange = {
                            vibrateOnHoldRepeatState = it
                            updateAppearance()
                        },
                        onTypeChange = {
                            vibrateHoldRepeatTypeState = it
                            updateAppearance()
                        },
                        defaultType = hapticTypeFromDb(DEFAULT_VIBRATE_HOLD_REPEAT_TYPE),
                        title = R.string.vibrate_on_hold_repeat,
                        onSummary = R.string.vibrate_on_hold_repeat_on,
                        offSummary = R.string.vibrate_on_hold_repeat_off,
                        info = R.string.vibrate_on_hold_repeat_info,
                        typeTitle = R.string.vibrate_hold_repeat_type,
                        typeSummary = R.string.vibrate_hold_repeat_type_summary,
                        icon = Icons.Outlined.Repeat,
                    )
                    HapticChannelPreference(
                        enabled = vibrateOnModifierState,
                        type = vibrateModifierTypeState,
                        onEnabledChange = {
                            vibrateOnModifierState = it
                            updateAppearance()
                        },
                        onTypeChange = {
                            vibrateModifierTypeState = it
                            updateAppearance()
                        },
                        defaultType = hapticTypeFromDb(DEFAULT_VIBRATE_MODIFIER_TYPE),
                        title = R.string.vibrate_on_modifier,
                        onSummary = R.string.vibrate_on_modifier_on,
                        offSummary = R.string.vibrate_on_modifier_off,
                        info = R.string.vibrate_on_modifier_info,
                        typeTitle = R.string.vibrate_modifier_type,
                        typeSummary = R.string.vibrate_modifier_type_summary,
                        icon = Icons.Outlined.KeyboardCapslock,
                    )
                    }

                    SettingsSection(
                        title = stringResource(R.string.settings_section_animations)                    ) {
                        SwitchPreference(
                            value = animationPressHighlightState,
                            onValueChange = {
                                animationPressHighlightState = it
                                updateAppearance()
                            },
                            title = {
                                SettingTitle(
                                    text = stringResource(R.string.animation_press_highlight),
                                    infoText = stringResource(R.string.animation_press_highlight_info),
                                )
                            },
                            summary = {
                                Text(
                                    stringResource(
                                        if (animationPressHighlightState) {
                                            R.string.animation_press_highlight_on
                                        } else {
                                            R.string.animation_press_highlight_off
                                        },
                                    ),
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Highlight,
                                    contentDescription = null,
                                )
                            },
                        )
                        SwitchPreference(
                            value = animationReleaseFlashState,
                            onValueChange = {
                                animationReleaseFlashState = it
                                updateAppearance()
                            },
                            title = {
                                SettingTitle(
                                    text = stringResource(R.string.animation_release_flash),
                                    infoText = stringResource(R.string.animation_release_flash_info),
                                )
                            },
                            summary = {
                                Text(
                                    stringResource(
                                        if (animationReleaseFlashState) {
                                            R.string.animation_release_flash_on
                                        } else {
                                            R.string.animation_release_flash_off
                                        },
                                    ),
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                )
                            },
                        )
                        SwitchPreference(
                            value = animationLetterDropState,
                            onValueChange = {
                                animationLetterDropState = it
                                updateAppearance()
                            },
                            title = {
                                SettingTitle(
                                    text = stringResource(R.string.animation_letter_drop),
                                    infoText = stringResource(R.string.animation_letter_drop_info),
                                )
                            },
                            summary = {
                                Text(
                                    stringResource(
                                        if (animationLetterDropState) {
                                            R.string.animation_letter_drop_on
                                        } else {
                                            R.string.animation_letter_drop_off
                                        },
                                    ),
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.South,
                                    contentDescription = null,
                                )
                            },
                        )
                    }

                    TestOutTextField()
                }
            }
        },
    )
}

@Composable
private fun HideLabelSwitch(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    @androidx.annotation.StringRes title: Int,
    @androidx.annotation.StringRes onSummary: Int,
    @androidx.annotation.StringRes offSummary: Int,
    icon: ImageVector,
    infoText: String? = null,
) {
    SwitchPreference(
        value = value,
        onValueChange = onValueChange,
        title = {
            SettingTitle(text = stringResource(title), infoText = infoText)
        },
        summary = {
            Text(stringResource(if (value) onSummary else offSummary))
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
    )
}

@Composable
private fun HideKeyGroupsPreference(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val selected = parseHideKeyCategories(value)
    var showDialog by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(selected) }

    SettingRow(
        onReset = { onValueChange(DEFAULT_HIDE_KEY_CATEGORIES) },
    ) {
        Preference(
            title = {
                SettingTitle(
                    text = stringResource(R.string.hide_key_groups),
                    infoText = stringResource(R.string.hide_key_groups_info),
                )
            },
            summary = {
                Text(hideKeyGroupsSummary(selected))
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.HideImage,
                    contentDescription = null,
                )
            },
            onClick = {
                draft = selected
                showDialog = true
            },
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.hide_key_groups)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    HIDE_KEY_GROUP_ORDER.forEach { category ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        draft = toggleHideKeyGroupSelection(draft, category)
                                    }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = category in draft,
                                onCheckedChange = null,
                            )
                            Text(
                                text = stringResource(category.hideGroupNameRes()),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(formatHideKeyCategories(draft))
                        showDialog = false
                    },
                ) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun hideKeyGroupsSummary(selected: Set<LegendCategory>): String {
    val ordered = HIDE_KEY_GROUP_ORDER.filter { it in selected }
    val names = ordered.map { stringResource(it.hideGroupNameRes()) }
    val groupsText =
        when {
            ordered.size == HIDE_KEY_GROUP_ORDER.size ->
                stringResource(R.string.hide_key_groups_all)
            names.size <= 1 -> names.firstOrNull().orEmpty()
            names.size == 2 ->
                stringResource(R.string.hide_key_groups_two, names[0], names[1])
            else ->
                stringResource(
                    R.string.hide_key_groups_many,
                    names.dropLast(1).joinToString(", "),
                    names.last(),
                )
        }
    return stringResource(R.string.hide_key_groups_summary, groupsText)
}

private fun LegendCategory.hideGroupNameRes(): Int =
    when (this) {
        LegendCategory.LETTER -> R.string.hide_group_letters
        LegendCategory.SYMBOL -> R.string.hide_group_symbols
        LegendCategory.NUMBER -> R.string.hide_group_numbers
        LegendCategory.MODIFIER -> R.string.hide_group_modifiers
        LegendCategory.LAYER_SWITCH -> R.string.hide_group_layer_switches
        LegendCategory.SPECIAL -> R.string.hide_group_specials
        LegendCategory.NAVIGATION -> R.string.hide_group_navigation
        LegendCategory.EDITING -> R.string.hide_group_editing
    }

@Composable
private fun LayerHeightRow(
    layer: LayoutLayer,
    namedLayout: NamedLayout,
    overrides: Map<LayoutLayer, Int>,
    showInfo: Boolean,
    vibrateOnRepeat: Boolean,
    repeatHapticType: HapticType,
    onOverridesChange: (Map<LayoutLayer, Int>) -> Unit,
) {
    val gridRows = namedLayout.gridRowCount(layer)
    val currentRows = namedLayout.heightRows(layer, overrides[layer] ?: 0)
    val extraRows = currentRows - gridRows

    SettingRow(
        onReset = {
            onOverridesChange(overrides - layer)
        },
    ) {
        IntStepperPreference(
            value = currentRows,
            onValueChange = { rows ->
                onOverridesChange(overrides + (layer to rows))
            },
            valueRange = gridRows..MAX_LAYER_HEIGHT_ROWS,
            vibrateOnRepeat = vibrateOnRepeat,
            repeatHapticType = repeatHapticType,
            title = {
                SettingTitle(
                    text = stringResource(layer.heightTitleRes()),
                    infoText = if (showInfo) stringResource(R.string.layer_height_info) else null,
                )
            },
            summary = {
                Text(
                    if (extraRows == 0) {
                        stringResource(R.string.layer_height_summary_flush, currentRows.toString())
                    } else {
                        stringResource(
                            R.string.layer_height_summary_extra,
                            currentRows.toString(),
                            extraRows.toString(),
                        )
                    },
                )
            },
            icon = {
                Icon(
                    imageVector = layer.heightIcon(),
                    contentDescription = null,
                )
            },
        )
    }
}

private fun LayoutLayer.heightTitleRes(): Int =
    when (this) {
        LayoutLayer.MAIN -> R.string.layer_height_main
        LayoutLayer.NUMERIC -> R.string.layer_height_numeric
        LayoutLayer.EMOJI -> R.string.layer_height_emoji
        LayoutLayer.CLIPBOARD -> R.string.layer_height_clipboard
    }

private fun LayoutLayer.heightIcon(): ImageVector =
    when (this) {
        LayoutLayer.MAIN -> Icons.Outlined.Keyboard
        LayoutLayer.NUMERIC -> Icons.Outlined.Numbers
        LayoutLayer.EMOJI -> Icons.Outlined.EmojiEmotions
        LayoutLayer.CLIPBOARD -> Icons.Outlined.History
    }

private fun tenthsOfDpLabel(tenths: Int): String {
    val whole = tenths / 10
    val frac = tenths % 10
    return if (frac == 0) whole.toString() else "$whole.$frac"
}
