package com.suave.keyboard.ui.components.settings.appearance

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.suave.keyboard.R
import com.suave.keyboard.ui.components.common.ColorPickerSheet
import com.suave.keyboard.ui.components.common.settingsScreenBodyPadding
import com.suave.keyboard.ui.theme.NamedTheme
import com.suave.keyboard.ui.theme.ThemeStore
import com.suave.keyboard.ui.theme.json.THEME_COLOR_ROLES
import com.suave.keyboard.ui.theme.json.THEME_SCHEMA_VERSION
import com.suave.keyboard.ui.theme.json.ThemeDocument
import com.suave.keyboard.ui.theme.json.parseArgbHex
import com.suave.keyboard.ui.theme.json.parseThemeDocument
import com.suave.keyboard.utils.SimpleTopAppBar
import com.suave.keyboard.utils.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val THEME_AUTO_SAVE_DEBOUNCE_MS = 250L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeEditorScreen(
    navController: NavController,
    themeId: String,
) {
    Log.d(TAG, "Got to theme editor for $themeId")

    val context = LocalContext.current
    val activity = LocalActivity.current
    val store = remember { ThemeStore.get(context) }
    val initial =
        remember(themeId) {
            store.load(themeId) ?: error("Missing user theme $themeId")
        }
    val openBaseline =
        remember(themeId) {
            completeRoles(initial.toDocument())
        }

    var sessionBaseline by remember { mutableStateOf(openBaseline) }
    var draft by remember { mutableStateOf(openBaseline) }
    var dirty by remember { mutableStateOf(false) }
    var editingDark by remember { mutableStateOf(false) }
    var pickerRole by remember { mutableStateOf<String?>(null) }
    var persistJob by remember { mutableStateOf<Job?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val activeRoles = if (editingDark) draft.dark else draft.light

    val previewTheme =
        remember(draft) {
            runCatching {
                NamedTheme.fromDocument(draft, builtin = false)
            }.getOrNull()
        }

    fun leaveEditor() {
        if (navController.previousBackStackEntry == null) {
            activity?.finish()
        } else {
            navController.popBackStack()
        }
    }

    fun schedulePersist(document: ThemeDocument) {
        persistJob?.cancel()
        persistJob =
            scope.launch {
                delay(THEME_AUTO_SAVE_DEBOUNCE_MS)
                try {
                    withContext(Dispatchers.IO) { store.save(document) }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.theme_save_failed, e.message ?: ""),
                    )
                }
            }
    }

    fun commitDraft(next: ThemeDocument) {
        if (next == draft) return
        draft = next
        dirty = next != sessionBaseline
        // Skip disk writes while a hex is mid-edit / invalid; last good save stays.
        if (runCatching { NamedTheme.fromDocument(next, builtin = false) }.isSuccess) {
            schedulePersist(next)
        }
    }

    fun updateTitle(title: String) {
        commitDraft(draft.copy(title = title))
    }

    fun updateRole(
        role: String,
        hex: String,
    ) {
        val light = draft.light.toMutableMap()
        val dark = draft.dark.toMutableMap()
        if (editingDark) {
            dark[role] = hex
        } else {
            light[role] = hex
        }
        commitDraft(draft.copy(light = light, dark = dark))
    }

    fun abortAndLeave() {
        persistJob?.cancel()
        scope.launch {
            try {
                withContext(Dispatchers.IO) { store.save(sessionBaseline) }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.theme_save_failed, e.message ?: ""),
                )
            } finally {
                leaveEditor()
            }
        }
    }

    BackHandler {
        // Edits already auto-save; Back keeps them. Discard is the explicit revert action.
        leaveEditor()
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
                            // Prefer the live draft so export matches what is on screen even
                            // if the debounce has not flushed yet.
                            store.save(draft)
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

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                try {
                    val json =
                        withContext(Dispatchers.IO) {
                            context.contentResolver
                                .openInputStream(uri)
                                ?.bufferedReader()
                                ?.use { it.readText() }
                                ?: throw IllegalStateException("Could not read file")
                        }
                    val imported = parseThemeDocument(json)
                    commitDraft(
                        completeRoles(
                            draft.copy(
                                title = imported.title.ifBlank { draft.title },
                                light = imported.light,
                                dark = imported.dark,
                                schemaVersion = THEME_SCHEMA_VERSION,
                            ),
                        ),
                    )
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.theme_imported_into_editor, imported.title),
                    )
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.theme_import_failed, e.message ?: ""),
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
                onNavigateBack = { leaveEditor() },
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
                    value = draft.title,
                    onValueChange = { updateTitle(it) },
                    label = { Text(stringResource(R.string.theme_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.theme_preview),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.theme_preview_tap_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (previewTheme != null) {
                    ThemePreviewCard(
                        theme = previewTheme,
                        showDark = editingDark,
                        onRoleClick = { role -> pickerRole = role },
                    )
                } else {
                    Text(
                        stringResource(R.string.theme_preview_invalid),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    if (editingDark) {
                        stringResource(R.string.theme_editing_dark)
                    } else {
                        stringResource(R.string.theme_editing_light)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                for (role in THEME_COLOR_ROLES) {
                    val value = activeRoles[role] ?: "#FF000000"
                    RoleColorRow(
                        role = role,
                        value = value,
                        onValueChange = { updateRole(role, it) },
                        onOpenPicker = { pickerRole = role },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.theme_unsaved_changes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { abortAndLeave() },
                    enabled = dirty,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    border =
                        BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text(stringResource(R.string.theme_cancel_edits))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.theme_import))
                    }
                    Button(
                        onClick = { exportLauncher.launch("$themeId.json") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.theme_export))
                    }
                }
            }
        },
    )

    pickerRole?.let { role ->
        ColorPickerSheet(
            role = role,
            initialHex = activeRoles[role] ?: "#FF000000",
            onDismiss = { pickerRole = null },
            onConfirm = { hex ->
                updateRole(role, hex)
                pickerRole = null
            },
        )
    }
}

