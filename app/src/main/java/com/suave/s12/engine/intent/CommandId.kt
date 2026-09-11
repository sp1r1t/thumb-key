package com.suave.s12.engine.intent

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
    ;

    /**
     * Whether [com.suave.s12.engine.gesture.Gesture.HoldRepeat] should re-fire this command.
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
            -> false
        }

    /** Commands that [com.suave.s12.engine.output.OutputExecutor] can send as Android KeyEvents. */
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
            -> true

            else -> false
        }
}
