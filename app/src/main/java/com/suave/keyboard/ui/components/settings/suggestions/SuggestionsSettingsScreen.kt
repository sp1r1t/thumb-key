package com.suave.keyboard.ui.components.settings.suggestions

import android.os.Build
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.suave.keyboard.R
import com.suave.keyboard.db.AppSettingsViewModel
import com.suave.keyboard.db.DEFAULT_INLINE_SUGGESTIONS
import com.suave.keyboard.db.DEFAULT_INLINE_SUGGESTION_HEIGHT
import com.suave.keyboard.db.DEFAULT_VIBRATE_HOLD_REPEAT_TYPE
import com.suave.keyboard.db.DEFAULT_VIBRATE_ON_HOLD_REPEAT
import com.suave.keyboard.db.MAX_INLINE_SUGGESTION_HEIGHT
import com.suave.keyboard.db.MIN_INLINE_SUGGESTION_HEIGHT
import com.suave.keyboard.db.SuggestionsUpdate
import com.suave.keyboard.engine.feedback.hapticTypeFromDb
import com.suave.keyboard.ui.components.common.IntStepperPreference
import com.suave.keyboard.ui.components.common.SettingRow
import com.suave.keyboard.ui.components.common.SettingTitle
import com.suave.keyboard.ui.components.common.SettingsScreenBody
import com.suave.keyboard.utils.SimpleTopAppBar
import com.suave.keyboard.utils.TAG
import com.suave.keyboard.utils.toBool
import com.suave.keyboard.utils.toInt
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceTheme
import me.zhanghai.compose.preference.SwitchPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsSettingsScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
) {
    Log.d(TAG, "Got to suggestions settings activity")

    val settings by appSettingsViewModel.appSettings.observeAsState()

    var inlineSuggestionsState =
        (settings?.inlineSuggestions ?: DEFAULT_INLINE_SUGGESTIONS).toBool()
    var inlineSuggestionHeightState =
        settings?.inlineSuggestionHeight ?: DEFAULT_INLINE_SUGGESTION_HEIGHT
    val vibrateOnHoldRepeat =
        (settings?.vibrateOnHoldRepeat ?: DEFAULT_VIBRATE_ON_HOLD_REPEAT).toBool()
    val vibrateHoldRepeatType =
        hapticTypeFromDb(settings?.vibrateHoldRepeatType ?: DEFAULT_VIBRATE_HOLD_REPEAT_TYPE)

    val snackbarHostState = remember { SnackbarHostState() }

    fun updateSuggestions() {
        appSettingsViewModel.updateSuggestions(
            SuggestionsUpdate(
                id = 1,
                inlineSuggestions = inlineSuggestionsState.toInt(),
                inlineSuggestionHeight = inlineSuggestionHeightState,
            ),
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleTopAppBar(
                text = stringResource(R.string.settings_section_suggestions),
                navController = navController,
            )
        },
        content = { padding ->
            SettingsScreenBody(padding = padding) {
                ProvidePreferenceTheme {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        Preference(
                            title = {
                                Text(stringResource(R.string.inline_suggestions))
                            },
                            summary = {
                                Text(stringResource(R.string.inline_suggestions_requires_android_11))
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                )
                            },
                            enabled = false,
                            onClick = {},
                        )
                    } else {
                        SettingRow {
                            SwitchPreference(
                                value = inlineSuggestionsState,
                                onValueChange = {
                                    inlineSuggestionsState = it
                                    updateSuggestions()
                                },
                                title = {
                                    SettingTitle(
                                        text = stringResource(R.string.inline_suggestions),
                                        infoText = stringResource(R.string.inline_suggestions_info),
                                    )
                                },
                                summary = {
                                    Text(
                                        stringResource(
                                            if (inlineSuggestionsState) {
                                                R.string.inline_suggestions_on
                                            } else {
                                                R.string.inline_suggestions_off
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
                        }
                        if (inlineSuggestionsState) {
                            IntStepperPreference(
                                value = inlineSuggestionHeightState,
                                onValueChange = {
                                    inlineSuggestionHeightState = it
                                    updateSuggestions()
                                },
                                valueRange =
                                    MIN_INLINE_SUGGESTION_HEIGHT..MAX_INLINE_SUGGESTION_HEIGHT,
                                vibrateOnRepeat = vibrateOnHoldRepeat,
                                repeatHapticType = vibrateHoldRepeatType,
                                title = {
                                    SettingTitle(
                                        text = stringResource(R.string.inline_suggestion_height),
                                    )
                                },
                                summary = {
                                    Text(
                                        stringResource(
                                            R.string.inline_suggestion_height_summary,
                                            inlineSuggestionHeightState,
                                        ),
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Height,
                                        contentDescription = null,
                                    )
                                },
                                onReset = {
                                    inlineSuggestionHeightState = DEFAULT_INLINE_SUGGESTION_HEIGHT
                                    updateSuggestions()
                                },
                                resetTo = DEFAULT_INLINE_SUGGESTION_HEIGHT,
                            )
                        }
                    }
                }
            }
        },
    )
}
