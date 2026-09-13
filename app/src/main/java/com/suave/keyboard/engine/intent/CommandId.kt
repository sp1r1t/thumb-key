package com.suave.keyboard.engine.intent

/**
 * Every non-character action a layout can place on a cell or swipe zone. Key-event commands
 * (Enter, arrows) and app/editor commands (Copy, Settings) are the same kind of thing: a
 * [KeyIntent.Command] the dispatcher treats uniformly. Hold-repeat is this id's decision, not a
 * privilege of "normal keys".
 */
enum class CommandId {
    ENTER,
    TAB,
    BACKSPACE,
    DELETE_FORWARD,
    SPACE,
    ARROW_LEFT,
    ARROW_RIGHT,
    ARROW_UP,
    ARROW_DOWN,
    ESCAPE,
    CTRL,
    ALT,
    SHIFT,
    COPY,
    CUT,
    PASTE,
    SELECT_ALL,
    UNDO,
    REDO,
    GOTO_SETTINGS,
    TOGGLE_HIDE_LETTERS,
    SWITCH_IME,
    SWITCH_IME_VOICE,
    SWITCH_LANGUAGE,
    MOVE_KEYBOARD,
    TOGGLE_EMOJI_MODE,
    TOGGLE_NUMERIC_MODE,
    TOGGLE_ABC_MODE,
    TOGGLE_CLIPBOARD_HISTORY,
    /** Perform the editor's current IME action (Search / Done / Go / Send / ...). */
    IME_ACTION,
    /** Request the system hide this IME. */
    HIDE_KEYBOARD,
    /** Super/Meta/Win modifier (also sendable as a standalone key). */
    META,
    /**
     * Toggle landscape floating for the current host app (layout default, overridden per
     * package and remembered on the layout).
     */
    TOGGLE_LANDSCAPE_FLOATING,
    ;

    /**
     * Whether [com.suave.keyboard.engine.gesture.Gesture.HoldRepeat] should re-fire this command.
     * Typing keys say yes; Copy/settings/mode switches say no. Undo/redo repeat because holding
     * them to walk history is useful in the same way holding Backspace is.
     */
    fun repeatsOnHold(): Boolean =
        when (this) {
            ENTER,
            TAB,
            BACKSPACE,
            DELETE_FORWARD,
            SPACE,
            ARROW_LEFT,
            ARROW_RIGHT,
            ARROW_UP,
            ARROW_DOWN,
            ESCAPE,
            CTRL,
            ALT,
            SHIFT,
            UNDO,
            REDO,
            -> true

            COPY,
            CUT,
            PASTE,
            SELECT_ALL,
            GOTO_SETTINGS,
            TOGGLE_HIDE_LETTERS,
            SWITCH_IME,
            SWITCH_IME_VOICE,
            SWITCH_LANGUAGE,
            MOVE_KEYBOARD,
            TOGGLE_EMOJI_MODE,
            TOGGLE_NUMERIC_MODE,
            TOGGLE_ABC_MODE,
            TOGGLE_CLIPBOARD_HISTORY,
            IME_ACTION,
            HIDE_KEYBOARD,
            META,
            TOGGLE_LANDSCAPE_FLOATING,
            -> false
        }

    /** Commands that [com.suave.keyboard.engine.output.OutputExecutor] can send as Android KeyEvents. */
    fun isKeyEventCommand(): Boolean =
        when (this) {
            ENTER,
            TAB,
            BACKSPACE,
            DELETE_FORWARD,
            SPACE,
            ARROW_LEFT,
            ARROW_RIGHT,
            ARROW_UP,
            ARROW_DOWN,
            ESCAPE,
            CTRL,
            ALT,
            SHIFT,
            META,
            -> true

            else -> false
        }

    /**
     * Builtin layer navigation that [KeyIntent.SwitchLayer] also covers. The layout editor
     * offers those under Switch layer only; these ids remain so existing layouts keep working.
     */
    fun isLayerSwitchCommand(): Boolean = switchLayerIdOrNull() != null

    /** Layer id when this command is a builtin layer switch; otherwise null. */
    fun switchLayerIdOrNull(): String? =
        when (this) {
            TOGGLE_ABC_MODE -> "main"
            TOGGLE_NUMERIC_MODE -> "numeric"
            TOGGLE_EMOJI_MODE -> "emoji"
            TOGGLE_CLIPBOARD_HISTORY -> "clipboard"
            else -> null
        }

    /**
     * Modifier keys that [KeyIntent.ModifierPress] covers. The layout editor offers those under
     * Modifier only; these ids remain so existing layouts keep working.
     */
    fun isModifierCommand(): Boolean = modifierIdOrNull() != null

    fun modifierIdOrNull(): ModifierId? =
        when (this) {
            CTRL -> ModifierId.CTRL
            ALT -> ModifierId.ALT
            SHIFT -> ModifierId.SHIFT
            ESCAPE -> ModifierId.ESC
            META -> ModifierId.META
            else -> null
        }
}
