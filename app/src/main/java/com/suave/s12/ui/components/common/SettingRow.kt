package com.suave.s12.ui.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.suave.s12.R

/**
 * Title slot for a preference row. Optional [infoText] places a compact "i" next to the title
 * (not a trailing row control) and opens a bottom sheet with the longer rationale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingTitle(
    text: String,
    infoText: String? = null,
) {
    var showInfo by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text)
        if (infoText != null) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.more_info),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .padding(start = 6.dp)
                        .size(18.dp)
                        .clickable(role = Role.Button) { showInfo = true },
            )
        }
    }

    if (infoText != null && showInfo) {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(),
            onDismissRequest = { showInfo = false },
        ) {
            Text(infoText, modifier = Modifier.padding(16.dp))
        }
    }
}

/**
 * Wraps a non-boolean preference with a reset-to-default action, per this project's UI
 * principles (see the repo's CLAUDE.md). The preference renders at `Modifier.weight(1f)` so
 * its own trailing control (a stepper or dropdown) stays on the row instead of overlapping
 * the reset icon. Info belongs in [SettingTitle], not here: a trailing "i" squeezed switches
 * inward and misaligned the list.
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

    Row(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
        IconButton(onClick = onReset) {
            Icon(
                imageVector = Icons.Outlined.RestartAlt,
                contentDescription = stringResource(R.string.reset_to_default),
            )
        }
    }
}
