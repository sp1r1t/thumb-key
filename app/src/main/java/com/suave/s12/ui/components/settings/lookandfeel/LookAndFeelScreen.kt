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
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Crop75
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Vibration
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation.NavController
import com.suave.s12.R
import com.suave.s12.db.AppSettingsViewModel
import com.suave.s12.db.DEFAULT_DISABLE_FULLSCREEN_EDITOR
import com.suave.s12.db.DEFAULT_HIDE_LETTERS
import com.suave.s12.db.DEFAULT_IGNORE_BOTTOM_PADDING
import com.suave.s12.db.DEFAULT_KEY_HEIGHT
import com.suave.s12.db.DEFAULT_THEME
import com.suave.s12.db.DEFAULT_THEME_COLOR
import com.suave.s12.db.DEFAULT_VIBRATE_ON_SLIDE
import com.suave.s12.db.DEFAULT_VIBRATE_ON_TAP
import com.suave.s12.db.LookAndFeelUpdate
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
import me.zhanghai.compose.preference.SliderPreference
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
    var keyHeightState = (settings?.keyHeight ?: DEFAULT_KEY_HEIGHT).toFloat()
    var keyHeightSliderState by remember { mutableFloatStateOf(keyHeightState) }

    var vibrateOnTapState = (settings?.vibrateOnTap ?: DEFAULT_VIBRATE_ON_TAP).toBool()
    var vibrateOnSlideState = (settings?.vibrateOnSlide ?: DEFAULT_VIBRATE_ON_SLIDE).toBool()
    var hideLettersState = (settings?.hideLetters ?: DEFAULT_HIDE_LETTERS).toBool()
    var ignoreBottomPaddingState = (settings?.ignoreBottomPadding ?: DEFAULT_IGNORE_BOTTOM_PADDING).toBool()
    var disableFullscreenEditorState = (settings?.disableFullscreenEditor ?: DEFAULT_DISABLE_FULLSCREEN_EDITOR).toBool()

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
                keyHeight = keyHeightState.toInt(),
                disableFullscreenEditor = disableFullscreenEditorState.toInt(),
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
                            keyHeightState = DEFAULT_KEY_HEIGHT.toFloat()
                            keyHeightSliderState = DEFAULT_KEY_HEIGHT.toFloat()
                            updateLookAndFeel()
                        },
                    ) {
                        SliderPreference(
                            value = keyHeightState,
                            sliderValue = keyHeightSliderState,
                            onValueChange = {
                                keyHeightState = it
                                updateLookAndFeel()
                            },
                            onSliderValueChange = {
                                keyHeightSliderState = it
                            },
                            valueRange = 10f..200f,
                            title = {
                                Text(stringResource(R.string.key_height))
                            },
                            summary = {
                                Text(stringResource(R.string.key_height_summary, keyHeightSliderState.toInt().toString()))
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Crop75,
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
