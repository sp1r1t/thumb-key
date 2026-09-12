package com.suave.keyboard.ui.components.settings.appearance

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.suave.keyboard.R
import com.suave.keyboard.ui.components.common.settingsScreenBodyPadding
import com.suave.keyboard.ui.theme.NamedTheme
import com.suave.keyboard.ui.theme.ThemeStore
import com.suave.keyboard.ui.theme.json.THEME_COLOR_ROLES
import com.suave.keyboard.ui.theme.json.THEME_SCHEMA_VERSION
import com.suave.keyboard.ui.theme.json.ThemeDocument
import com.suave.keyboard.ui.theme.json.parseArgbHex
import com.suave.keyboard.utils.SimpleTopAppBar
import com.suave.keyboard.utils.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeEditorScreen(
    navController: NavController,
    themeId: String,
) {
    Log.d(TAG, "Got to theme editor for $themeId")

    val context = LocalContext.current
    val store = remember { ThemeStore.get(context) }
    val initial =
        remember(themeId) {
            store.load(themeId) ?: error("Missing user theme $themeId")
        }

    var title by remember { mutableStateOf(initial.title) }
    var editingDark by remember { mutableStateOf(false) }
    val lightRoles =
        remember {
            mutableStateMapOf<String, String>().apply {
                putAll(initial.toDocument().light)
            }
        }
    val darkRoles =
        remember {
            mutableStateMapOf<String, String>().apply {
                putAll(initial.toDocument().dark)
            }
        }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val activeRoles = if (editingDark) darkRoles else lightRoles

    val previewTheme =
        remember(title, lightRoles.toMap(), darkRoles.toMap()) {
            runCatching {
                NamedTheme.fromDocument(
                    ThemeDocument(
                        schemaVersion = THEME_SCHEMA_VERSION,
                        id = themeId,
                        title = title.ifBlank { initial.title },
                        light = lightRoles.toMap(),
                        dark = darkRoles.toMap(),
                    ),
                    builtin = false,
                )
            }.getOrNull()
        }

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                try {
                    val json =
                        withContext(Dispatchers.IO) {
                            store.exportJson(themeId)
                        }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray())
                        } ?: throw IllegalStateException("Could not write file")
                    }
                    snackbarHostState.showSnackbar(context.getString(R.string.theme_exported))
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.theme_export_failed, e.message ?: ""),
                    )
                }
            }
        }

    fun save() {
        scope.launch {
            try {
                val document =
                    ThemeDocument(
                        schemaVersion = THEME_SCHEMA_VERSION,
                        id = themeId,
                        title = title.ifBlank { initial.title },
                        light = lightRoles.toMap(),
                        dark = darkRoles.toMap(),
                    )
                withContext(Dispatchers.IO) { store.save(document) }
                snackbarHostState.showSnackbar(context.getString(R.string.theme_saved))
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.theme_save_failed, e.message ?: ""),
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleTopAppBar(
                text = stringResource(R.string.theme_editor),
                navController = navController,
            )
        },
        content = { padding ->
            val layoutDirection = LocalLayoutDirection.current
            val density = LocalDensity.current
            val imeBottom = with(density) { WindowInsets.ime.getBottom(density).toDp() }
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            settingsScreenBodyPadding(
                                padding = padding,
                                layoutDirection = layoutDirection,
                                imeBottom = imeBottom,
                            ),
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.theme_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    stringResource(R.string.theme_preview),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                if (previewTheme != null) {
                    ThemePreviewCard(previewTheme, showDark = editingDark)
                } else {
                    Text(
                        stringResource(R.string.theme_preview_invalid),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !editingDark,
                        onClick = { editingDark = false },
                        label = { Text(stringResource(R.string.light)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.LightMode, contentDescription = null)
                        },
                    )
                    FilterChip(
                        selected = editingDark,
                        onClick = { editingDark = true },
                        label = { Text(stringResource(R.string.dark)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.DarkMode, contentDescription = null)
                        },
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    if (editingDark) {
                        stringResource(R.string.theme_editing_dark)
                    } else {
                        stringResource(R.string.theme_editing_light)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(8.dp))

                for (role in THEME_COLOR_ROLES) {
                    val value = activeRoles[role] ?: "#FF000000"
                    RoleColorRow(
                        role = role,
                        value = value,
                        onValueChange = { activeRoles[role] = it },
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { save() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.theme_save))
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val document =
                                        ThemeDocument(
                                            schemaVersion = THEME_SCHEMA_VERSION,
                                            id = themeId,
                                            title = title.ifBlank { initial.title },
                                            light = lightRoles.toMap(),
                                            dark = darkRoles.toMap(),
                                        )
                                    withContext(Dispatchers.IO) { store.save(document) }
                                    exportLauncher.launch("$themeId.json")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.theme_save_failed, e.message ?: ""),
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.theme_export))
                    }
                }
            }
        },
    )
}

@Composable
private fun ThemePreviewCard(
    theme: NamedTheme,
    showDark: Boolean,
) {
    val scheme = if (showDark) theme.dark else theme.light
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.background)
                .border(1.dp, scheme.outline, RoundedCornerShape(12.dp))
                .padding(12.dp),
    ) {
        Text(
            theme.title,
            color = scheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PreviewKey(scheme.primary, scheme.onPrimary, "Aa")
            PreviewKey(scheme.surface, scheme.onSurface, "Bb")
            PreviewKey(scheme.surfaceVariant, scheme.onSurfaceVariant, "Cc")
            PreviewKey(scheme.tertiaryContainer, scheme.onTertiaryContainer, "123")
        }
        Spacer(Modifier.height(8.dp))
        RoleSwatchStrip(theme)
    }
}

@Composable
private fun PreviewKey(
    background: Color,
    foreground: Color,
    label: String,
) {
    Box(
        modifier =
            Modifier
                .size(width = 48.dp, height = 36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = foreground, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun RoleColorRow(
    role: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    val parsed = runCatching { parseArgbHex(value) }.getOrNull()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(parsed ?: Color.Transparent)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(6.dp),
                    ),
        )
        Spacer(Modifier.width(12.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(role) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            isError = parsed == null,
            supportingText = {
                if (parsed == null) {
                    Text(stringResource(R.string.theme_invalid_hex))
                }
            },
        )
    }
}
