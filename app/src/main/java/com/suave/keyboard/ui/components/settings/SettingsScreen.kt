package com.suave.keyboard.ui.components.settings

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.suave.keyboard.R
import com.suave.keyboard.db.AppSettingsViewModel
import com.suave.keyboard.ui.components.common.SettingsScreenBody
import com.suave.keyboard.utils.TAG
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
    suaveEnabled: Boolean,
    suaveSelected: Boolean,
) {
    Log.d(TAG, "Got to settings activity")

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
            )
        },
        content = { padding ->
            SettingsScreenBody(padding = padding) {
                ProvidePreferenceTheme {
                    if (!(suaveEnabled || suaveSelected)) {
                        Preference(
                            title = {
                                val setupStr = stringResource(R.string.setup)
                                Text(setupStr)
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.InstallMobile,
                                    contentDescription = null,
                                )
                            },
                            onClick = { navController.navigate("setup") },
                        )
                    }

                    Preference(
                        title = { Text(stringResource(R.string.appearance)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = null,
                            )
                        },
                        onClick = { navController.navigate("appearance") },
                    )
                    Preference(
                        title = { Text(stringResource(R.string.behavior)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.TouchApp,
                                contentDescription = null,
                            )
                        },
                        onClick = { navController.navigate("behavior") },
                    )
                    Preference(
                        title = { Text(stringResource(R.string.layouts)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Keyboard,
                                contentDescription = null,
                            )
                        },
                        onClick = { navController.navigate("layouts") },
                    )
                    Preference(
                        title = { Text(stringResource(R.string.settings_section_suggestions)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                            )
                        },
                        onClick = { navController.navigate("suggestionsSettings") },
                    )
                    Preference(
                        title = { Text(stringResource(R.string.clipboard_history)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.ContentPaste,
                                contentDescription = null,
                            )
                        },
                        onClick = { navController.navigate("clipboardSettings") },
                    )
                    Preference(
                        title = { Text(stringResource(R.string.backup_and_restore)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Restore,
                                contentDescription = null,
                            )
                        },
                        onClick = { navController.navigate("backupAndRestore") },
                    )
                    Preference(
                        title = { Text(stringResource(R.string.other)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Build,
                                contentDescription = null,
                            )
                        },
                        onClick = { navController.navigate("otherSettings") },
                    )
                    Preference(
                        title = { Text(stringResource(R.string.about)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                            )
                        },
                        onClick = { navController.navigate("about") },
                    )
                }
            }
        },
    )
}
