package com.suave.s12

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.lifecycleScope
import com.suave.s12.db.AppSettingsRepository
import com.suave.s12.layout.BuiltinLayouts
import com.suave.s12.ui.engine.EngineKeyboardScreen
import com.suave.s12.ui.theme.ThumbkeyTheme
import com.suave.s12.utils.KeyboardPosition
import com.suave.s12.utils.toBool
import com.suave.s12.utils.toInt
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

        ThumbkeyTheme(
            settings = settings,
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                EngineKeyboardScreen(
                    settings = settings,
                    // No emoji/numeric screen exists on the new engine yet - those keys are
                    // first-class commands and fire these callbacks, there's just nothing
                    // further to switch to here.
                    onToggleEmojiMode = {},
                    onToggleNumericMode = {},
                    onSwitchLanguage = {
                        ctx.lifecycleScope.launch {
                            val state = settingsState.value
                            state?.let { s ->
                                val layouts = BuiltinLayouts.enabledFromDb(s.keyboardLayouts)
                                val current = BuiltinLayouts.byIndex(s.keyboardLayout)
                                val index = layouts.indexOfFirst { it.id == current.id }.let { if (it < 0) 0 else it }
                                val next = layouts[(index + 1).mod(layouts.size)]
                                val nextIndex = BuiltinLayouts.ALL.indexOfFirst { it.id == next.id }.coerceAtLeast(0)
                                val s2 = s.copy(keyboardLayout = nextIndex)
                                settingsRepo.update(s2)

                                if (s.showToastOnLayoutSwitch.toBool()) {
                                    Toast
                                        .makeText(context, next.title, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }
                    },
                    onChangePosition = { f ->
                        ctx.lifecycleScope.launch {
                            val state = settingsState.value
                            state?.let { s ->
                                val nextPosition = f(KeyboardPosition.entries[s.position]).ordinal
                                val s2 = s.copy(position = nextPosition)
                                settingsRepo.update(s2)
                            }
                        }
                    },
                    onToggleHideLetters = {
                        ctx.lifecycleScope.launch {
                            val state = settingsState.value
                            state?.let { s ->
                                val newHideLetters = (!s.hideLetters.toBool()).toInt()
                                val s2 = s.copy(hideLetters = newHideLetters)
                                settingsRepo.update(s2)
                            }
                        }
                    },
                )
            }
        }
    }
}
