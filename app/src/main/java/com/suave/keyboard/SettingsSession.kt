package com.suave.keyboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide flag that [MainActivity] (settings) is in the foreground. The IME reads this
 * to show a settings chrome bar while the user has Suave open - the keyboard is effectively
 * in "garage" mode for tweaking layouts and appearance.
 *
 * Same process as the IME on a normal install (both run in the app process), so a
 * [StateFlow] is enough; no binder or DB write required.
 */
object SettingsSession {
    private val _open = MutableStateFlow(false)
    val open: StateFlow<Boolean> = _open.asStateFlow()

    val isOpen: Boolean
        get() = _open.value

    fun enter() {
        _open.value = true
    }

    fun leave() {
        _open.value = false
    }
}
