package com.suave.s12.engine.output

import android.view.KeyEvent
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import com.suave.s12.engine.action.CursorDirection
import com.suave.s12.engine.action.SemanticAction
import com.suave.s12.engine.capability.EditorCapabilities
import com.suave.s12.engine.capability.EditorCapabilityLevel
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
    /**
     * The last position [stepSelection] wrote via `setSelection`, trusted as ground truth for the
     * next step of the *same* slide instead of re-querying the editor - see [SemanticAction]'s
     * `resetAnchor` doc for why. Deliberately a single shared cache, not scoped per key/press:
     * this object is a stateless-by-design singleton reused for every key on the keyboard, and
     * the only two callers that ever populate it (a slide's own repeated steps, always spaced
     * closely enough in time that nothing else could have touched the selection in between) don't
     * practically collide - two different slide-capable keys being slid literally simultaneously
     * with two fingers is the one scenario this cache doesn't handle correctly, accepted as a
     * rare edge case rather than threading per-key identity through this pipeline for it.
     */
    private var cachedExtent: SelectionExtent? = null

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
                typeCommand(action, capabilities, inputConnection)
            }

            is SemanticAction.MoveCursor -> {
                moveCursor(action.direction, action.resetAnchor, extend = false, capabilities, inputConnection)
            }

            is SemanticAction.ExtendSelection -> {
                moveCursor(action.direction, action.resetAnchor, extend = true, capabilities, inputConnection)
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
        capabilities: EditorCapabilities,
        ic: InputConnection,
    ) {
        val direction = arrowDirectionFor(action.id)
        // Ctrl/Alt+Arrow (e.g. terminal word-jump) still needs a real KeyEvent with those meta
        // flags, which the setSelection path below has no way to carry - only Shift-or-nothing
        // arrow presses route through it. Each is a single discrete tap, not a continuous slide,
        // so it always re-derives from the editor (resetAnchor = true) rather than trusting
        // whatever the last slide happened to leave cached.
        if (direction != null && ModifierId.CTRL !in action.modifiers && ModifierId.ALT !in action.modifiers) {
            val extend = ModifierId.SHIFT in action.modifiers
            moveCursor(direction, resetAnchor = true, extend = extend, capabilities, ic)
            return
        }
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
                CommandId.ESCAPE -> KeyEvent.KEYCODE_ESCAPE
                CommandId.CTRL -> KeyEvent.KEYCODE_CTRL_LEFT
                CommandId.ALT -> KeyEvent.KEYCODE_ALT_LEFT
                CommandId.SHIFT -> KeyEvent.KEYCODE_SHIFT_LEFT
            }
        sendEscIfNeeded(action.modifiers, ic)
        sendKeyEvent(ic, keyCode, metaStateFor(action.modifiers))
    }

    private fun arrowDirectionFor(id: CommandId): CursorDirection? =
        when (id) {
            CommandId.ARROW_LEFT -> CursorDirection.LEFT
            CommandId.ARROW_RIGHT -> CursorDirection.RIGHT
            CommandId.ARROW_UP -> CursorDirection.UP
            CommandId.ARROW_DOWN -> CursorDirection.DOWN
            else -> null
        }

    /**
     * Moves the cursor (or extends the selection) left/right, preferring `setSelection` over a
     * raw arrow [KeyEvent] whenever the editor has a real selection concept to move within.
     * This matters beyond style: sending an arrow KeyEvent the field can't consume (cursor
     * already at the start/end of the text) is unhandled input, and Android's default View
     * focus-navigation treats an unhandled DPAD key as "move focus to the next view" - which
     * pulls focus off the text field entirely and dismisses the keyboard. `setSelection` is
     * clamped to the text's actual bounds here, so it can never produce that unhandled edge
     * case in the first place. Only a RAW editor (e.g. Termux, no InputConnection selection
     * semantics at all) falls back to the raw KeyEvent this always used to send - the exact
     * primitive the pre-rewrite app's cursor-slide was deliberately rewritten to use for
     * Termux compatibility, just no longer applied unconditionally to every editor.
     */
    private fun moveCursor(
        direction: CursorDirection,
        resetAnchor: Boolean,
        extend: Boolean,
        capabilities: EditorCapabilities,
        ic: InputConnection,
    ) {
        val delta =
            when (direction) {
                CursorDirection.LEFT -> -1

                CursorDirection.RIGHT -> 1

                // Slide only ever produces LEFT/RIGHT in this layout; UP/DOWN have no simple
                // selection-relative equivalent without knowing line-wrap layout, so they always
                // fall through to the KeyEvent path below.
                CursorDirection.UP, CursorDirection.DOWN -> null
            }
        val handled =
            delta != null &&
                capabilities.level != EditorCapabilityLevel.RAW &&
                stepSelection(ic, resetAnchor, delta, extend)
        if (!handled) sendArrow(direction, extend, ic)
    }

    private data class SelectionExtent(
        val anchor: Int,
        val cursor: Int,
        val lowerBound: Int,
        val upperBound: Int,
    )

    /** Returns false (caller falls back to a KeyEvent) if the editor didn't expose extracted text. */
    private fun stepSelection(
        ic: InputConnection,
        resetAnchor: Boolean,
        delta: Int,
        extend: Boolean,
    ): Boolean {
        val base = (if (resetAnchor) null else cachedExtent) ?: queryExtent(ic) ?: return false
        val newCursor = (base.cursor + delta).coerceIn(base.lowerBound, base.upperBound)
        val newAnchor = if (extend) base.anchor else newCursor
        ic.setSelection(newAnchor, newCursor)
        cachedExtent = base.copy(anchor = newAnchor, cursor = newCursor)
        return true
    }

    private fun queryExtent(ic: InputConnection): SelectionExtent? {
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0) ?: return null
        val text = extracted.text ?: return null
        val length = text.length
        val base = extracted.startOffset
        return SelectionExtent(
            anchor = base + extracted.selectionStart.coerceIn(0, length),
            cursor = base + extracted.selectionEnd.coerceIn(0, length),
            lowerBound = base,
            upperBound = base + length,
        )
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
        if (ModifierId.SHIFT in modifiers) meta = meta or KeyEvent.META_SHIFT_ON
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
