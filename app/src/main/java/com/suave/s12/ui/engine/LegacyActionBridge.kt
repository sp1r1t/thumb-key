package com.suave.s12.ui.engine

import com.suave.s12.IMEService
import com.suave.s12.utils.KeyAction
import com.suave.s12.utils.KeyboardDefinitionSettings
import com.suave.s12.utils.KeyboardPosition
import com.suave.s12.utils.performKeyAction

/**
 * Unwraps [com.suave.s12.engine.intent.KeyIntent.LegacyAction] by calling straight into the old
 * `performKeyAction` dispatcher - see that intent's doc for why this bridge exists and exactly
 * what it's scoped to (settings, clipboard, emoji picker, numeric-layout switch, IME/language
 * switch - not the full old `KeyAction` surface). Every callback `performKeyAction` can invoke
 * for an action *outside* that curated set (shift/ctrl/alt mode toggles, caps lock, auto-
 * capitalize, key-event notification) is a safe no-op here, verified by reading every branch
 * `performKeyAction` has for the actions this bridge actually forwards.
 */
fun dispatchLegacyAction(
    action: KeyAction,
    ime: IMEService,
    onToggleHideLetters: () -> Unit,
    onToggleEmojiMode: (enable: Boolean) -> Unit,
    onToggleNumericMode: (enable: Boolean) -> Unit,
    onSwitchLanguage: () -> Unit,
    onChangePosition: ((old: KeyboardPosition) -> KeyboardPosition) -> Unit,
) {
    performKeyAction(
        action = action,
        ime = ime,
        autoCapitalize = false,
        keyboardSettings = KeyboardDefinitionSettings(),
        onToggleShiftMode = {},
        onToggleCtrlMode = {},
        onToggleAltMode = {},
        onToggleNumericMode = onToggleNumericMode,
        onToggleEmojiMode = onToggleEmojiMode,
        onToggleClipboardMode = {},
        onToggleCapsLock = {},
        onToggleHideLetters = onToggleHideLetters,
        onAutoCapitalize = {},
        onSwitchLanguage = onSwitchLanguage,
        onChangePosition = onChangePosition,
        onKeyEvent = {},
    )
}
