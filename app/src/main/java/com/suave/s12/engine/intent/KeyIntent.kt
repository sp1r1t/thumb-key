package com.suave.s12.engine.intent

/**
 * What a position+gesture on the layout means, before modifier transformation. A layout is pure
 * data mapping position and gesture to one of these - it never encodes what Ctrl+X or Alt+X
 * should produce, that's the modifier engine's job (see `engine/modifier`).
 */
sealed class KeyIntent {
    /** Arbitrary typed content - usually one character, but Suave has multi-character keys too
     *  (e.g. "sch"/"ch"). Only single-character [Text] gets Ctrl/Alt/Esc-combo or raw-editor
     *  KeyEvent treatment; see [com.suave.s12.engine.modifier.ModifierEngine]. */
    data class Text(
        val text: String,
    ) : KeyIntent()

    data class Command(
        val id: CommandId,
    ) : KeyIntent()

    data class ModifierPress(
        val modifier: ModifierId,
    ) : KeyIntent()

    object Noop : KeyIntent()

    /**
     * Whether [com.suave.s12.engine.gesture.Gesture.HoldRepeat] should re-fire this intent.
     * The dispatcher asks the intent; it does not special-case "copy vs enter vs letter".
     * Copy can refuse; Backspace and a character say yes.
     */
    fun repeatsOnHold(): Boolean =
        when (this) {
            is Text -> true
            is Command -> id.repeatsOnHold()
            is ModifierPress, Noop -> false
        }
}
