package com.suave.keyboard.ui.engine

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import com.suave.keyboard.IMEService
import com.suave.keyboard.MainActivity
import com.suave.keyboard.R
import com.suave.keyboard.engine.action.SemanticAction
import com.suave.keyboard.engine.capability.EditorCapabilities
import com.suave.keyboard.engine.capability.EditorCapabilityLevel
import com.suave.keyboard.engine.intent.CommandId
import com.suave.keyboard.engine.intent.ModifierId
import com.suave.keyboard.engine.output.ClipboardPaste
import com.suave.keyboard.engine.output.OutputExecutor
import com.suave.keyboard.layout.ActiveLayer
import com.suave.keyboard.utils.KeyboardPosition

/**
 * Host callbacks for commands that are not editor I/O: settings, layout switch, IME picker,
 * keyboard position, hide-letters, and layer switches (numeric / emoji / abc / clipboard).
 */
data class AppCommandHost(
    val onToggleHideLetters: () -> Unit,
    val onSwitchLanguage: () -> Unit,
    val onChangePosition: ((old: KeyboardPosition) -> KeyboardPosition) -> Unit,
    val onSelectLayer: (ActiveLayer) -> Unit,
    val onSwitchLayer: (String) -> Unit,
    val onToggleEmojiLayer: () -> Unit,
    val onToggleClipboardHistory: () -> Unit,
)

/**
 * Single execute entry for every [SemanticAction] the dispatcher emits. Key-event commands and
 * typed text go through [OutputExecutor]; clipboard and app commands are this class's job so
 * Copy is the same kind of command as Enter, just with different primitives.
 *
 * Attached modifiers on clipboard/app commands are ignored at execution (Ctrl+Copy is still
 * Copy). The dispatcher still consumes one-shot modifiers, because pressing Copy is a completed
 * key press. RAW editors (Termux) get the terminal's own copy/paste combos rather than the
 * context-menu actions they do not implement.
 */
object ActionExecutor {
    private const val SELECT_ALL_SETTLE_MS = 100L

    fun execute(
        action: SemanticAction,
        capabilities: EditorCapabilities,
        ime: IMEService,
        host: AppCommandHost,
    ) {
        if (action is SemanticAction.SwitchLayer) {
            host.onSwitchLayer(action.layerId)
            return
        }
        if (action is SemanticAction.TypeCommand && !action.id.isKeyEventCommand()) {
            executeSpecialCommand(action.id, capabilities, ime, host)
            return
        }
        val ignoreCount = cursorMovesToIgnore(action)
        if (ignoreCount > 0) {
            ime.ignoreNextCursorMove(ignoreCount)
        }
        val ic = ime.currentInputConnection ?: return
        OutputExecutor.execute(action, capabilities, ic)
    }

    /**
     * ReplaceLastText does delete + commit and often yields two cursor updates; ignoring only
     * one lets the second look like a user move and resets space multitap.
     */
    private fun cursorMovesToIgnore(action: SemanticAction): Int =
        when (action) {
            is SemanticAction.ReplaceLastText -> 2
            is SemanticAction.TypeText -> 1
            is SemanticAction.TypeCommand -> if (action.id == CommandId.SPACE) 1 else 0
            else -> 0
        }

    private fun executeSpecialCommand(
        id: CommandId,
        capabilities: EditorCapabilities,
        ime: IMEService,
        host: AppCommandHost,
    ) {
        when (id) {
            CommandId.COPY -> copy(ime, capabilities)
            CommandId.CUT -> cut(ime, capabilities)
            CommandId.PASTE -> ClipboardPaste.execute(ime, capabilities)
            CommandId.SELECT_ALL -> selectAll(ime)
            CommandId.UNDO -> sendEditorShortcut(ime, capabilities, "z", setOf(ModifierId.CTRL))
            CommandId.REDO -> sendEditorShortcut(ime, capabilities, "z", setOf(ModifierId.CTRL, ModifierId.SHIFT))
            CommandId.GOTO_SETTINGS -> openSettings(ime)
            CommandId.TOGGLE_HIDE_LETTERS -> host.onToggleHideLetters()
            CommandId.SWITCH_IME -> showImePicker(ime)
            CommandId.SWITCH_IME_VOICE -> switchToVoiceIme(ime)
            CommandId.SWITCH_LANGUAGE -> host.onSwitchLanguage()
            CommandId.MOVE_KEYBOARD -> cycleKeyboardRight(host)
            CommandId.TOGGLE_EMOJI_MODE -> host.onToggleEmojiLayer()
            CommandId.TOGGLE_NUMERIC_MODE -> host.onSelectLayer(ActiveLayer.Numeric)
            CommandId.TOGGLE_ABC_MODE -> host.onSelectLayer(ActiveLayer.Main)
            CommandId.TOGGLE_CLIPBOARD_HISTORY -> host.onToggleClipboardHistory()
            CommandId.IME_ACTION -> performImeAction(ime)
            CommandId.HIDE_KEYBOARD -> ime.requestHideSelf(0)
            else -> {}
        }
    }

