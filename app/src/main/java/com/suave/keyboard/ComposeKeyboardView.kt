package com.suave.keyboard

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.lifecycleScope
import com.suave.keyboard.db.AppSettingsRepository
import com.suave.keyboard.db.isCredentialStorageUnlocked
import com.suave.keyboard.layout.LayoutRegistry
import com.suave.keyboard.ui.engine.EngineKeyboardScreen
import com.suave.keyboard.ui.engine.toggleHideLabels
import com.suave.keyboard.ui.theme.SuaveTheme
import com.suave.keyboard.utils.KeyboardPosition
import com.suave.keyboard.utils.toBool
import kotlinx.coroutines.launch

@SuppressLint("ViewConstructor")
class ComposeKeyboardView(
    context: Context,
    private val settingsRepo: AppSettingsRepository,
) : AbstractComposeView(context) {
    @Composable
    override fun Content() {
        val settingsState = settingsRepo.appSettings.observeAsState()
        val settings by settingsState
        val ctx = context as IMEService
        val clipboardRepository =
            if (isCredentialStorageUnlocked(ctx.applicationContext)) {
                (ctx.applicationContext as SuaveApplication).clipboardRepository
            } else {
                null
            }

        SuaveTheme(
            settings = settings,
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                EngineKeyboardScreen(
                    settings = settings,
                    onSwitchLanguage = {
                        ctx.lifecycleScope.launch {
                            val state = settingsState.value
                            state?.let { s ->
                                val layouts = LayoutRegistry.enabledFromDb(ctx, s.keyboardLayouts)
                                if (layouts.size < 2) return@let
                                val current = LayoutRegistry.byId(ctx, s.keyboardLayout)
                                val index = layouts.indexOfFirst { it.id == current.id }.let { if (it < 0) 0 else it }
                                val next = layouts[(index + 1).mod(layouts.size)]
                                val s2 = s.copy(keyboardLayout = next.id)
                                settingsRepo.update(s2)
                                if (s.showToastOnLayoutSwitch.toBool()) {
                                    ctx.showNotice(next.title)
                                }
                            }
                        }
                    },
                    onChangePosition = { f ->
                        ctx.lifecycleScope.launch {
                            val state = settingsState.value
                            state?.let { s ->
                                val current =
                                    KeyboardPosition.entries.getOrElse(s.position) { KeyboardPosition.Center }
                                val next = f(current)
                                if (next == current) return@let
                                settingsRepo.update(s.copy(position = next.ordinal))
                                if (s.showToastOnLayoutSwitch.toBool()) {
                                    ctx.showNotice(context.getString(next.resId))
                                }
                            }
                        }
                    },
                    onToggleHideLetters = {
                        ctx.lifecycleScope.launch {
                            val state = settingsState.value
                            state?.let { s ->
                                settingsRepo.update(toggleHideLabels(s))
                            }
                        }
                    },
                    clipboardRepository = clipboardRepository,
                )
            }
        }
    }
}
