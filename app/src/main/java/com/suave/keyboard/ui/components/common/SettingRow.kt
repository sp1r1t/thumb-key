package com.suave.keyboard.ui.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suave.keyboard.R
import dev.jeziellago.compose.markdowntext.MarkdownText

/**
 * Title slot for a preference row. Optional [infoText] places a compact "i" next to the title
 * (not a trailing row control) and opens a bottom sheet with the longer rationale.
 * [infoText] may use Markdown (headings, lists, emphasis).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingTitle(
    text: String,
    infoText: String? = null,
    modifier: Modifier = Modifier,
) {
    var showInfo by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text)
        if (infoText != null) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.more_info),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .padding(start = 6.dp)
                        .requiredSize(18.dp)
                        .clickable(role = Role.Button) { showInfo = true },
            )
        }
    }

    if (infoText != null && showInfo) {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(),
            onDismissRequest = { showInfo = false },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(bottom = 24.dp),
            ) {
                MarkdownText(
                    markdown = infoText,
                    linkColor = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Right-aligned trailing control cluster for a preference row. Put secondary actions (reset)
 * first and the primary control last so the primary control owns the far trailing edge.
 *
 * Example: reset, then a horizontal - / value / + pill.
 */
@Composable
fun PreferenceControlCluster(content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/** Reset affordance for use inside [PreferenceControlCluster]; tinted with theme primary. */
@Composable
fun PreferenceResetButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    resetToLabel: String? = null,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier =
            Modifier
                .widthIn(min = 32.dp)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.RestartAlt,
            contentDescription = stringResource(R.string.reset_to_default),
            tint =
                if (enabled) {
                    accent
                } else {
                    accent.copy(alpha = 0.38f)
                },
            modifier = Modifier.size(22.dp),
        )
        if (resetToLabel != null) {
            Text(
                text = resetToLabel,
                color =
                    if (enabled) {
                        accent
                    } else {
                        accent.copy(alpha = 0.38f)
                    },
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                lineHeight = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/**
 * Wraps a preference that has **no** built-in trailing widget (e.g. a dropdown [ListPreference]
 * or a custom title row) with a right-aligned reset.
 *
 * For steppers, pass `onReset` to [IntStepperPreference] instead - that keeps reset with
 * the - / value / + pill on the row under the title.
 *
 * Trailing-control rule (see also [PreferenceControlCluster]): the primary control owns the
 * far right; reset sits immediately left of it when both are present.
 */
@Composable
fun SettingRow(
    onReset: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (onReset == null) {
        content()
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
        PreferenceControlCluster {
            PreferenceResetButton(onClick = onReset)
        }
    }
}
