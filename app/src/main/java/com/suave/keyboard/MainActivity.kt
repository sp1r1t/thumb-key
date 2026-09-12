package com.suave.keyboard

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.suave.keyboard.db.AppDB
import com.suave.keyboard.db.AppSettingsRepository
import com.suave.keyboard.db.AppSettingsViewModel
import com.suave.keyboard.db.AppSettingsViewModelFactory
import com.suave.keyboard.db.ClipboardDB
import com.suave.keyboard.db.ClipboardRepository
import com.suave.keyboard.layout.LayoutRegistry
import com.suave.keyboard.layout.UserLayoutStore
import com.suave.keyboard.ui.components.common.ShowChangelog
import com.suave.keyboard.ui.components.settings.SettingsScreen
import com.suave.keyboard.ui.components.settings.about.AboutScreen
import com.suave.keyboard.ui.components.settings.backupandrestore.BackupAndRestoreScreen
import com.suave.keyboard.ui.components.settings.behavior.BehaviorScreen
import com.suave.keyboard.ui.components.settings.clipboard.ClipboardSettingsScreen
import com.suave.keyboard.ui.components.settings.appearance.AppearanceScreen
import com.suave.keyboard.ui.components.settings.appearance.ThemeEditorScreen
import com.suave.keyboard.ui.components.settings.appearance.ThemesScreen
import com.suave.keyboard.ui.components.settings.layouts.LayoutEditorScreen
import com.suave.keyboard.ui.components.settings.layouts.LayoutsScreen
import com.suave.keyboard.ui.components.settings.other.OtherSettingsScreen
import com.suave.keyboard.ui.components.settings.suggestions.SuggestionsSettingsScreen
import com.suave.keyboard.ui.components.setup.SetupScreen
import com.suave.keyboard.ui.theme.SuaveTheme
import com.suave.keyboard.ui.theme.ThemeRegistry
import com.suave.keyboard.utils.ANIMATION_SPEED
import com.suave.keyboard.utils.getImeNames
import splitties.systemservices.inputMethodManager

class SuaveApplication : Application() {
    // New instance each time so a Direct Boot reopen of AppDB (after first unlock
    // migrates settings out of credential storage) is not stuck on a closed DAO.
    val appSettingsRepository: AppSettingsRepository
        get() = AppSettingsRepository(AppDB.getDatabase(this).appSettingsDao())

    private val clipboardDatabase by lazy { ClipboardDB.getDatabase(this) }
    val clipboardRepository by lazy {
        ClipboardRepository(
            clipboardDatabase.clipboardItemDao(),
            AppDB.getDatabase(this).appSettingsDao(),
            this,
        )
    }

    val userLayoutStore: UserLayoutStore by lazy { UserLayoutStore.create(this) }

    override fun onCreate() {
        super.onCreate()
        LayoutRegistry.ensureLoaded(this)
        ThemeRegistry.ensureLoaded(this)
        try {
            kotlinx.coroutines.runBlocking {
                userLayoutStore.loadIntoRegistry()
            }
        } catch (e: Exception) {
            android.util.Log.e("suave", "Failed to load user layouts: ${e.message}")
        }
    }
}

class MainActivity : AppCompatActivity() {
    private val appSettingsViewModel: AppSettingsViewModel by viewModels {
        AppSettingsViewModelFactory((application as SuaveApplication).appSettingsRepository)
    }

    override fun onStart() {
        super.onStart()
        SettingsSession.enter()
    }

