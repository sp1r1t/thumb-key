package com.suave.s12.ui.components.settings.lookandfeel

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BorderBottom
import androidx.compose.material.icons.outlined.BorderOuter
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Crop75
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Padding
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RoundedCorner
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material.icons.outlined.WebAssetOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation.NavController
import com.suave.s12.R
import com.suave.s12.db.AppSettingsViewModel
import com.suave.s12.db.DEFAULT_BACKDROP_ENABLED
import com.suave.s12.db.DEFAULT_DISABLE_FULLSCREEN_EDITOR
import com.suave.s12.db.DEFAULT_HIDE_LETTERS
import com.suave.s12.db.DEFAULT_IGNORE_BOTTOM_PADDING
import com.suave.s12.db.DEFAULT_KEY_BORDER_WIDTH
import com.suave.s12.db.DEFAULT_KEY_HEIGHT
import com.suave.s12.db.DEFAULT_KEY_PADDING
import com.suave.s12.db.DEFAULT_KEY_RADIUS
import com.suave.s12.db.DEFAULT_PUSHUP_SIZE
import com.suave.s12.db.DEFAULT_THEME
import com.suave.s12.db.DEFAULT_THEME_COLOR
import com.suave.s12.db.DEFAULT_VIBRATE_ON_SLIDE
import com.suave.s12.db.DEFAULT_VIBRATE_ON_TAP
import com.suave.s12.db.LookAndFeelUpdate
import com.suave.s12.layout.BuiltinLayouts
import com.suave.s12.layout.DEFAULT_LAYER_HEIGHTS
import com.suave.s12.layout.LayoutLayer
import com.suave.s12.layout.MAX_LAYER_HEIGHT_ROWS
import com.suave.s12.layout.NamedLayout
import com.suave.s12.layout.formatLayerHeightOverrides
import com.suave.s12.layout.parseLayerHeightOverrides
import com.suave.s12.ui.components.common.IntStepperPreference
import com.suave.s12.ui.components.common.SettingRow
import com.suave.s12.ui.components.common.TestOutTextField
import com.suave.s12.ui.components.settings.about.SettingsDivider
import com.suave.s12.utils.SimpleTopAppBar
import com.suave.s12.utils.TAG
import com.suave.s12.utils.ThemeColor
import com.suave.s12.utils.ThemeMode
import com.suave.s12.utils.toBool
import com.suave.s12.utils.toInt
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.ProvidePreferenceTheme
import me.zhanghai.compose.preference.SwitchPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LookAndFeelScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
) {
    Log.d(TAG, "Got to lookAndFeel activity")

    val resources = LocalResources.current
    val settings by appSettingsViewModel.appSettings.observeAsState()
    var themeState = ThemeMode.entries[settings?.theme ?: DEFAULT_THEME]
    var themeColorState = ThemeColor.entries[settings?.themeColor ?: DEFAULT_THEME_COLOR]
    var keyHeightState = settings?.keyHeight ?: DEFAULT_KEY_HEIGHT

    var vibrateOnTapState = (settings?.vibrateOnTap ?: DEFAULT_VIBRATE_ON_TAP).toBool()
    var vibrateOnSlideState = (settings?.vibrateOnSlide ?: DEFAULT_VIBRATE_ON_SLIDE).toBool()
    var hideLettersState = (settings?.hideLetters ?: DEFAULT_HIDE_LETTERS).toBool()
    var ignoreBottomPaddingState = (settings?.ignoreBottomPadding ?: DEFAULT_IGNORE_BOTTOM_PADDING).toBool()
    var disableFullscreenEditorState = (settings?.disableFullscreenEditor ?: DEFAULT_DISABLE_FULLSCREEN_EDITOR).toBool()
    var backdropEnabledState = (settings?.backdropEnabled ?: DEFAULT_BACKDROP_ENABLED).toBool()
    var keyPaddingState = settings?.keyPadding ?: DEFAULT_KEY_PADDING
    var keyBorderWidthState = settings?.keyBorderWidth ?: DEFAULT_KEY_BORDER_WIDTH
    var keyRadiusState = settings?.keyRadius ?: DEFAULT_KEY_RADIUS
    var pushupSizeState = settings?.pushupSize ?: DEFAULT_PUSHUP_SIZE
    var layerHeightsState = settings?.layerHeights ?: DEFAULT_LAYER_HEIGHTS
    val namedLayout = BuiltinLayouts.byIndex(settings?.keyboardLayout ?: 0)
    val layerHeightOverrides = parseLayerHeightOverrides(layerHeightsState)

    fun updateLookAndFeel() {
        appSettingsViewModel.updateLookAndFeel(
            LookAndFeelUpdate(
                id = 1,
                vibrateOnTap = vibrateOnTapState.toInt(),
                vibrateOnSlide = vibrateOnSlideState.toInt(),
                hideLetters = hideLettersState.toInt(),
                ignoreBottomPadding = ignoreBottomPaddingState.toInt(),
                theme = themeState.ordinal,
                themeColor = themeColorState.ordinal,
                keyHeight = keyHeightState,
                layerHeights = layerHeightsState,
                disableFullscreenEditor = disableFullscreenEditorState.toInt(),
                backdropEnabled = backdropEnabledState.toInt(),
                keyPadding = keyPaddingState,
                keyBorderWidth = keyBorderWidthState,
                keyRadius = keyRadiusState,
                pushupSize = pushupSizeState,
            ),
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleTopAppBar(text = stringResource(R.string.look_and_feel), navController = navController)
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
                    SettingRow(onReset = {
                        themeState = ThemeMode.entries[DEFAULT_THEME]
                        updateLookAndFeel()
                    }) {
                        ListPreference(
                            type = ListPreferenceType.DROPDOWN_MENU,
                            value = themeState,
                            onValueChange = {
                                themeState = it
                                updateLookAndFeel()
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
                        updateLookAndFeel()
                    }) {
                        ListPreference(
                            type = ListPreferenceType.DROPDOWN_MENU,
                            value = themeColorState,
                            onValueChange = {
                                themeColorState = it
                                updateLookAndFeel()
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

                    SettingsDivider()

                    SettingRow {
                        SwitchPreference(
                            value = hideLettersState,
                            onValueChange = {
                                hideLettersState = it
                                updateLookAndFeel()
                            },
                            title = {
                                Text(stringResource(R.string.hide_letters))
                            },
                            summary = {
                                Text(stringResource(if (hideLettersState) R.string.hide_letters_on else R.string.hide_letters_off))
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.HideImage,
                                    contentDescription = null,
                                )
                            },
                        )
                    }

                    SettingRow {
                        SwitchPreference(
                            value = backdropEnabledState,
                            onValueChange = {
                                backdropEnabledState = it
                                updateLookAndFeel()
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
                                updateLookAndFeel()
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
                        infoText = stringResource(R.string.raise_from_bottom_info),
                        onReset = {
                            pushupSizeState = DEFAULT_PUSHUP_SIZE
                            updateLookAndFeel()
                        },
                    ) {
                        IntStepperPreference(
                            value = pushupSizeState,
                            onValueChange = {
                                pushupSizeState = it
                                updateLookAndFeel()
                            },
                            valueRange = 0..250,
                            title = {
                                Text(stringResource(R.string.raise_from_bottom))
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

                    SettingRow(infoText = stringResource(R.string.disable_fullscreen_editor_info)) {
                        SwitchPreference(
                            value = disableFullscreenEditorState,
                            onValueChange = {
                                disableFullscreenEditorState = it
                                updateLookAndFeel()
                            },
                            title = {
                                Text(stringResource(R.string.disable_fullscreen_editor))
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

                    SettingRow(
                        onReset = {
                            keyHeightState = DEFAULT_KEY_HEIGHT
                            updateLookAndFeel()
                        },
                    ) {
                        IntStepperPreference(
                            value = keyHeightState,
                            onValueChange = {
                                keyHeightState = it
                                updateLookAndFeel()
                            },
                            valueRange = 10..200,
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

                    namedLayout.availableLayers().forEachIndexed { index, layer ->
                        key(layer) {
                            LayerHeightRow(
                                layer = layer,
                                namedLayout = namedLayout,
                                overrides = layerHeightOverrides,
                                showInfo = index == 0,
                                onOverridesChange = { next ->
                                    layerHeightsState = formatLayerHeightOverrides(next)
                                    updateLookAndFeel()
                                },
                            )
                        }
                    }

                    SettingRow(
                        onReset = {
                            keyPaddingState = DEFAULT_KEY_PADDING
                            updateLookAndFeel()
                        },
                    ) {
                        IntStepperPreference(
                            value = keyPaddingState,
                            onValueChange = {
                                keyPaddingState = it
                                updateLookAndFeel()
                            },
                            valueRange = 0..10,
                            title = {
                                Text(stringResource(R.string.key_spacing))
                            },
                            summary = {
                                Text(
                                    if (keyPaddingState == 0) {
                                        stringResource(R.string.key_spacing_summary_none)
                                    } else {
                                        stringResource(R.string.key_spacing_summary, keyPaddingState.toString())
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
                        infoText = stringResource(R.string.border_thickness_info),
                        onReset = {
                            keyBorderWidthState = DEFAULT_KEY_BORDER_WIDTH
                            updateLookAndFeel()
                        },
                    ) {
                        IntStepperPreference(
                            value = keyBorderWidthState,
                            onValueChange = {
                                keyBorderWidthState = it
                                updateLookAndFeel()
                            },
                            valueRange = 0..50,
                            title = {
                                Text(stringResource(R.string.border_thickness))
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
                            updateLookAndFeel()
                        },
                    ) {
                        IntStepperPreference(
                            value = keyRadiusState,
                            onValueChange = {
                                keyRadiusState = it
                                updateLookAndFeel()
                            },
                            valueRange = 0..100,
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

                    SettingsDivider()

                    SettingRow(infoText = stringResource(R.string.vibrate_on_tap_info)) {
                        SwitchPreference(
                            value = vibrateOnTapState,
                            onValueChange = {
                                vibrateOnTapState = it
                                updateLookAndFeel()
                            },
                            title = {
                                Text(stringResource(R.string.vibrate_on_tap))
                            },
                            summary = {
                                Text(stringResource(if (vibrateOnTapState) R.string.vibrate_on_tap_on else R.string.vibrate_on_tap_off))
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Vibration,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                    SettingRow(infoText = stringResource(R.string.vibrate_on_slide_info)) {
                        SwitchPreference(
                            value = vibrateOnSlideState,
                            onValueChange = {
                                vibrateOnSlideState = it
                                updateLookAndFeel()
                            },
                            title = {
                                Text(stringResource(R.string.vibrate_on_slide))
                            },
                            summary = {
                                Text(
                                    stringResource(
                                        if (vibrateOnSlideState) R.string.vibrate_on_slide_on else R.string.vibrate_on_slide_off,
                                    ),
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.LinearScale,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                    SettingsDivider()
                    TestOutTextField()
                }
            }
        },
    )
}

@Composable
private fun LayerHeightRow(
    layer: LayoutLayer,
    namedLayout: NamedLayout,
    overrides: Map<LayoutLayer, Int>,
    showInfo: Boolean,
    onOverridesChange: (Map<LayoutLayer, Int>) -> Unit,
) {
    val gridRows = namedLayout.gridRowCount(layer)
    val currentRows = namedLayout.heightRows(layer, overrides[layer] ?: 0)
    val extraRows = currentRows - gridRows

    SettingRow(
        infoText = if (showInfo) stringResource(R.string.layer_height_info) else null,
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
            title = {
                Text(stringResource(layer.heightTitleRes()))
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
    }

private fun LayoutLayer.heightIcon(): ImageVector =
    when (this) {
        LayoutLayer.MAIN -> Icons.Outlined.Keyboard
        LayoutLayer.NUMERIC -> Icons.Outlined.Numbers
        LayoutLayer.EMOJI -> Icons.Outlined.EmojiEmotions
    }

private fun tenthsOfDpLabel(tenths: Int): String {
    val whole = tenths / 10
    val frac = tenths % 10
    return if (frac == 0) whole.toString() else "$whole.$frac"
}
