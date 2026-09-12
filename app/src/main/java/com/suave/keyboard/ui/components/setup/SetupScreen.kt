package com.suave.keyboard.ui.components.setup

import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.suave.keyboard.R
import com.suave.keyboard.ui.components.common.SettingsCard
import com.suave.keyboard.utils.SimpleTopAppBar
import com.suave.keyboard.utils.TAG
import splitties.systemservices.inputMethodManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    navController: NavController,
    suaveEnabled: Boolean,
    suaveSelected: Boolean,
) {
    Log.d(TAG, "Got to setup activity")

    val snackbarHostState = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    val scrollState = rememberScrollState()
    val setupComplete = suaveEnabled && suaveSelected

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleTopAppBar(
                text = stringResource(R.string.setup_suave),
                navController = navController,
                showBack = false,
            )
        },
        content = { padding ->
            Column(
                modifier =
                    Modifier
                        .padding(padding)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.setup_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                SetupStepCard(
                    stepNumber = 1,
                    title = stringResource(R.string.enable_suave),
                    summary =
                        if (suaveEnabled) {
                            stringResource(R.string.setup_enable_done)
                        } else {
                            stringResource(R.string.setup_enable_needed)
                        },
                    done = suaveEnabled,
                    icon = Icons.Outlined.Keyboard,
                    actionLabel =
                        if (!suaveEnabled) {
                            stringResource(R.string.setup_open_ime_settings)
                        } else {
                            null
                        },
                    onAction =
                        if (!suaveEnabled) {
                            {
                                ctx.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                            }
                        } else {
                            null
                        },
                )

                SetupStepCard(
                    stepNumber = 2,
                    title = stringResource(R.string.select_suave),
                    summary =
                        when {
                            !suaveEnabled -> stringResource(R.string.setup_select_waiting)
                            suaveSelected -> stringResource(R.string.setup_select_done)
                            else -> stringResource(R.string.setup_select_needed)
                        },
                    done = suaveSelected,
                    icon = Icons.Outlined.TouchApp,
                    actionLabel =
                        if (suaveEnabled && !suaveSelected) {
                            stringResource(R.string.setup_choose_keyboard)
                        } else {
                            null
                        },
                    onAction =
                        if (suaveEnabled && !suaveSelected) {
                            { inputMethodManager.showInputMethodPicker() }
                        } else {
                            null
                        },
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (setupComplete) {
                    Button(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        onClick = { navController.navigate("settings") },
                    ) {
                        Text(stringResource(R.string.finish_setup))
                    }
                } else {
                    TextButton(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        onClick = { navController.navigate("settings") },
                    ) {
                        Text(stringResource(R.string.setup_skip_for_now))
                    }
                }
            }
        },
    )
}

@Composable
private fun SetupStepCard(
    stepNumber: Int,
    title: String,
    summary: String,
    done: Boolean,
    icon: ImageVector,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    val statusIcon =
        if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked
    val statusTint =
        if (done) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    SettingsCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.setup_step_title, stepNumber, title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusTint,
                    modifier = Modifier.size(28.dp),
                )
            }

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAction,
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
