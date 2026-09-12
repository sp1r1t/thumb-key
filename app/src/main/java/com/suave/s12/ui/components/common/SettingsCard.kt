package com.suave.s12.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Rounded settings card used by haptic channels and chip multi-selects.
 */
@Composable
fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(content = content)
    }
}

/**
 * Title, live summary, named chips, and optional extra rows on a [SettingsCard].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipSelectCard(
    title: String,
    infoText: String,
    summary: String,
    icon: ImageVector,
    onReset: () -> Unit,
    extra: @Composable ColumnScope.() -> Unit = {},
    chips: @Composable () -> Unit,
) {
    SettingsCard {
        SettingRow(onReset = onReset) {
            Row(
                modifier =
                    Modifier.padding(
                        start = 16.dp,
                        top = 12.dp,
                        bottom = 8.dp,
                        end = 4.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    SettingTitle(text = title, infoText = infoText)
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        FlowRow(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            chips()
        }
        extra()
    }
}
