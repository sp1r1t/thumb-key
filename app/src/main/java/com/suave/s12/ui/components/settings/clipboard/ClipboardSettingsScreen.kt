package com.suave.s12.ui.components.settings.clipboard

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DataArray
import androidx.compose.material.icons.outlined.DiscFull
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.suave.s12.R
import com.suave.s12.db.AppSettingsViewModel
import com.suave.s12.db.ClipboardRepository
import com.suave.s12.db.ClipboardSettingsUpdate
import com.suave.s12.db.DEFAULT_CLIPBOARD_AUTO_CLEANUP_ENABLED
import com.suave.s12.db.DEFAULT_CLIPBOARD_CLEANUP_AFTER_MINUTES
import com.suave.s12.db.DEFAULT_CLIPBOARD_HISTORY_ENABLED
import com.suave.s12.db.DEFAULT_CLIPBOARD_MAX_SIZE
import com.suave.s12.db.DEFAULT_CLIPBOARD_SIZE_LIMIT_ENABLED
import com.suave.s12.db.DEFAULT_USE_PRIVATE_CLIPBOARD
import com.suave.s12.db.MAX_CLIPBOARD_MAX_SIZE
import com.suave.s12.db.MIN_CLIPBOARD_MAX_SIZE
import com.suave.s12.ui.components.common.SettingRow
import com.suave.s12.utils.SimpleTopAppBar
import com.suave.s12.utils.TAG
import com.suave.s12.utils.toBool
import com.suave.s12.utils.toInt
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.ProvidePreferenceTheme
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.SwitchPreference