    override fun onStop() {
        SettingsSession.leave()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val settings by appSettingsViewModel.appSettings.observeAsState()
            val ctx = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            // Re-read IME state after system settings (ON_RESUME) and after the
            // in-place keyboard picker (DEFAULT_INPUT_METHOD change; no pause).
            var imeRefresh by remember { mutableIntStateOf(0) }
            DisposableEffect(lifecycleOwner, ctx) {
                val bump = { imeRefresh++ }
                val lifecycleObserver =
                    LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            bump()
                        }
                    }
                val defaultImeObserver =
                    object : ContentObserver(Handler(Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean) {
                            bump()
                        }
                    }
                val inputMethodChangedReceiver =
                    object : BroadcastReceiver() {
                        override fun onReceive(
                            context: Context?,
                            intent: Intent?,
                        ) {
                            bump()
                        }
                    }
                lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
                ctx.contentResolver.registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD),
                    false,
                    defaultImeObserver,
                )
                ContextCompat.registerReceiver(
                    ctx,
                    inputMethodChangedReceiver,
                    IntentFilter(Intent.ACTION_INPUT_METHOD_CHANGED),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                    ctx.contentResolver.unregisterContentObserver(defaultImeObserver)
                    ctx.unregisterReceiver(inputMethodChangedReceiver)
                }
            }

            // imeRefresh is read so composition re-runs after IME changes.
            val imeNames = ctx.getImeNames().also { imeRefresh }
            val suaveEnabled =
                inputMethodManager.enabledInputMethodList.any {
                    imeNames.contains(it.id)
                }
            val selectedName =
                Settings.Secure.getString(
                    ctx.contentResolver,
                    Settings.Secure.DEFAULT_INPUT_METHOD,
                )
            val suaveSelected = imeNames.contains(selectedName)

            val startDestination by remember {
                mutableStateOf(
                    if (!suaveEnabled) {
                        "setup"
                    } else {
                        intent.extras?.getString("startRoute") ?: "settings"
                    },
                )
            }

            SuaveTheme(
                settings = settings,
            ) {
                val navController = rememberNavController()

                if (startDestination == "settings") {
                    ShowChangelog(appSettingsViewModel = appSettingsViewModel)
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    enterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(ANIMATION_SPEED),
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(ANIMATION_SPEED),
                        )
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(ANIMATION_SPEED),
                        )
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(ANIMATION_SPEED),
                        )
                    },
                ) {
                    composable(
                        route = "setup",
                    ) {
                        SetupScreen(
                            navController = navController,
                            suaveEnabled = suaveEnabled,
                            suaveSelected = suaveSelected,
                        )
                    }
                    composable(route = "settings") {
                        SettingsScreen(
                            navController = navController,
                            appSettingsViewModel = appSettingsViewModel,
                            suaveEnabled = suaveEnabled,
                            suaveSelected = suaveSelected,
                        )
                    }
                    composable(route = "appearance") {
                        AppearanceScreen(
                            navController = navController,
                            appSettingsViewModel = appSettingsViewModel,
                        )
                    }
                    composable(route = "themes") {
                        ThemesScreen(
                            navController = navController,
                            appSettingsViewModel = appSettingsViewModel,
                        )
                    }
                    composable(route = "themeEditor/{themeId}") { entry ->
                        val themeId = entry.arguments?.getString("themeId") ?: return@composable
                        ThemeEditorScreen(
                            navController = navController,
                            themeId = themeId,
                        )
                    }
                    composable(route = "behavior") {
                        BehaviorScreen(
                            navController = navController,
                            appSettingsViewModel = appSettingsViewModel,
                        )
                    }
                    composable(route = "suggestionsSettings") {
                        SuggestionsSettingsScreen(
                            navController = navController,
                            appSettingsViewModel = appSettingsViewModel,
                        )
                    }
                    composable(route = "clipboardSettings") {
                        ClipboardSettingsScreen(
                            navController = navController,
                            appSettingsViewModel = appSettingsViewModel,
                            clipboardRepository = (application as SuaveApplication).clipboardRepository,
                        )
                    }
                    composable(
                        route = "about",
                    ) {
                        AboutScreen(
                            navController = navController,
                        )
                    }
                    composable(
                        route = "backupAndRestore",
                    ) {
                        BackupAndRestoreScreen(
                            navController = navController,
                            appSettingsViewModel = appSettingsViewModel,
                        )
                    }
                    composable(route = "otherSettings") {
                        OtherSettingsScreen(
                            navController = navController,
                            appSettingsViewModel = appSettingsViewModel,
                        )
                    }
                    composable(route = "layouts") {
                        LayoutsScreen(
                            navController = navController,
                            appSettingsViewModel = appSettingsViewModel,
                        )
                    }
                    composable(route = "layoutEditor/{layoutId}") { entry ->
                        LayoutEditorScreen(
                            navController = navController,
                            appSettingsViewModel = appSettingsViewModel,
                            editId = entry.arguments?.getString("layoutId"),
                        )
                    }
                    composable(route = "layoutCreate/{sourceId}") { entry ->
                        LayoutEditorScreen(
                            navController = navController,
                            appSettingsViewModel = appSettingsViewModel,
                            createFrom = entry.arguments?.getString("sourceId"),
                        )
                    }
                }
            }
        }
    }
}
