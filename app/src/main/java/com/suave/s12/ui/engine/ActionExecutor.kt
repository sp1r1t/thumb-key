package com.suave.s12.ui.engine

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.suave.s12.IMEService
import com.suave.s12.MainActivity
import com.suave.s12.R
import com.suave.s12.engine.action.SemanticAction
import com.suave.s12.engine.capability.EditorCapabilities
import com.suave.s12.engine.capability.EditorCapabilityLevel
import com.suave.s12.engine.intent.CommandId
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.engine.output.OutputExecutor
import com.suave.s12.layout.LayoutLayer
import com.suave.s12.utils.KeyboardPosition
import com.suave.s12.utils.nextVisible

/**
 * Host callbacks for commands that are not editor I/O: settings, layout switch, IME picker,
 * keyboard position, hide-letters, and layer switches (numeric / emoji / abc).
 */
data class AppCommandHost(
    val onToggleHideLetters: () -> Unit,
    val onSwitchLanguage: () -> Unit,
    val onChangePosition: ((old: KeyboardPosition) -> KeyboardPosition) -> Unit,
    val onSelectLayer: (LayoutLayer) -> Unit,
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
        if (action is SemanticAction.TypeCommand && !action.id.isKeyEventCommand()) {
            executeSpecialCommand(action.id, capabilities, ime, host)
            return
        }
        OutputExecutor.execute(action, capabilities, ime.currentInputConnection)
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
            CommandId.PASTE -> paste(ime, capabilities)
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
            CommandId.TOGGLE_NUMERIC_MODE -> host.onSelectLayer(LayoutLayer.NUMERIC)
            CommandId.TOGGLE_ABC_MODE -> host.onSelectLayer(LayoutLayer.MAIN)
            CommandId.TOGGLE_CLIPBOARD_HISTORY -> host.onToggleClipboardHistory()
            else -> {}
        }
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
                    Toast.makeText(ime, ime.getString(R.string.copy), Toast.LENGTH_SHORT).show()
                }
            } else {
                ic.performContextMenuAction(android.R.id.copy)
                Toast.makeText(ime, ime.getString(R.string.copy), Toast.LENGTH_SHORT).show()
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
                }
            } else {
                ic.performContextMenuAction(android.R.id.cut)
            }
        }
        withSelectionOrAll(ime, ::performCut)
    }

    private fun paste(
        ime: IMEService,
        capabilities: EditorCapabilities,
    ) {
        // Termux binds paste to Ctrl+Alt+V, not the Ctrl+Shift+V most other terminal emulators
        // use (see termux-app discussion #3928).
        if (capabilities.level == EditorCapabilityLevel.RAW) {
            sendEditorShortcut(ime, capabilities, "v", setOf(ModifierId.CTRL, ModifierId.ALT))
            return
        }
        val ic = ime.currentInputConnection ?: return
        if (!ime.clipboardUsePrivate()) {
            ic.performContextMenuAction(android.R.id.paste)
            return
        }
        if (ime.clipboardWasLastCopyDoneViaSystem()) {
            ic.performContextMenuAction(android.R.id.paste)
        } else {
            val text = ime.clipboardGetLastClip()
            if (!text.isNullOrEmpty()) ic.commitText(text, 1)
        }
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
        host.onChangePosition { it.nextVisible() }
    }
}
