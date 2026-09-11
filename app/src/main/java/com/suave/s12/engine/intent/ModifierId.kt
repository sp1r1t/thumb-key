package com.suave.s12.engine.intent

/**
 * The complete set of modifiers this engine knows about. Lives in `intent` rather than
 * `modifier` because a [KeyIntent.ModifierPress] needs to name one without depending on the
 * modifier package's activation-state machinery - the modifier engine depends on this
 * vocabulary, not the other way around.
 *
 * [standaloneCommand] is what this modifier sends when it is *not* acting as a modifier
 * (see [com.suave.s12.engine.modifier.ModifierBehavior.actsAsModifier]) - the same Command any
 * other key would use, so "Esc as a plain Escape key" is not an Esc-specific code path.
 */
enum class ModifierId {
    CTRL,
    ALT,
    SHIFT,
    ESC,
    ;

    val standaloneCommand: CommandId
        get() =
            when (this) {
                CTRL -> CommandId.CTRL
                ALT -> CommandId.ALT
                SHIFT -> CommandId.SHIFT
                ESC -> CommandId.ESCAPE
            }
}
