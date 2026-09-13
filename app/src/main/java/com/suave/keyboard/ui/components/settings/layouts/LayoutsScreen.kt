package com.suave.keyboard.ui.components.settings.layouts

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.suave.keyboard.R
import com.suave.keyboard.SuaveApplication
import com.suave.keyboard.db.AppSettingsViewModel
import com.suave.keyboard.db.DEFAULT_KEYBOARD_LAYOUT
import com.suave.keyboard.db.LayoutsUpdate
import com.suave.keyboard.layout.LAYOUT_SOURCE_USER
import com.suave.keyboard.layout.LayoutRegistry
import com.suave.keyboard.layout.LayoutSort
import com.suave.keyboard.layout.StartLayoutChoice
import com.suave.keyboard.layout.UserLayoutIndex
import com.suave.keyboard.layout.filterAvailableLayouts
import com.suave.keyboard.layout.startLayoutChoices
import com.suave.keyboard.layout.tagsFromIndex
import com.suave.keyboard.ui.components.common.SettingTitle
import com.suave.keyboard.ui.components.common.SettingsScreenBody
import com.suave.keyboard.ui.components.common.SettingsSection
import com.suave.keyboard.utils.SimpleTopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceTheme
import me.zhanghai.compose.preference.SwitchPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutsScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SuaveApplication
    val store = app.userLayoutStore
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by appSettingsViewModel.appSettings.observeAsState()
    val index by store.observeIndex().observeAsState(emptyList())

    var pendingExportId by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<UserLayoutIndex?>(null) }
    var showAddLayout by remember { mutableStateOf(false) }
    var availableQuery by remember { mutableStateOf("") }
    var availableSort by remember { mutableStateOf(LayoutSort.TITLE) }

    val enabledIds =
        remember(settings?.keyboardLayouts) {
            settings
                ?.keyboardLayouts
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                .orEmpty()
                .ifEmpty { setOf(DEFAULT_KEYBOARD_LAYOUT) }
        }
    val activeId = settings?.keyboardLayout ?: DEFAULT_KEYBOARD_LAYOUT

    val layoutRows =
        remember(index) {
            index.filter { it.source == LAYOUT_SOURCE_USER }
        }
    val visibleRows =
        remember(layoutRows, availableQuery, availableSort) {
            filterAvailableLayouts(layoutRows, availableQuery, availableSort)
        }

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json"),
        ) { uri: Uri? ->
            val id = pendingExportId
            pendingExportId = null
            if (uri == null || id == null) return@rememberLauncherForActivityResult
            scope.launch {
                try {
                    val json = store.exportToJson(id)
                    withContext(Dispatchers.IO) {
                        ctx.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        } ?: error("Could not open export uri")
                    }
                    Toast.makeText(ctx, ctx.getString(R.string.layout_exported), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast
                        .makeText(
                            ctx,
                            ctx.getString(R.string.layout_export_failed, e.message ?: ""),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                try {
                    val json =
                        withContext(Dispatchers.IO) {
                            ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                                ?: error("Could not read file")
                        }
                    val layout = store.importFromJson(json)
                    val next = enabledIds + layout.id
                    appSettingsViewModel.updateLayouts(
                        LayoutsUpdate(
                            id = 1,
                            keyboardLayout = activeId,
                            keyboardLayouts = next.joinToString(","),
                        ),
                    )
                    Toast
                        .makeText(
                            ctx,
                            ctx.getString(R.string.layout_imported, layout.title),
                            Toast.LENGTH_SHORT,
                        ).show()
                } catch (e: Exception) {
                    Toast
                        .makeText(
                            ctx,
                            ctx.getString(R.string.layout_import_failed, e.message ?: ""),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        }

    fun openCreateFrom(sourceId: String) {
        showAddLayout = false
        navController.navigate("layoutCreate/$sourceId")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleTopAppBar(
                text = stringResource(R.string.layouts),
                navController = navController,
            )
        },
    ) { padding ->
        SettingsScreenBody(padding = padding) {
            ProvidePreferenceTheme {
                SettingsSection(
                    title = stringResource(R.string.layouts_section_manage),
                    initiallyExpanded = true,
                ) {
                    Preference(
                        title = { Text(stringResource(R.string.layout_create_new)) },
                        summary = {
                            Text(stringResource(R.string.layout_create_new_summary))
                        },
                        icon = {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                        },
                        onClick = { showAddLayout = true },
                    )
                    Preference(
                        title = { Text(stringResource(R.string.layout_import)) },
                        summary = { Text(stringResource(R.string.layout_import_summary)) },
                        icon = {
                            Icon(Icons.Outlined.FileDownload, contentDescription = null)
                        },
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                    )
                }

                SettingsSection(
                    title = stringResource(R.string.layouts_section_available),
                    initiallyExpanded = true,
                ) {
                    OutlinedTextField(
                        value = availableQuery,
                        onValueChange = { availableQuery = it },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true,
                        label = { Text(stringResource(R.string.layout_search_available)) },
                    )
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = availableSort == LayoutSort.TITLE,
                            onClick = { availableSort = LayoutSort.TITLE },
                            label = { Text(stringResource(R.string.layout_sort_title)) },
                        )
                        FilterChip(
                            selected = availableSort == LayoutSort.UPDATED,
                            onClick = { availableSort = LayoutSort.UPDATED },
                            label = { Text(stringResource(R.string.layout_sort_updated)) },
                        )
                    }
                    Text(
                        text =
                            stringResource(
                                if (availableSort == LayoutSort.TITLE) {
                                    R.string.layout_sort_summary_title
                                } else {
                                    R.string.layout_sort_summary_updated
                                },
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    if (visibleRows.isEmpty()) {
                        Text(
                            text =
                                stringResource(
                                    if (layoutRows.isEmpty()) {
                                        R.string.layout_available_empty
                                    } else {
                                        R.string.layout_search_empty
                                    },
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                    for (entry in visibleRows) {
                        LayoutIndexRow(
                            entry = entry,
                            enabled = entry.id in enabledIds,
                            active = entry.id == activeId,
                            onToggleEnabled = { on ->
                                val next =
                                    if (on) {
                                        enabledIds + entry.id
                                    } else {
                                        (enabledIds - entry.id).ifEmpty { setOf(DEFAULT_KEYBOARD_LAYOUT) }
                                    }
                                val newActive =
                                    when {
                                        activeId in next -> activeId
                                        else -> next.first()
                                    }
                                appSettingsViewModel.updateLayouts(
                                    LayoutsUpdate(
                                        id = 1,
                                        keyboardLayout = newActive,
                                        keyboardLayouts = next.joinToString(","),
                                    ),
                                )
                            },
                            onSelectActive = {
                                val next = enabledIds + entry.id
                                appSettingsViewModel.updateLayouts(
                                    LayoutsUpdate(
                                        id = 1,
                                        keyboardLayout = entry.id,
                                        keyboardLayouts = next.joinToString(","),
                                    ),
                                )
                            },
                            onEdit = {
                                navController.navigate("layoutEditor/${entry.id}")
                            },
                            onDuplicate = {
                                openCreateFrom(entry.id)
                            },
                            onExport = {
                                pendingExportId = entry.id
                                exportLauncher.launch("${entry.id}.json")
                            },
                            onShare = {
                                scope.launch {
                                    try {
                                        val json = store.exportToJson(entry.id)
                                        val send =
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "application/json"
                                                putExtra(Intent.EXTRA_TEXT, json)
                                                putExtra(Intent.EXTRA_SUBJECT, entry.title)
                                            }
                                        ctx.startActivity(
                                            Intent.createChooser(
                                                send,
                                                ctx.getString(R.string.layout_share),
                                            ),
                                        )
                                    } catch (e: Exception) {
                                        Toast
                                            .makeText(
                                                ctx,
                                                ctx.getString(R.string.layout_export_failed, e.message ?: ""),
                                                Toast.LENGTH_LONG,
                                            ).show()
                                    }
                                }
                            },
                            onDelete = { deleteTarget = entry },
                        )
                    }
                }
            }
        }
    }

    if (showAddLayout) {
        LayoutStartPickerDialog(
            templates = LayoutRegistry.templates(ctx),
            onDismiss = { showAddLayout = false },
            onSelect = { openCreateFrom(it) },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.layout_delete_title)) },
            text = {
                Text(stringResource(R.string.layout_delete_message, target.title))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = target.id
                        deleteTarget = null
                        scope.launch {
                            try {
                                store.delete(id)
                                val next = (enabledIds - id).ifEmpty { setOf(DEFAULT_KEYBOARD_LAYOUT) }
                                val newActive = if (activeId == id) next.first() else activeId
                                appSettingsViewModel.updateLayouts(
                                    LayoutsUpdate(
                                        id = 1,
                                        keyboardLayout = newActive,
                                        keyboardLayouts = next.joinToString(","),
                                    ),
                                )
                                Toast
                                    .makeText(ctx, ctx.getString(R.string.layout_deleted), Toast.LENGTH_SHORT)
                                    .show()
                            } catch (e: Exception) {
                                Toast
                                    .makeText(
                                        ctx,
                                        ctx.getString(R.string.layout_action_failed, e.message ?: ""),
                                        Toast.LENGTH_LONG,
                                    ).show()
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun LayoutStartPickerDialog(
    templates: List<com.suave.keyboard.layout.NamedLayout>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val blankTitle = stringResource(R.string.layout_start_blank)
    val choices =
        remember(templates, query, blankTitle) {
            startLayoutChoices(templates, query, blankTitle)
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.layout_select_start_layout_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    singleLine = true,
                    label = { Text(stringResource(R.string.layout_select_start_search)) },
                )
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                ) {
                    items(choices, key = { it.id }) { choice ->
                        StartLayoutChoiceRow(
                            choice = choice,
                            onSelect = { onSelect(choice.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        dismissButton = {},
    )
}

@Composable
private fun StartLayoutChoiceRow(
    choice: StartLayoutChoice,
    onSelect: () -> Unit,
) {
    val tagsLabel =
        if (choice.tags.isEmpty()) {
            choice.id
        } else {
            choice.tags.joinToString(", ")
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect)
                .padding(vertical = 12.dp, horizontal = 4.dp),
    ) {
        Text(
            text = choice.title,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = tagsLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LayoutIndexRow(
    entry: UserLayoutIndex,
    enabled: Boolean,
    active: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectActive: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val tags = tagsFromIndex(entry.tags)
    val status =
        buildString {
            when {
                active -> append(stringResource(R.string.layout_active))
                enabled -> append(stringResource(R.string.layout_enabled))
                else -> append(stringResource(R.string.layout_disabled))
            }
            if (tags.isNotEmpty()) {
                append(" - ")
                append(tags.joinToString(", "))
            }
        }

    Column(modifier = Modifier.fillMaxWidth()) {
        SwitchPreference(
            value = enabled,
            onValueChange = onToggleEnabled,
            title = { SettingTitle(text = entry.title) },
            summary = { Text(status) },
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!active) {
                TextButton(onClick = onSelectActive) {
                    Text(stringResource(R.string.layout_use))
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.layout_edit))
            }
            IconButton(onClick = onDuplicate) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.layout_duplicate))
            }
            IconButton(onClick = onExport) {
                Icon(Icons.Outlined.FileUpload, contentDescription = stringResource(R.string.layout_export))
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.layout_share))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
