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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.suave.s12.R
import com.suave.s12.db.AppSettingsViewModel
import com.suave.s12.db.BehaviorUpdate
import com.suave.s12.db.DEFAULT_ALT_AS_MODIFIER
import com.suave.s12.db.DEFAULT_AUTO_CAPITALIZE
import com.suave.s12.db.DEFAULT_CIRCULAR_DRAG_ENABLED
import com.suave.s12.db.DEFAULT_CLOCKWISE_DRAG_ACTION
import com.suave.s12.db.DEFAULT_COUNTERCLOCKWISE_DRAG_ACTION
import com.suave.s12.db.DEFAULT_CTRL_AS_MODIFIER
import com.suave.s12.db.DEFAULT_DRAG_RETURN_ENABLED
import com.suave.s12.db.DEFAULT_ESC_AS_MODIFIER
import com.suave.s12.db.DEFAULT_GHOST_KEYS_ENABLED
import com.suave.s12.db.DEFAULT_MIN_SWIPE_LENGTH
import com.suave.s12.db.DEFAULT_SHIFT_AS_MODIFIER
import com.suave.s12.db.DEFAULT_SLIDE_BACKSPACE_DEADZONE_ENABLED
import com.suave.s12.db.DEFAULT_SLIDE_CURSOR_MOVEMENT_MODE
import com.suave.s12.db.DEFAULT_SLIDE_ENABLED
import com.suave.s12.db.DEFAULT_SLIDE_HOLD_ENABLED
import com.suave.s12.db.DEFAULT_SLIDE_SENSITIVITY
import com.suave.s12.db.DEFAULT_SLIDE_SPACEBAR_DEADZONE_ENABLED
import com.suave.s12.db.DEFAULT_SPACEBAR_MULTITAPS
import com.suave.s12.ui.components.common.SettingRow
import com.suave.s12.ui.components.common.TestOutTextField
import com.suave.s12.ui.components.settings.about.SettingsDivider
import com.suave.s12.utils.SimpleTopAppBar
import com.suave.s12.utils.TAG
import com.suave.s12.utils.toBool
import com.suave.s12.utils.toInt
import me.zhanghai.compose.preference.ProvidePreferenceTheme
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.SwitchPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehaviorScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
) {
    Log.d(TAG, "Got to behavior activity")

    val settings by appSettingsViewModel.appSettings.observeAsState()

    var minSwipeLengthState = (settings?.minSwipeLength ?: DEFAULT_MIN_SWIPE_LENGTH).toFloat()
    var minSwipeLengthSliderState by remember { mutableFloatStateOf(minSwipeLengthState) }

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
                minSwipeLength = minSwipeLengthState.toInt(),
                // Everything below is a read-through of whatever's already in the DB, not an
                // editable setting any more - see CLAUDE.md's UI principles and this screen's
                // audit history for why (dead fields from the pre-rewrite engine, with no live
                // reader left anywhere). Passing the stored value instead of the bare default
                // avoids silently resetting it just because an unrelated live control changed.
                slideSensitivity = settings?.slideSensitivity ?: DEFAULT_SLIDE_SENSITIVITY,
                slideEnabled = settings?.slideEnabled ?: DEFAULT_SLIDE_ENABLED,
                slideCursorMovementMode = settings?.slideCursorMovementMode ?: DEFAULT_SLIDE_CURSOR_MOVEMENT_MODE,
                slideSpacebarDeadzoneEnabled = settings?.slideSpacebarDeadzoneEnabled ?: DEFAULT_SLIDE_SPACEBAR_DEADZONE_ENABLED,
                slideBackspaceDeadzoneEnabled = settings?.slideBackspaceDeadzoneEnabled ?: DEFAULT_SLIDE_BACKSPACE_DEADZONE_ENABLED,
                autoCapitalize = settings?.autoCapitalize ?: DEFAULT_AUTO_CAPITALIZE,
                spacebarMultiTaps = settings?.spacebarMultiTaps ?: DEFAULT_SPACEBAR_MULTITAPS,
                dragReturnEnabled = settings?.dragReturnEnabled ?: DEFAULT_DRAG_RETURN_ENABLED,
                circularDragEnabled = settings?.circularDragEnabled ?: DEFAULT_CIRCULAR_DRAG_ENABLED,
                clockwiseDragAction = settings?.clockwiseDragAction ?: DEFAULT_CLOCKWISE_DRAG_ACTION,
                counterclockwiseDragAction = settings?.counterclockwiseDragAction ?: DEFAULT_COUNTERCLOCKWISE_DRAG_ACTION,
                ghostKeysEnabled = settings?.ghostKeysEnabled ?: DEFAULT_GHOST_KEYS_ENABLED,
                slideHoldEnabled = settings?.slideHoldEnabled ?: DEFAULT_SLIDE_HOLD_ENABLED,
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
                    SettingRow(
                        onReset = {
                            minSwipeLengthState = DEFAULT_MIN_SWIPE_LENGTH.toFloat()
                            minSwipeLengthSliderState = DEFAULT_MIN_SWIPE_LENGTH.toFloat()
                            updateBehavior()
                        },
                    ) {
                        SliderPreference(
                            value = minSwipeLengthState,
                            sliderValue = minSwipeLengthSliderState,
                            onValueChange = {
                                minSwipeLengthState = it
                                updateBehavior()
                            },
                            onSliderValueChange = { minSwipeLengthSliderState = it },
                            valueRange = 0f..200f,
                            title = {
                                Text(stringResource(R.string.min_swipe_length))
                            },
                            summary = {
                                Text(
                                    stringResource(
                                        R.string.min_swipe_length_summary,
                                        minSwipeLengthSliderState.toInt().toString(),
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
                    SettingsDivider()
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
                    SettingsDivider()
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
                    SettingsDivider()
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
                    SettingsDivider()
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
                    SettingsDivider()
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
    SettingRow(infoText = stringResource(info)) {
        SwitchPreference(
            value = value,
            onValueChange = onValueChange,
            title = { Text(stringResource(title)) },
            summary = { Text(stringResource(if (value) onSummary else offSummary)) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.SwapHoriz,
                    contentDescription = stringResource(title),
                )
            },
        )
    }
}