    private fun performImeAction(ime: IMEService) {
        val editor = ime.currentInputEditorInfo ?: return
        val action = editor.imeOptions and android.view.inputmethod.EditorInfo.IME_MASK_ACTION
        if (action == android.view.inputmethod.EditorInfo.IME_ACTION_NONE ||
            action == android.view.inputmethod.EditorInfo.IME_ACTION_UNSPECIFIED
        ) {
            val ic = ime.currentInputConnection ?: return
            OutputExecutor.execute(
                SemanticAction.TypeCommand(CommandId.ENTER, emptySet()),
                com.suave.keyboard.engine.capability.EditorCapabilityResolver.resolve(editor),
                ic,
            )
            return
        }
        ime.currentInputConnection?.performEditorAction(action)
    }

    private fun copy(
        ime: IMEService,
        capabilities: EditorCapabilities,
    ) {
        if (capabilities.level == EditorCapabilityLevel.RAW) {
            sendEditorShortcut(ime, capabilities, "c", setOf(ModifierId.CTRL, ModifierId.ALT))
            return
        }
        fun performCopy() {
            val ic = ime.currentInputConnection ?: return
            if (ime.clipboardUsePrivate()) {
                val text = ic.getSelectedText(0) ?: return
                ime.clipboardAddPrivateClip(text.toString())?.let {
                    showActionNotice(
                        ime,
                        ime.showToastOnCopy(),
                        R.string.copy,
                        detailRes = R.string.clipboard_private_badge,
                    )
                }
            } else {
                ic.performContextMenuAction(android.R.id.copy)
                showActionNotice(ime, ime.showToastOnCopy(), R.string.copy)
            }
        }
        withSelectionOrAll(ime, ::performCopy)
    }

    private fun cut(
        ime: IMEService,
        capabilities: EditorCapabilities,
    ) {
        if (capabilities.level == EditorCapabilityLevel.RAW) {
            sendEditorShortcut(ime, capabilities, "x", setOf(ModifierId.CTRL, ModifierId.ALT))
            return
        }
        fun performCut() {
            val ic = ime.currentInputConnection ?: return
            if (ime.clipboardUsePrivate()) {
                val text = ic.getSelectedText(0) ?: return
                ime.clipboardAddPrivateClip(text.toString())?.let {
                    ic.commitText("", 1)
                    showActionNotice(
                        ime,
                        ime.showToastOnCut(),
                        R.string.cut,
                        detailRes = R.string.clipboard_private_badge,
                    )
                }
            } else {
                ic.performContextMenuAction(android.R.id.cut)
                showActionNotice(ime, ime.showToastOnCut(), R.string.cut)
            }
        }
        withSelectionOrAll(ime, ::performCut)
    }

    private fun selectAll(ime: IMEService) {
        ime.currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
    }

    /**
     * If nothing is selected, select everything first (the pre-rewrite Copy/Cut behavior), then
     * run [action] after the editor has had a beat to apply select-all.
     */
    private fun withSelectionOrAll(
        ime: IMEService,
        action: () -> Unit,
    ) {
        val ic = ime.currentInputConnection ?: return
        if (ic.getSelectedText(0).isNullOrEmpty()) {
            ic.performContextMenuAction(android.R.id.selectAll)
            Handler(Looper.getMainLooper()).postDelayed(action, SELECT_ALL_SETTLE_MS)
        } else {
            action()
        }
    }

    private fun sendEditorShortcut(
        ime: IMEService,
        capabilities: EditorCapabilities,
        key: String,
        modifiers: Set<ModifierId>,
    ) {
        val ic = ime.currentInputConnection ?: return
        OutputExecutor.execute(SemanticAction.TypeText(key, modifiers), capabilities, ic)
    }

    private fun openSettings(ime: IMEService) {
        val intent = Intent(ime, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("startRoute", "settings")
        ime.startActivity(intent)
    }

    private fun showImePicker(ime: IMEService) {
        val imeManager = ime.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imeManager.showInputMethodPicker()
    }

    private fun switchToVoiceIme(ime: IMEService) {
        val imeManager = ime.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val list: List<InputMethodInfo> = imeManager.enabledInputMethodList
        for (el in list) {
            for (i in 0 until el.subtypeCount) {
                if (el.getSubtypeAt(i).mode != "voice") continue
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ime.switchInputMethod(el.id)
                } else {
                    ime.window.window?.let { window ->
                        @Suppress("DEPRECATION")
                        imeManager.setInputMethod(window.attributes.token, el.id)
                    }
                }
            }
        }
    }

    private fun cycleKeyboardRight(host: AppCommandHost) {
        host.onChangePosition { it }
    }

    private fun showActionNotice(
        ime: IMEService,
        enabled: Boolean,
        textRes: Int,
        detailRes: Int? = null,
    ) {
        if (shouldShowActionNotice(enabled, succeeded = true)) {
            ime.showNotice(
                text = ime.getString(textRes),
                detail = detailRes?.let { ime.getString(it) },
            )
        }
    }
}

/** Overlay notice only when the setting is on and the copy/cut actually happened. */
fun shouldShowActionNotice(
    enabled: Boolean,
    succeeded: Boolean,
): Boolean = enabled && succeeded