enum class CleanupDuration(
    val minutes: Int,
    val displayNameResId: Int,
) {
    MINUTES_5(5, R.string.duration_5_minutes),
    MINUTES_10(10, R.string.duration_10_minutes),
    MINUTES_30(30, R.string.duration_30_minutes),
    HOURS_1(60, R.string.duration_1_hour),
    HOURS_2(2 * 60, R.string.duration_2_hours),
    HOURS_4(4 * 60, R.string.duration_4_hours),
    HOURS_12(12 * 60, R.string.duration_12_hours),
    DAYS_1(24 * 60, R.string.duration_1_day),
    DAYS_2(2 * 24 * 60, R.string.duration_2_days),
    DAYS_4(4 * 24 * 60, R.string.duration_4_days),
    DAYS_7(7 * 24 * 60, R.string.duration_7_days),
    ;

    companion object {
        fun fromIndex(index: Int): CleanupDuration = entries.getOrElse(index) { HOURS_2 }

        fun fromMinutes(minutes: Int): CleanupDuration = entries.find { it.minutes == minutes } ?: HOURS_2
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardSettingsScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
    clipboardRepository: ClipboardRepository?,
) {
    Log.d(TAG, "Got to clipboard settings activity")

    val settings by appSettingsViewModel.appSettings.observeAsState()
    val scope = rememberCoroutineScope()

    var clipboardHistoryEnabledState =
        (settings?.clipboardHistoryEnabled ?: DEFAULT_CLIPBOARD_HISTORY_ENABLED).toBool()
    var clipboardAutoCleanupEnabledState =
        (settings?.clipboardAutoCleanupEnabled ?: DEFAULT_CLIPBOARD_AUTO_CLEANUP_ENABLED).toBool()
    val currentCleanupMinutes = settings?.clipboardCleanupAfterMinutes ?: DEFAULT_CLIPBOARD_CLEANUP_AFTER_MINUTES
    var clipboardCleanupDuration = CleanupDuration.fromMinutes(currentCleanupMinutes)
    var clipboardCleanupSliderState by remember {
        mutableFloatStateOf(clipboardCleanupDuration.ordinal.toFloat())
    }
    var clipboardSizeLimitEnabledState =
        (settings?.clipboardSizeLimitEnabled ?: DEFAULT_CLIPBOARD_SIZE_LIMIT_ENABLED).toBool()
    // Float is required because SliderPreference uses float values for the slider position
    var clipboardMaxSizeState =
        (settings?.clipboardMaxSize ?: DEFAULT_CLIPBOARD_MAX_SIZE).toFloat()
    var clipboardMaxSizeSliderState by remember { mutableFloatStateOf(clipboardMaxSizeState) }
    var usePrivateClipboardState =
        (settings?.usePrivateClipboard ?: DEFAULT_USE_PRIVATE_CLIPBOARD).toBool()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    fun updateClipboardSettings() {
        appSettingsViewModel.updateClipboardSettings(
            ClipboardSettingsUpdate(
                id = 1,
                clipboardHistoryEnabled = clipboardHistoryEnabledState.toInt(),
                clipboardAutoCleanupEnabled = clipboardAutoCleanupEnabledState.toInt(),
                clipboardCleanupAfterMinutes = clipboardCleanupDuration.minutes,
                clipboardSizeLimitEnabled = clipboardSizeLimitEnabledState.toInt(),
                clipboardMaxSize = clipboardMaxSizeState.toInt(),
                usePrivateClipboard = usePrivateClipboardState.toInt(),
            ),
        )
        // Enforce size limit after updating settings
        scope.launch {
            clipboardRepository?.enforceSizeLimit()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleTopAppBar(
                text = stringResource(R.string.clipboard_history),
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
                            value = clipboardHistoryEnabledState,
                            onValueChange = {
                                clipboardHistoryEnabledState = it
                                updateClipboardSettings()
                            },
                            title = {
                                Text(stringResource(R.string.clipboard_history_enabled))
                            },
                            summary = {
                                Text(
                                    stringResource(
                                        if (clipboardHistoryEnabledState) {
                                            R.string.clipboard_history_enabled_on
                                        } else {
                                            R.string.clipboard_history_enabled_off
                                        },
                                    ),
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                    SettingRow {
                        SwitchPreference(
                            value = clipboardAutoCleanupEnabledState,
                            onValueChange = {
                                clipboardAutoCleanupEnabledState = it
                                updateClipboardSettings()
                            },
                            enabled = clipboardHistoryEnabledState,
                            title = {
                                Text(stringResource(R.string.clipboard_auto_cleanup))
                            },
                            summary = {
                                Text(
                                    stringResource(
                                        if (clipboardAutoCleanupEnabledState) {
                                            R.string.clipboard_auto_cleanup_on
                                        } else {
                                            R.string.clipboard_auto_cleanup_off
                                        },
                                    ),
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.CleaningServices,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                    SettingRow(
                        onReset = {
                            clipboardCleanupDuration = CleanupDuration.fromMinutes(DEFAULT_CLIPBOARD_CLEANUP_AFTER_MINUTES)
                            clipboardCleanupSliderState = clipboardCleanupDuration.ordinal.toFloat()
                            updateClipboardSettings()
                        },
                    ) {
                        SliderPreference(
                            value = clipboardCleanupDuration.ordinal.toFloat(),
                            sliderValue = clipboardCleanupSliderState,
                            onValueChange = {
                                clipboardCleanupDuration = CleanupDuration.fromIndex(it.toInt())
                                updateClipboardSettings()
                            },
                            onSliderValueChange = { clipboardCleanupSliderState = it },
                            valueRange = 0f..(CleanupDuration.entries.size - 1).toFloat(),
                            valueSteps = CleanupDuration.entries.size - 2,
                            enabled = clipboardHistoryEnabledState && clipboardAutoCleanupEnabledState,
                            title = {
                                Text(stringResource(R.string.clipboard_cleanup_after))
                            },
                            summary = {
                                val duration = CleanupDuration.fromIndex(clipboardCleanupSliderState.toInt())
                                Text(
                                    stringResource(
                                        R.string.clipboard_cleanup_after_summary,
                                        stringResource(duration.displayNameResId),
                                    ),
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.HourglassTop,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                    SettingRow {
                        SwitchPreference(
                            value = clipboardSizeLimitEnabledState,
                            onValueChange = {
                                clipboardSizeLimitEnabledState = it
                                updateClipboardSettings()
                            },
                            enabled = clipboardHistoryEnabledState,
                            title = {
                                Text(stringResource(R.string.clipboard_size_limit))
                            },
                            summary = {
                                Text(
                                    stringResource(
                                        if (clipboardSizeLimitEnabledState) {
                                            R.string.clipboard_size_limit_on
                                        } else {
                                            R.string.clipboard_size_limit_off
                                        },
                                    ),
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.DiscFull,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                    SettingRow(
                        onReset = {
                            clipboardMaxSizeState = DEFAULT_CLIPBOARD_MAX_SIZE.toFloat()
                            clipboardMaxSizeSliderState = DEFAULT_CLIPBOARD_MAX_SIZE.toFloat()
                            updateClipboardSettings()
                        },
                    ) {
                        SliderPreference(
                            value = clipboardMaxSizeState,
                            sliderValue = clipboardMaxSizeSliderState,
                            onValueChange = {
                                clipboardMaxSizeState = it
                                updateClipboardSettings()
                            },
                            onSliderValueChange = { clipboardMaxSizeSliderState = it },
                            valueRange = MIN_CLIPBOARD_MAX_SIZE.toFloat()..MAX_CLIPBOARD_MAX_SIZE.toFloat(),
                            enabled = clipboardHistoryEnabledState && clipboardSizeLimitEnabledState,
                            title = {
                                Text(stringResource(R.string.clipboard_max_size))
                            },
                            summary = {
                                Text(stringResource(R.string.clipboard_max_size_summary, clipboardMaxSizeSliderState.toInt()))
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.DataArray,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                    SettingRow(infoText = stringResource(R.string.use_private_clipboard_info)) {
                        SwitchPreference(
                            value = usePrivateClipboardState,
                            onValueChange = {
                                usePrivateClipboardState = it
                                updateClipboardSettings()
                            },
                            enabled = clipboardHistoryEnabledState,
                            title = {
                                Text(stringResource(R.string.use_private_clipboard))
                            },
                            summary = {
                                Text(
                                    stringResource(
                                        if (usePrivateClipboardState) {
                                            R.string.use_private_clipboard_on
                                        } else {
                                            R.string.use_private_clipboard_off
                                        },
                                    ),
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.VisibilityOff,
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
