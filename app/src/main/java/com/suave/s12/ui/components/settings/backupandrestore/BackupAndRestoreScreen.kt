package com.suave.s12.ui.components.settings.backupandrestore

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ResetTv
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.roomdbexportimport.RoomDBExportImport
import com.suave.s12.R
import com.suave.s12.db.AppDB
import com.suave.s12.db.AppSettings
import com.suave.s12.db.AppSettingsViewModel
import com.suave.s12.db.DEFAULT_ALT_AS_MODIFIER
import com.suave.s12.db.DEFAULT_BACKDROP_ENABLED
import com.suave.s12.db.DEFAULT_CLIPBOARD_AUTO_CLEANUP_ENABLED
import com.suave.s12.db.DEFAULT_CLIPBOARD_CLEANUP_AFTER_MINUTES
import com.suave.s12.db.DEFAULT_CLIPBOARD_HISTORY_ENABLED
import com.suave.s12.db.DEFAULT_CLIPBOARD_MAX_SIZE
import com.suave.s12.db.DEFAULT_CLIPBOARD_SIZE_LIMIT_ENABLED
import com.suave.s12.db.DEFAULT_CTRL_AS_MODIFIER
import com.suave.s12.db.DEFAULT_DISABLE_FULLSCREEN_EDITOR
import com.suave.s12.db.DEFAULT_DISTINCT_LETTER_CONTROL_COLORS
import com.suave.s12.db.DEFAULT_ESC_AS_MODIFIER
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
import com.suave.s12.db.DEFAULT_KEYBOARD_LAYOUT
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
import com.suave.s12.db.DEFAULT_SHOW_ON_SCREEN_KEYBOARD
import com.suave.s12.db.DEFAULT_SHOW_TOAST_ON_COPY
import com.suave.s12.db.DEFAULT_SHOW_TOAST_ON_CUT
import com.suave.s12.db.DEFAULT_SHOW_TOAST_ON_LAYOUT_SWITCH
import com.suave.s12.db.DEFAULT_THEME
import com.suave.s12.db.DEFAULT_THEME_COLOR
import com.suave.s12.db.DEFAULT_USE_PRIVATE_CLIPBOARD
import com.suave.s12.db.DEFAULT_CAPTURE_SYSTEM_CLIPBOARD
import com.suave.s12.db.DEFAULT_VIBRATE_ON_HOLD_REPEAT
import com.suave.s12.db.DEFAULT_VIBRATE_ON_SLIDE
import com.suave.s12.db.DEFAULT_VIBRATE_ON_TAP
import com.suave.s12.layout.DEFAULT_LAYER_HEIGHTS
import com.suave.s12.utils.SimpleTopAppBar
import com.suave.s12.utils.keyboardLayoutsSetFromDbIndexString
import com.suave.s12.utils.updateLayouts
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndRestoreScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
) {
    Log.d("thumb key", "Got to Backup and Restore screen")

    val ctx = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showConfirmResetDialog by remember { mutableStateOf(false) }

    val dbSavedText = stringResource(R.string.database_backed_up)
    val dbRestoredText = stringResource(R.string.database_restored)

    val dbHelper = RoomDBExportImport(AppDB.getDatabase(ctx).openHelper)

    val exportDbLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip"),
        ) {
            it?.also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    dbHelper.export(ctx, it)
                    Toast.makeText(ctx, dbSavedText, Toast.LENGTH_SHORT).show()
                }
            }
        }

    val importDbLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) {
            it?.also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    dbHelper.import(ctx, it, true)
                    Toast.makeText(ctx, dbRestoredText, Toast.LENGTH_SHORT).show()
                }
            }
        }

    if (showConfirmResetDialog) {
        AlertDialog(
            onDismissRequest = {
                showConfirmResetDialog = false
            },
            title = {
                Text(stringResource(R.string.reset_to_defaults))
            },
            text = {
                Text(stringResource(R.string.reset_to_defaults_msg))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmResetDialog = false
                        resetAppSettingsToDefault(
                            appSettingsViewModel,
                        )
                    },
                ) {
                    Text(stringResource(R.string.reset_to_defaults_confirm))
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showConfirmResetDialog = false
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleTopAppBar(text = stringResource(R.string.backup_and_restore), navController = navController)
        },
        content = { padding ->
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(padding),
            ) {
                ProvidePreferenceTheme {
                    Preference(
                        title = { Text(stringResource(R.string.backup_database)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Save,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            exportDbLauncher.launch("thumb-key")
                        },
                    )
                    Preference(
                        title = { Text(stringResource(R.string.restore_database)) },
                        summary = {
                            Text(stringResource(R.string.restore_database_warning))
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Restore,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            importDbLauncher.launch(arrayOf("application/zip"))
                        },
                    )
                    Preference(
                        title = {
                            Text(stringResource(R.string.reset_to_defaults))
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.ResetTv,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            showConfirmResetDialog = true
                        },
                    )
                }
            }
        },
    )
}