private fun completeRoles(document: ThemeDocument): ThemeDocument {
    val light = document.light.toMutableMap()
    val dark = document.dark.toMutableMap()
    for (role in THEME_COLOR_ROLES) {
        light.putIfAbsent(role, "#FF888888")
        dark.putIfAbsent(role, "#FF888888")
    }
    return document.copy(
        schemaVersion = THEME_SCHEMA_VERSION,
        title = document.title.ifBlank { "Theme" },
        light = light,
        dark = dark,
    )
}

@Composable
private fun ThemePreviewCard(
    theme: NamedTheme,
    showDark: Boolean,
    onRoleClick: (String) -> Unit,
) {
    val scheme = if (showDark) theme.dark else theme.light
    val extras = if (showDark) theme.darkExtras else theme.lightExtras
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
            modifier =
                Modifier.clickable(role = Role.Button) {
                    onRoleClick("onBackground")
                },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PreviewKey(
                background = scheme.primary,
                foreground = scheme.onPrimary,
                label = "Aa",
                backgroundRole = "primary",
                foregroundRole = "onPrimary",
                onRoleClick = onRoleClick,
            )
            PreviewKey(
                background = scheme.surface,
                foreground = scheme.onSurface,
                label = "Bb",
                backgroundRole = "surface",
                foregroundRole = "onSurface",
                onRoleClick = onRoleClick,
            )
            PreviewKey(
                background = scheme.surfaceVariant,
                foreground = scheme.onSurfaceVariant,
                label = "Cc",
                backgroundRole = "surfaceVariant",
                foregroundRole = "onSurfaceVariant",
                onRoleClick = onRoleClick,
            )
            PreviewKey(
                background = scheme.tertiaryContainer,
                foreground = scheme.onTertiaryContainer,
                label = "123",
                backgroundRole = "tertiaryContainer",
                foregroundRole = "onTertiaryContainer",
                onRoleClick = onRoleClick,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            stringResource(R.string.theme_preview_roles),
            color = scheme.onBackground.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (role in PREVIEW_EDITABLE_ROLES) {
                val color =
                    when (role) {
                        "success" -> extras.success
                        "onSuccess" -> extras.onSuccess
                        "successContainer" -> extras.successContainer
                        "onSuccessContainer" -> extras.onSuccessContainer
                        else -> scheme.colorForRole(role)
                    }
                PreviewRoleChip(
                    role = role,
                    color = color,
                    labelColor = scheme.onBackground,
                    onClick = { onRoleClick(role) },
                )
            }
        }
    }
}

private val PREVIEW_EDITABLE_ROLES =
    listOf(
        "primary",
        "secondary",
        "tertiary",
        "error",
        "success",
        "background",
        "surface",
        "surfaceVariant",
        "outline",
        "inversePrimary",
        "tertiaryContainer",
    )

@Composable
private fun PreviewRoleChip(
    role: String,
    color: Color,
    labelColor: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .widthIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .border(1.dp, labelColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = role,
            color = labelColor,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PreviewKey(
    background: Color,
    foreground: Color,
    label: String,
    backgroundRole: String,
    foregroundRole: String,
    onRoleClick: (String) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(width = 48.dp, height = 36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(background)
                .clickable(role = Role.Button) { onRoleClick(backgroundRole) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = foreground,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clickable(role = Role.Button) { onRoleClick(foregroundRole) },
        )
    }
}

@Composable
private fun RoleColorRow(
    role: String,
    value: String,
    onValueChange: (String) -> Unit,
    onOpenPicker: () -> Unit,
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
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(parsed ?: Color.Transparent)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable(role = Role.Button, onClick = onOpenPicker),
        )
        Spacer(modifier = Modifier.width(12.dp))
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
                } else {
                    Text(stringResource(R.string.theme_color_tap_swatch))
                }
            },
        )
    }
}

private fun androidx.compose.material3.ColorScheme.colorForRole(role: String): Color =
    when (role) {
        "primary" -> primary
        "onPrimary" -> onPrimary
        "secondary" -> secondary
        "onSecondary" -> onSecondary
        "tertiary" -> tertiary
        "onTertiary" -> onTertiary
        "background" -> background
        "onBackground" -> onBackground
        "surface" -> surface
        "onSurface" -> onSurface
        "surfaceVariant" -> surfaceVariant
        "onSurfaceVariant" -> onSurfaceVariant
        "outline" -> outline
        "inversePrimary" -> inversePrimary
        "tertiaryContainer" -> tertiaryContainer
        "onTertiaryContainer" -> onTertiaryContainer
        "error" -> error
        "onError" -> onError
        "errorContainer" -> errorContainer
        "onErrorContainer" -> onErrorContainer
        else -> Color.Magenta
    }
