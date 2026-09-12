package com.suave.keyboard.ui.components.settings.appearance

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.suave.keyboard.R
import com.suave.keyboard.db.AppSettingsViewModel
import com.suave.keyboard.db.DEFAULT_THEME_COLOR
import com.suave.keyboard.ui.components.common.SettingsScreenBody
import com.suave.keyboard.ui.components.common.SettingsSection
import com.suave.keyboard.ui.theme.NamedTheme
import com.suave.keyboard.ui.theme.ThemeRegistry
import com.suave.keyboard.ui.theme.ThemeStore
import com.suave.keyboard.utils.SimpleTopAppBar
import com.suave.keyboard.utils.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
) {
    Log.d(TAG, "Got to themes activity")

    val context = LocalContext.current
    val settings by appSettingsViewModel.appSettings.observeAsState()
    val store = remember { ThemeStore.get(context) }
    var refresh by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ThemeRegistry.ensureLoaded(context)
    store.loadIntoRegistry()
    // refresh is read so the list recomposes after import/delete/duplicate.
    val userThemes = remember(refresh) { store.listIndex() }
    val currentId = settings?.themeColor ?: DEFAULT_THEME_COLOR

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                try {
                    val json =
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                                ?: throw IllegalStateException("Could not read file")
                        }
                    val theme = withContext(Dispatchers.IO) { store.importJson(json) }
                    refresh++
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.theme_imported, theme.title),
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
            SimpleTopAppBar(text = stringResource(R.string.themes), navController = navController)
        },
        content = { padding ->
            SettingsScreenBody(padding = padding) {
                ProvidePreferenceTheme {
                    SettingsSection(title = stringResource(R.string.theme_actions)) {
                        Preference(
                            title = { Text(stringResource(R.string.theme_duplicate_current)) },
                            summary = {
                                Text(
                                    stringResource(
                                        R.string.theme_duplicate_current_summary,
                                        ThemeRegistry.title(context, currentId),
                                    ),
                                )
                            },
                            icon = {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                            },
                            onClick = {
                                scope.launch {
                                    try {
                                        val theme =
                                            withContext(Dispatchers.IO) {
                                                store.duplicateFrom(currentId)
                                            }
                                        refresh++
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.theme_duplicated, theme.title),
                                        )
                                        navController.navigate("themeEditor/${theme.id}")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(
                                            context.getString(
                                                R.string.theme_duplicate_failed,
                                                e.message ?: "",
                                            ),
                                        )
                                    }
                                }
                            },
                        )
                        Preference(
                            title = { Text(stringResource(R.string.theme_import)) },
                            summary = { Text(stringResource(R.string.theme_import_summary)) },
                            icon = {
                                Icon(Icons.Outlined.FileUpload, contentDescription = null)
                            },
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                        )
                    }

                    SettingsSection(title = stringResource(R.string.builtin_themes)) {
                        for (id in ThemeRegistry.BUILTIN_PALETTE_IDS) {
                            val theme = ThemeRegistry.byId(context, id)
                            ThemePreviewRow(
                                title = ThemeRegistry.title(context, id),
                                theme = theme,
                                onClick = {
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(R.string.theme_builtin_read_only),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                },
                                trailing = {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val copy =
                                                    withContext(Dispatchers.IO) {
                                                        store.duplicateFrom(id)
                                                    }
                                                refresh++
                                                navController.navigate("themeEditor/${copy.id}")
                                            }
                                        },
                                    ) {
                                        Icon(
                                            Icons.Outlined.ContentCopy,
                                            contentDescription = stringResource(R.string.theme_duplicate),
                                        )
                                    }
                                },
                            )
                        }
                    }

                    if (userThemes.isNotEmpty()) {
                        SettingsSection(title = stringResource(R.string.user_themes)) {
                            for (index in userThemes) {
                                val theme = store.load(index.id) ?: continue
                                ThemePreviewRow(
                                    title = theme.title,
                                    theme = theme,
                                    onClick = { navController.navigate("themeEditor/${theme.id}") },
                                    trailing = {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        store.delete(theme.id)
                                                    }
                                                    if (settings?.themeColor == theme.id) {
                                                        // AppearanceScreen owns the full AppearanceUpdate;
                                                        // fall back by rewriting settings when needed.
                                                        settings?.let {
                                                            appSettingsViewModel.update(
                                                                it.copy(themeColor = DEFAULT_THEME_COLOR),
                                                            )
                                                        }
                                                    }
                                                    refresh++
                                                }
                                            },
                                        ) {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = stringResource(R.string.theme_delete),
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun ThemePreviewRow(
    title: String,
    theme: NamedTheme,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            RoleSwatchStrip(theme)
        }
        trailing()
    }
}

@Composable
fun RoleSwatchStrip(theme: NamedTheme) {
    val roles = theme.light.toRolePreviewColors()
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (color in roles) {
            Box(
                modifier =
                    Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color),
            )
        }
    }
}

private fun androidx.compose.material3.ColorScheme.toRolePreviewColors(): List<Color> =
    listOf(primary, secondary, tertiary, error, background, surface, outline)
