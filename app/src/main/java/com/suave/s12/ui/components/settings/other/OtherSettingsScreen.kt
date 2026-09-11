package com.suave.s12.ui.components.settings.other

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Keyboard
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.suave.s12.R
import com.suave.s12.db.AppSettingsViewModel
import com.suave.s12.db.DEFAULT_SHOW_DEBUG_BAR
import com.suave.s12.db.DEFAULT_SHOW_ON_SCREEN_KEYBOARD
import com.suave.s12.db.OtherSettingsUpdate
import com.suave.s12.ui.components.common.SettingRow
import com.suave.s12.utils.SimpleTopAppBar
import com.suave.s12.utils.TAG
import com.suave.s12.utils.toBool
import com.suave.s12.utils.toInt
import me.zhanghai.compose.preference.ProvidePreferenceTheme
import me.zhanghai.compose.preference.SwitchPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherSettingsScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
) {
    Log.d(TAG, "Got to 'other' settings activity")

    val settings by appSettingsViewModel.appSettings.observeAsState()

    var showOnScreenKeyboardState =
        (settings?.showOnScreenKeyboard ?: DEFAULT_SHOW_ON_SCREEN_KEYBOARD).toBool()
    var showDebugBarState =
        (settings?.showDebugBar ?: DEFAULT_SHOW_DEBUG_BAR).toBool()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    fun updateOtherSettings() {
        appSettingsViewModel.updateOtherSettings(
            OtherSettingsUpdate(
                id = 1,
                showOnScreenKeyboard = showOnScreenKeyboardState.toInt(),
                showDebugBar = showDebugBarState.toInt(),
            ),
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleTopAppBar(
                text = stringResource(R.string.other),
                navController = navController,
            )
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
                    SettingRow {
                        SwitchPreference(
                            value = showOnScreenKeyboardState,
                            onValueChange = {
                                showOnScreenKeyboardState = it
                                updateOtherSettings()
                            },
                            title = {
                                Text(stringResource(R.string.show_on_screen_keyboard))
                            },
                            summary = {
                                Text(
                                    stringResource(
                                        if (showOnScreenKeyboardState) {
                                            R.string.show_on_screen_keyboard_on
                                        } else {
                                            R.string.show_on_screen_keyboard_off
                                        },
                                    ),
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Keyboard,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                    SettingRow(
                        infoText = stringResource(R.string.show_debug_bar_info),
                    ) {
                        SwitchPreference(
                            value = showDebugBarState,
                            onValueChange = {
                                showDebugBarState = it
                                updateOtherSettings()
                            },
                            title = {
                                Text(stringResource(R.string.show_debug_bar))
                            },
                            summary = {
                                Text(
                                    stringResource(
                                        if (showDebugBarState) {
                                            R.string.show_debug_bar_on
                                        } else {
                                            R.string.show_debug_bar_off
                                        },
                                    ),
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.BugReport,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                }
            }
        },
    )
}
