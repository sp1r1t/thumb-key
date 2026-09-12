package com.suave.s12.ui.components.settings.behavior

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Abc
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardControlKey
import androidx.compose.material.icons.outlined.KeyboardOptionKey
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SpaceBar
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.suave.s12.IMEService
import com.suave.s12.R
import com.suave.s12.db.AppSettingsViewModel
import com.suave.s12.db.BehaviorUpdate
import com.suave.s12.db.DEFAULT_ALT_AS_MODIFIER
import com.suave.s12.db.DEFAULT_AUTO_CAPITALIZE
import com.suave.s12.db.DEFAULT_CTRL_AS_MODIFIER
import com.suave.s12.db.DEFAULT_ESC_AS_MODIFIER
import com.suave.s12.db.DEFAULT_MIN_SWIPE_LENGTH
import com.suave.s12.db.DEFAULT_SHIFT_AS_MODIFIER
import com.suave.s12.db.DEFAULT_SHOW_TOAST_ON_COPY
import com.suave.s12.db.DEFAULT_SHOW_TOAST_ON_CUT
import com.suave.s12.db.DEFAULT_SHOW_TOAST_ON_LAYOUT_SWITCH
import com.suave.s12.db.DEFAULT_SPACEBAR_MULTITAPS
import com.suave.s12.db.DEFAULT_USE_PRIVATE_CLIPBOARD
import com.suave.s12.db.DEFAULT_VIBRATE_HOLD_REPEAT_TYPE
import com.suave.s12.db.DEFAULT_VIBRATE_ON_HOLD_REPEAT
import com.suave.s12.engine.feedback.hapticTypeFromDb
import com.suave.s12.layout.BuiltinLayouts
import com.suave.s12.ui.components.common.IntStepperPreference
import com.suave.s12.ui.components.common.SettingRow
import com.suave.s12.ui.components.common.SettingTitle
import com.suave.s12.ui.components.common.SettingsScreenBody
import com.suave.s12.ui.components.common.SettingsSection
import com.suave.s12.ui.components.common.TestOutKeyboardRequests
import com.suave.s12.utils.SimpleTopAppBar
import com.suave.s12.utils.TAG
import com.suave.s12.utils.toBool
import com.suave.s12.utils.toInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceTheme
import me.zhanghai.compose.preference.SwitchPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehaviorScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
) {
    Log.d(TAG, "Got to behavior activity")

    val context = LocalContext.current
    val settings by appSettingsViewModel.appSettings.observeAsState()
    val scope = rememberCoroutineScope()

    var minSwipeLengthState = settings?.minSwipeLength ?: DEFAULT_MIN_SWIPE_LENGTH

    var escAsModifierState = (settings?.escAsModifier ?: DEFAULT_ESC_AS_MODIFIER).toBool()
    var ctrlAsModifierState = (settings?.ctrlAsModifier ?: DEFAULT_CTRL_AS_MODIFIER).toBool()
    var altAsModifierState = (settings?.altAsModifier ?: DEFAULT_ALT_AS_MODIFIER).toBool()
    var shiftAsModifierState = (settings?.shiftAsModifier ?: DEFAULT_SHIFT_AS_MODIFIER).toBool()
    var autoCapitalizeState = (settings?.autoCapitalize ?: DEFAULT_AUTO_CAPITALIZE).toBool()
    var spacebarMultitapsState =
        (settings?.spacebarMultitaps ?: DEFAULT_SPACEBAR_MULTITAPS).toBool()
    var showToastOnSwitchState =
        (settings?.showToastOnLayoutSwitch ?: DEFAULT_SHOW_TOAST_ON_LAYOUT_SWITCH).toBool()
    var showToastOnCopyState =
        (settings?.showToastOnCopy ?: DEFAULT_SHOW_TOAST_ON_COPY).toBool()
    var showToastOnCutState =
        (settings?.showToastOnCut ?: DEFAULT_SHOW_TOAST_ON_CUT).toBool()
    val usePrivateClipboard =
        (settings?.usePrivateClipboard ?: DEFAULT_USE_PRIVATE_CLIPBOARD).toBool()
    val vibrateOnHoldRepeat = (settings?.vibrateOnHoldRepeat ?: DEFAULT_VIBRATE_ON_HOLD_REPEAT).toBool()
    val vibrateHoldRepeatType =
        hapticTypeFromDb(settings?.vibrateHoldRepeatType ?: DEFAULT_VIBRATE_HOLD_REPEAT_TYPE)
    val layoutSampleText = BuiltinLayouts.byIndex(settings?.keyboardLayout ?: 0).title
    val privateBadge =
        if (usePrivateClipboard) {
            context.getString(R.string.clipboard_private_badge)
        } else {
            null
        }

    val snackbarHostState = remember { SnackbarHostState() }

    fun updateBehavior() {
        appSettingsViewModel.updateBehavior(
            BehaviorUpdate(
                id = 1,
                minSwipeLength = minSwipeLengthState,
                escAsModifier = escAsModifierState.toInt(),
                ctrlAsModifier = ctrlAsModifierState.toInt(),
                altAsModifier = altAsModifierState.toInt(),
                shiftAsModifier = shiftAsModifierState.toInt(),
                autoCapitalize = autoCapitalizeState.toInt(),
                spacebarMultitaps = spacebarMultitapsState.toInt(),
                showToastOnLayoutSwitch = showToastOnSwitchState.toInt(),
                showToastOnCopy = showToastOnCopyState.toInt(),
                showToastOnCut = showToastOnCutState.toInt(),
            ),
        )
    }

    fun playNotice(
        text: String,
        detail: String? = null,
    ) {
        scope.launch {
            TestOutKeyboardRequests.open()
            repeat(50) {
                delay(100)
                if (IMEService.showNoticeOnActiveIme(text = text, detail = detail)) {
                    return@launch
                }
            }
            snackbarHostState.showSnackbar(
                context.getString(R.string.notice_sample_need_keyboard),
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleTopAppBar(
                text = stringResource(R.string.behavior),
                navController = navController,
            )
        },
        content = { padding ->
            SettingsScreenBody(padding = padding) {
                ProvidePreferenceTheme {
                    SettingsSection(title = stringResource(R.string.settings_section_gestures)) {
                        SettingRow(
                            onReset = {
                                minSwipeLengthState = DEFAULT_MIN_SWIPE_LENGTH
                                updateBehavior()
                            },
                        ) {
                            IntStepperPreference(
                                value = minSwipeLengthState,
                                onValueChange = {
                                    minSwipeLengthState = it
                                    updateBehavior()
                                },
                                valueRange = 0..200,
                                vibrateOnRepeat = vibrateOnHoldRepeat,
                                repeatHapticType = vibrateHoldRepeatType,
                                title = {
                                    Text(stringResource(R.string.min_swipe_length))
                                },
                                summary = {
                                    Text(
                                        stringResource(
                                            R.string.min_swipe_length_summary,
                                            minSwipeLengthState.toString(),
                                        ),
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Swipe,
                                        contentDescription = null,
                                    )
                                },
                            )
                        }
                    }
                    SettingsSection(title = stringResource(R.string.settings_section_typing)) {
                        BehaviorSwitchPreference(
                            title = R.string.auto_capitalize,
                            onSummary = R.string.auto_capitalize_on,
                            offSummary = R.string.auto_capitalize_off,
                            info = R.string.auto_capitalize_info,
                            icon = Icons.Outlined.Abc,
                            value = autoCapitalizeState,
                            onValueChange = {
                                autoCapitalizeState = it
                                updateBehavior()
                            },
                        )
                        BehaviorSwitchPreference(
                            title = R.string.spacebar_multitaps,
                            onSummary = R.string.spacebar_multitaps_on,
                            offSummary = R.string.spacebar_multitaps_off,
                            info = R.string.spacebar_multitaps_info,
                            icon = Icons.Outlined.SpaceBar,
                            value = spacebarMultitapsState,
                            onValueChange = {
                                spacebarMultitapsState = it
                                updateBehavior()
                            },
                        )
                    }
                    SettingsSection(title = stringResource(R.string.settings_section_modifiers)) {
                        BehaviorSwitchPreference(
                            title = R.string.ctrl_as_modifier,
                            onSummary = R.string.ctrl_as_modifier_on,
                            offSummary = R.string.ctrl_as_modifier_off,
                            info = R.string.ctrl_as_modifier_info,
                            icon = Icons.Outlined.KeyboardControlKey,
                            value = ctrlAsModifierState,
                            onValueChange = {
                                ctrlAsModifierState = it
                                updateBehavior()
                            },
                        )
                        BehaviorSwitchPreference(
                            title = R.string.alt_as_modifier,
                            onSummary = R.string.alt_as_modifier_on,
                            offSummary = R.string.alt_as_modifier_off,
                            info = R.string.alt_as_modifier_info,
                            icon = Icons.Outlined.KeyboardOptionKey,
                            value = altAsModifierState,
                            onValueChange = {
                                altAsModifierState = it
                                updateBehavior()
                            },
                        )
                        BehaviorSwitchPreference(
                            title = R.string.shift_as_modifier,
                            onSummary = R.string.shift_as_modifier_on,
                            offSummary = R.string.shift_as_modifier_off,
                            info = R.string.shift_as_modifier_info,
                            icon = Icons.Outlined.KeyboardArrowUp,
                            value = shiftAsModifierState,
                            onValueChange = {
                                shiftAsModifierState = it
                                updateBehavior()
                            },
                        )
                        BehaviorSwitchPreference(
                            title = R.string.esc_as_modifier,
                            onSummary = R.string.esc_as_modifier_on,
                            offSummary = R.string.esc_as_modifier_off,
                            info = R.string.esc_as_modifier_info,
                            iconContent = {
                                Text(
                                    text = "esc",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace,
                                )
                            },
                            value = escAsModifierState,
                            onValueChange = {
                                escAsModifierState = it
                                updateBehavior()
                            },
                        )
                    }
                    SettingsSection(title = stringResource(R.string.settings_section_notices)) {
                        NoticeSwitchPreference(
                            title = R.string.show_toast_on_switch,
                            info = R.string.show_toast_on_switch_info,
                            onSummary = R.string.show_toast_on_switch_on,
                            offSummary = R.string.show_toast_on_switch_off,
                            icon = Icons.Outlined.Notifications,
                            value = showToastOnSwitchState,
                            onValueChange = {
                                showToastOnSwitchState = it
                                updateBehavior()
                            },
                            onSample = { playNotice(layoutSampleText) },
                        )
                        NoticeSwitchPreference(
                            title = R.string.show_toast_on_copy,
                            info = R.string.show_toast_on_copy_info,
                            onSummary = R.string.show_toast_on_copy_on,
                            offSummary = R.string.show_toast_on_copy_off,
                            icon = Icons.Outlined.ContentCopy,
                            value = showToastOnCopyState,
                            onValueChange = {
                                showToastOnCopyState = it
                                updateBehavior()
                            },
                            onSample = {
                                playNotice(
                                    text = context.getString(R.string.copy),
                                    detail = privateBadge,
                                )
                            },
                        )
                        NoticeSwitchPreference(
                            title = R.string.show_toast_on_cut,
                            info = R.string.show_toast_on_cut_info,
                            onSummary = R.string.show_toast_on_cut_on,
                            offSummary = R.string.show_toast_on_cut_off,
                            icon = Icons.Outlined.ContentCut,
                            value = showToastOnCutState,
                            onValueChange = {
                                showToastOnCutState = it
                                updateBehavior()
                            },
                            onSample = {
                                playNotice(
                                    text = context.getString(R.string.cut),
                                    detail = privateBadge,
                                )
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun NoticeSwitchPreference(
    @androidx.annotation.StringRes title: Int,
    @androidx.annotation.StringRes info: Int,
    @androidx.annotation.StringRes onSummary: Int,
    @androidx.annotation.StringRes offSummary: Int,
    icon: ImageVector,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    onSample: () -> Unit,
) {
    Preference(
        title = {
            SettingTitle(
                text = stringResource(title),
                infoText = stringResource(info),
            )
        },
        summary = { Text(stringResource(if (value) onSummary else offSummary)) },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        onClick = { onValueChange(!value) },
        widgetContainer = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSample) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = stringResource(R.string.notice_sample),
                    )
                }
                Switch(
                    checked = value,
                    onCheckedChange = onValueChange,
                )
            }
        },
    )
}

@Composable
private fun BehaviorSwitchPreference(
    @androidx.annotation.StringRes title: Int,
    @androidx.annotation.StringRes onSummary: Int,
    @androidx.annotation.StringRes offSummary: Int,
    @androidx.annotation.StringRes info: Int,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    iconContent: (@Composable () -> Unit)? = null,
) {
    val resolvedIcon: (@Composable () -> Unit)? =
        when {
            iconContent != null -> iconContent
            icon != null -> {
                { Icon(imageVector = icon, contentDescription = stringResource(title)) }
            }
            else -> null
        }
    SwitchPreference(
        value = value,
        onValueChange = onValueChange,
        title = {
            SettingTitle(
                text = stringResource(title),
                infoText = stringResource(info),
            )
        },
        summary = { Text(stringResource(if (value) onSummary else offSummary)) },
        icon = resolvedIcon,
    )
}
