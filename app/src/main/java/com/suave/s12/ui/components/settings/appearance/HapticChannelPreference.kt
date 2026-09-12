package com.suave.s12.ui.components.settings.appearance

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suave.s12.R
import com.suave.s12.engine.feedback.HAPTIC_TYPE_BY_FEEL
import com.suave.s12.engine.feedback.HapticType
import com.suave.s12.ui.components.common.SettingRow
import com.suave.s12.ui.components.common.SettingTitle
import com.suave.s12.ui.components.common.SettingsCard
import com.suave.s12.ui.engine.playHaptic
import me.zhanghai.compose.preference.SwitchPreference

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HapticChannelPreference(
    enabled: Boolean,
    type: HapticType,
    onEnabledChange: (Boolean) -> Unit,
    onTypeChange: (HapticType) -> Unit,
    defaultType: HapticType,
    @StringRes title: Int,
    @StringRes onSummary: Int,
    @StringRes offSummary: Int,
    @StringRes info: Int,
    icon: ImageVector,
) {
    val view = LocalView.current
    val typeName = stringResource(type.resId)

    SettingsCard {
        Column {
            SwitchPreference(
                value = enabled,
                onValueChange = onEnabledChange,
                title = {
                    SettingTitle(
                        text = stringResource(title),
                        infoText = stringResource(info),
                    )
                },
                summary = {
                    Text(
                        if (enabled) {
                            stringResource(onSummary, typeName)
                        } else {
                            stringResource(offSummary)
                        },
                    )
                },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                    )
                },
            )
            AnimatedVisibility(visible = enabled) {
                Column(modifier = Modifier.padding(start = 12.dp, end = 4.dp, bottom = 10.dp)) {
                    SettingRow(
                        onReset = { onTypeChange(defaultType) },
                    ) {
                        Text(
                            text = stringResource(R.string.haptic_feel),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 44.dp, top = 8.dp, bottom = 4.dp),
                        )
                    }
                    FlowRow(
                        modifier = Modifier.padding(start = 44.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        HAPTIC_TYPE_BY_FEEL.forEach { option ->
                            FilterChip(
                                selected = option == type,
                                onClick = {
                                    onTypeChange(option)
                                    view.playHaptic(option)
                                },
                                label = { Text(stringResource(option.resId)) },
                            )
                        }
                    }
                }
            }
        }
    }
}
