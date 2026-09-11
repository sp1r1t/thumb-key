package com.suave.s12.engine.intent

/**
 * What a position+gesture on the layout means, before modifier transformation. A layout is pure
 * data mapping position and gesture to one of these - it never encodes what Ctrl+X or Alt+X
 * should produce, that's the modifier engine's job (see `engine/modifier`).
 */
sealed class KeyIntent {
    data class Character(
        val char: Char,
    ) : KeyIntent()

    data class Command(
        val id: CommandId,
    ) : KeyIntent()

    data class ModifierPress(
        val modifier: ModifierId,
    ) : KeyIntent()

    object Noop : KeyIntent()
}
