package com.suave.s12.ui.engine

import com.suave.s12.IMEService
import com.suave.s12.engine.action.SemanticAction
import com.suave.s12.engine.capability.EditorCapabilities
import com.suave.s12.engine.capability.EditorCapabilityLevel
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.engine.output.OutputExecutor
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
    capabilities: EditorCapabilities,
    onToggleHideLetters: () -> Unit,
    onToggleEmojiMode: (enable: Boolean) -> Unit,
    onToggleNumericMode: (enable: Boolean) -> Unit,
    onSwitchLanguage: () -> Unit,
    onChangePosition: ((old: KeyboardPosition) -> KeyboardPosition) -> Unit,
) {
    // KeyAction.Paste below normally calls performContextMenuAction(android.R.id.paste) - a
    // standard EditText/TextView primitive a RAW editor (Termux) has no InputConnection support
    // for at all. Termux binds paste to a real terminal key combo instead - deliberately
    // Ctrl+Alt+V, not the Ctrl+Shift+V most other terminal emulators use (see termux-app
    // discussion #3928) - so on a RAW editor, send that combo as a raw KeyEvent through the same
    // engine pipeline a physical Ctrl+Alt+V key press would use, instead of the no-op call.
    if (action is KeyAction.Paste && capabilities.level == EditorCapabilityLevel.RAW) {
        OutputExecutor.execute(
            SemanticAction.TypeText("v", setOf(ModifierId.CTRL, ModifierId.ALT)),
            capabilities,
            ime.currentInputConnection,
        )
        return
    }
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
