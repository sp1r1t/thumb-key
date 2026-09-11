package com.suave.s12.ui.components.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suave.s12.R

/**
 * Wraps a single preference row (`SwitchPreference`/`SliderPreference`/`ListPreference`) with up
 * to two optional trailing actions, per this project's UI principles (see the repo's CLAUDE.md):
 * a reset-to-default button for non-boolean settings, and an "i" info icon for settings whose
 * full rationale doesn't fit in the row's own dynamic summary text.
 *
 * The wrapped preference renders at `Modifier.weight(1f)` inside a [Row], so its own trailing
 * control (a Switch, a slider's value, a list's dropdown) keeps rendering exactly as it does
 * unwrapped - just in a slightly narrower row - rather than overlapping a bolted-on icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingRow(
    infoText: String? = null,
    onReset: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (onReset == null && infoText == null) {
        content()
        return
    }

    var showInfo by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
        if (onReset != null) {
            IconButton(onClick = onReset) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = stringResource(R.string.reset_to_default),
                )
            }
        }
        if (infoText != null) {
            IconButton(onClick = { showInfo = true }) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.more_info),
                )
            }
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
