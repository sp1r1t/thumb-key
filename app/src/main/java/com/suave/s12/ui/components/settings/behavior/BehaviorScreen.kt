package com.suave.s12.ui.components.settings.behavior

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Swipe
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
import com.suave.s12.db.BehaviorUpdate
import com.suave.s12.db.DEFAULT_ALT_AS_MODIFIER
import com.suave.s12.db.DEFAULT_CTRL_AS_MODIFIER
import com.suave.s12.db.DEFAULT_ESC_AS_MODIFIER
import com.suave.s12.db.DEFAULT_MIN_SWIPE_LENGTH
import com.suave.s12.db.DEFAULT_SHIFT_AS_MODIFIER
import com.suave.s12.ui.components.common.IntStepperPreference
import com.suave.s12.ui.components.common.SettingRow
import com.suave.s12.ui.components.common.SettingTitle
import com.suave.s12.ui.components.common.SettingsSection
import com.suave.s12.ui.components.common.TestOutTextField
import com.suave.s12.utils.SimpleTopAppBar
import com.suave.s12.utils.TAG
import com.suave.s12.utils.toBool
import com.suave.s12.utils.toInt
import me.zhanghai.compose.preference.ProvidePreferenceTheme
import me.zhanghai.compose.preference.SwitchPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehaviorScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
) {
    Log.d(TAG, "Got to behavior activity")

    val settings by appSettingsViewModel.appSettings.observeAsState()

    var minSwipeLengthState = settings?.minSwipeLength ?: DEFAULT_MIN_SWIPE_LENGTH

    var escAsModifierState = (settings?.escAsModifier ?: DEFAULT_ESC_AS_MODIFIER).toBool()
    var ctrlAsModifierState = (settings?.ctrlAsModifier ?: DEFAULT_CTRL_AS_MODIFIER).toBool()
    var altAsModifierState = (settings?.altAsModifier ?: DEFAULT_ALT_AS_MODIFIER).toBool()
    var shiftAsModifierState = (settings?.shiftAsModifier ?: DEFAULT_SHIFT_AS_MODIFIER).toBool()

    val snackbarHostState = remember { SnackbarHostState() }

    val scrollState = rememberScrollState()

    fun updateBehavior() {
        appSettingsViewModel.updateBehavior(
            BehaviorUpdate(
                id = 1,
                minSwipeLength = minSwipeLengthState,
                escAsModifier = escAsModifierState.toInt(),
                ctrlAsModifier = ctrlAsModifierState.toInt(),
                altAsModifier = altAsModifierState.toInt(),
                shiftAsModifier = shiftAsModifierState.toInt(),
            ),
        )
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
            Column(
                modifier =
                    Modifier
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .background(color = MaterialTheme.colorScheme.surface)
                        .imePadding(),
            ) {
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
                    SettingsSection(title = stringResource(R.string.settings_section_modifiers)) {
                    ModifierAsModifierSwitch(
                        title = R.string.ctrl_as_modifier,
                        onSummary = R.string.ctrl_as_modifier_on,
                        offSummary = R.string.ctrl_as_modifier_off,
                        info = R.string.ctrl_as_modifier_info,
                        value = ctrlAsModifierState,
                        onValueChange = {
                            ctrlAsModifierState = it
                            updateBehavior()
                        },
                    )
                    ModifierAsModifierSwitch(
                        title = R.string.alt_as_modifier,
                        onSummary = R.string.alt_as_modifier_on,
                        offSummary = R.string.alt_as_modifier_off,
                        info = R.string.alt_as_modifier_info,
                        value = altAsModifierState,
                        onValueChange = {
                            altAsModifierState = it
                            updateBehavior()
                        },
                    )
                    ModifierAsModifierSwitch(
                        title = R.string.shift_as_modifier,
                        onSummary = R.string.shift_as_modifier_on,
                        offSummary = R.string.shift_as_modifier_off,
                        info = R.string.shift_as_modifier_info,
                        value = shiftAsModifierState,
                        onValueChange = {
                            shiftAsModifierState = it
                            updateBehavior()
                        },
                    )
                    ModifierAsModifierSwitch(
                        title = R.string.esc_as_modifier,
                        onSummary = R.string.esc_as_modifier_on,
                        offSummary = R.string.esc_as_modifier_off,
                        info = R.string.esc_as_modifier_info,
                        value = escAsModifierState,
                        onValueChange = {
                            escAsModifierState = it
                            updateBehavior()
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
private fun ModifierAsModifierSwitch(
    @androidx.annotation.StringRes title: Int,
    @androidx.annotation.StringRes onSummary: Int,
    @androidx.annotation.StringRes offSummary: Int,
    @androidx.annotation.StringRes info: Int,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
) {
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
        icon = {
            Icon(
                imageVector = Icons.Outlined.SwapHoriz,
                contentDescription = stringResource(title),
            )
        },
    )
}
