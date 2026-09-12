package com.suave.s12.ui.components.settings.appearance

import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.suave.s12.engine.feedback.HapticType
import com.suave.s12.ui.components.common.SettingRow
import com.suave.s12.ui.components.common.SettingTitle
import com.suave.s12.ui.engine.playHaptic
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.SwitchPreference

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
    @StringRes typeTitle: Int,
    @StringRes typeSummary: Int,
    icon: ImageVector,
) {
    val resources = LocalResources.current
    val view = LocalView.current
    val typeName = stringResource(type.resId)

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

    if (enabled) {
        SettingRow(
            onReset = { onTypeChange(defaultType) },
        ) {
            ListPreference(
                type = ListPreferenceType.DROPDOWN_MENU,
                value = type,
                onValueChange = {
                    onTypeChange(it)
                    view.playHaptic(it)
                },
                values = HapticType.entries,
                valueToText = {
                    AnnotatedString(resources.getString(it.resId))
                },
                title = {
                    Text(stringResource(typeTitle))
                },
                summary = {
                    Text(stringResource(typeSummary, typeName))
                },
            )
        }
    }
}