private fun resetAppSettingsToDefault(appSettingsViewModel: AppSettingsViewModel) {
    val layoutsDefault = keyboardLayoutsSetFromDbIndexString(DEFAULT_KEYBOARD_LAYOUT.toString())
    updateLayouts(appSettingsViewModel, layoutsDefault)

    appSettingsViewModel.update(
        AppSettings(
            id = 1,
            theme = DEFAULT_THEME,
            themeColor = DEFAULT_THEME_COLOR,
            hideLetters = DEFAULT_HIDE_LETTERS,
            hideSymbols = DEFAULT_HIDE_SYMBOLS,
            hideNumbers = DEFAULT_HIDE_NUMBERS,
            hideModifiers = DEFAULT_HIDE_MODIFIERS,
            hideLayerSwitches = DEFAULT_HIDE_LAYER_SWITCHES,
            hideSpecials = DEFAULT_HIDE_SPECIALS,
            hideNavigation = DEFAULT_HIDE_NAVIGATION,
            hideEditing = DEFAULT_HIDE_EDITING,
            hideKeyCategories = DEFAULT_HIDE_KEY_CATEGORIES,
            ignoreBottomPadding = DEFAULT_IGNORE_BOTTOM_PADDING,
            disableFullscreenEditor = DEFAULT_DISABLE_FULLSCREEN_EDITOR,
            keyHeight = DEFAULT_KEY_HEIGHT,
            layerHeights = DEFAULT_LAYER_HEIGHTS,
            vibrateOnTap = DEFAULT_VIBRATE_ON_TAP,
            vibrateOnSlide = DEFAULT_VIBRATE_ON_SLIDE,
            vibrateOnHoldRepeat = DEFAULT_VIBRATE_ON_HOLD_REPEAT,
            minSwipeLength = DEFAULT_MIN_SWIPE_LENGTH,
            escAsModifier = DEFAULT_ESC_AS_MODIFIER,
            ctrlAsModifier = DEFAULT_CTRL_AS_MODIFIER,
            altAsModifier = DEFAULT_ALT_AS_MODIFIER,
            shiftAsModifier = DEFAULT_SHIFT_AS_MODIFIER,
            keyboardLayout = DEFAULT_KEYBOARD_LAYOUT,
            keyboardLayouts = setOf(DEFAULT_KEYBOARD_LAYOUT).joinToString(),
            showToastOnLayoutSwitch = DEFAULT_SHOW_TOAST_ON_LAYOUT_SWITCH,
            showToastOnCopy = DEFAULT_SHOW_TOAST_ON_COPY,
            showToastOnCut = DEFAULT_SHOW_TOAST_ON_CUT,
            position = DEFAULT_POSITION,
            lastVersionCodeViewed = appSettingsViewModel.appSettings.value?.lastVersionCodeViewed ?: 0,
            clipboardHistoryEnabled = DEFAULT_CLIPBOARD_HISTORY_ENABLED,
            clipboardAutoCleanupEnabled = DEFAULT_CLIPBOARD_AUTO_CLEANUP_ENABLED,
            clipboardCleanupAfterMinutes = DEFAULT_CLIPBOARD_CLEANUP_AFTER_MINUTES,
            clipboardSizeLimitEnabled = DEFAULT_CLIPBOARD_SIZE_LIMIT_ENABLED,
            clipboardMaxSize = DEFAULT_CLIPBOARD_MAX_SIZE,
            usePrivateClipboard = DEFAULT_USE_PRIVATE_CLIPBOARD,
            captureSystemClipboard = DEFAULT_CAPTURE_SYSTEM_CLIPBOARD,
            showOnScreenKeyboard = DEFAULT_SHOW_ON_SCREEN_KEYBOARD,
            showDebugBar = DEFAULT_SHOW_DEBUG_BAR,
            backdropEnabled = DEFAULT_BACKDROP_ENABLED,
            keyPadding = DEFAULT_KEY_PADDING,
            keyPaddingVertical = DEFAULT_KEY_PADDING_VERTICAL,
            keyBorderWidth = DEFAULT_KEY_BORDER_WIDTH,
            keyRadius = DEFAULT_KEY_RADIUS,
            pushupSize = DEFAULT_PUSHUP_SIZE,
            distinctLetterControlColors = DEFAULT_DISTINCT_LETTER_CONTROL_COLORS,
        ),
    )
}
