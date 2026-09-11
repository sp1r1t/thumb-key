package com.suave.s12.engine.modifier

import com.suave.s12.engine.intent.CommandId
import com.suave.s12.engine.intent.ModifierId

/**
 * A [com.suave.s12.engine.intent.KeyIntent] after modifier transformation - still capability-
 * agnostic (it doesn't know how Ctrl or Esc get encoded as Android primitives, that's the action
 * compiler's job). [modifiers] never contains [ModifierId.SHIFT]: Shift has already been baked
 * into [TypedCharacter.char] by the time a character reaches here, since it changes which
 * character was typed rather than how it's sent. For [TypedCommand], Shift stays in [modifiers]
 * (there's no "shifted backspace" character to bake it into).
 */
sealed class ResolvedIntent {
    data class TypedCharacter(
        val char: Char,
        val modifiers: Set<ModifierId> = emptySet(),
    ) : ResolvedIntent()

    data class TypedCommand(
        val id: CommandId,
        val modifiers: Set<ModifierId> = emptySet(),
    ) : ResolvedIntent()

    object Noop : ResolvedIntent()
}
