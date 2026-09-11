package com.suave.s12.engine.output

import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import com.suave.s12.engine.action.CursorDirection
import com.suave.s12.engine.action.SemanticAction
import com.suave.s12.engine.capability.EditorCapabilities
import com.suave.s12.engine.intent.CommandId
import com.suave.s12.engine.intent.ModifierId

/**
 * Executes a [SemanticAction] against a real [InputConnection], choosing the Android primitive
 * based on [EditorCapabilities] rather than guessing per-app behavior. Degrades downward: a
 * plain character prefers `commitText` (works on any BASIC_EDITABLE_TEXT+ editor) and only falls
 * back to a raw [KeyEvent] when a real modifier is involved or the editor is RAW - never
 * sideways into app-specific special cases.
 */
object OutputExecutor {
    fun execute(
        action: SemanticAction,
        capabilities: EditorCapabilities,
        inputConnection: InputConnection,
    ) {
        when (action) {
            is SemanticAction.TypeText -> {
                typeText(action, capabilities, inputConnection)
            }

            is SemanticAction.TypeCommand -> {
                typeCommand(action, inputConnection)
            }

            is SemanticAction.MoveCursor -> {
                sendArrow(action.direction, shift = false, inputConnection)
            }

            is SemanticAction.ExtendSelection -> {
                sendArrow(action.direction, shift = true, inputConnection)
            }

            SemanticAction.Noop -> {}
        }
    }

    private fun typeText(
        action: SemanticAction.TypeText,
        capabilities: EditorCapabilities,
        ic: InputConnection,
    ) {
        if (action.text.length != 1) {
            // Multi-character text (e.g. Suave's "sch"/"ch" keys) has no KeyEvent representation
            // - there's no such thing as "Ctrl+sch" or a raw KeyEvent for three characters at
            // once. Always commits as plain text; on a RAW editor this is a known gap inherited
            // unchanged from before this rewrite; only single-character keys get raw-editor
            // fallback below.
            ic.commitText(action.text, 1)
            return
        }
        val char = action.text[0]
        if (action.modifiers.isEmpty() && capabilities.supportsCommitText) {
            ic.commitText(action.text, 1)
            return
        }
        // A real modifier combo (Ctrl/Alt/Esc), or a RAW editor that doesn't observe commitText
        // at all: needs an actual KeyEvent, since commitText can't carry meta state.
        val keyCode = charToKeyCode(char)
        if (keyCode == null) {
            // No known KeyEvent code for this character (e.g. punctuation outside a-z/0-9) - the
            // only thing left to try is commitText, which silently drops any modifiers and does
            // nothing at all on a RAW editor. A real gap, not a guess at app-specific behavior.
            ic.commitText(action.text, 1)
            return
        }
        sendEscIfNeeded(action.modifiers, ic)
        sendKeyEvent(ic, keyCode, metaStateFor(action.modifiers))
    }

    private fun typeCommand(
        action: SemanticAction.TypeCommand,
        ic: InputConnection,
    ) {
        val keyCode =
            when (action.id) {
                CommandId.ENTER -> KeyEvent.KEYCODE_ENTER
                CommandId.TAB -> KeyEvent.KEYCODE_TAB
                CommandId.BACKSPACE -> KeyEvent.KEYCODE_DEL
                CommandId.DELETE_FORWARD -> KeyEvent.KEYCODE_FORWARD_DEL
                CommandId.SPACE -> KeyEvent.KEYCODE_SPACE
                CommandId.ARROW_LEFT -> KeyEvent.KEYCODE_DPAD_LEFT
                CommandId.ARROW_RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT
                CommandId.ARROW_UP -> KeyEvent.KEYCODE_DPAD_UP
                CommandId.ARROW_DOWN -> KeyEvent.KEYCODE_DPAD_DOWN
            }
        sendEscIfNeeded(action.modifiers, ic)
        val shiftFlag = if (ModifierId.SHIFT in action.modifiers) KeyEvent.META_SHIFT_ON else 0
        sendKeyEvent(ic, keyCode, metaStateFor(action.modifiers) or shiftFlag)
    }

    private fun sendArrow(
        direction: CursorDirection,
        shift: Boolean,
        ic: InputConnection,
    ) {
        val keyCode =
            when (direction) {
                CursorDirection.LEFT -> KeyEvent.KEYCODE_DPAD_LEFT
                CursorDirection.RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT
                CursorDirection.UP -> KeyEvent.KEYCODE_DPAD_UP
                CursorDirection.DOWN -> KeyEvent.KEYCODE_DPAD_DOWN
            }
        sendKeyEvent(ic, keyCode, if (shift) KeyEvent.META_SHIFT_ON else 0)
    }

    /**
     * Esc has no meta-state bit to fold into another KeyEvent the way Ctrl/Alt do - send it as
     * its own event first. This is also the standard "Meta via Escape" terminal convention
     * (Emacs and most terminals already treat "Esc x" as "M-x"), so it reads as a real Meta
     * combo on the receiving end, not just a lookalike.
     */
    private fun sendEscIfNeeded(
        modifiers: Set<ModifierId>,
        ic: InputConnection,
    ) {
        if (ModifierId.ESC in modifiers) sendKeyEvent(ic, KeyEvent.KEYCODE_ESCAPE, 0)
    }

    private fun metaStateFor(modifiers: Set<ModifierId>): Int {
        var meta = 0
        if (ModifierId.CTRL in modifiers) meta = meta or KeyEvent.META_CTRL_ON
        if (ModifierId.ALT in modifiers) meta = meta or KeyEvent.META_ALT_ON
        return meta
    }

    private fun sendKeyEvent(
        ic: InputConnection,
        keyCode: Int,
        metaState: Int,
    ) {
        val now = System.currentTimeMillis()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, metaState))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, metaState))
    }

    private fun charToKeyCode(char: Char): Int? {
        val lower = char.lowercaseChar()
        return when {
            lower in 'a'..'z' -> KeyEvent.KEYCODE_A + (lower - 'a')
            lower in '0'..'9' -> KeyEvent.KEYCODE_0 + (lower - '0')
            else -> null
        }
    }
}
