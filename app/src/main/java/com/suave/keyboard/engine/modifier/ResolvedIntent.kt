package com.suave.keyboard.engine.modifier

import com.suave.keyboard.engine.intent.CommandId
import com.suave.keyboard.engine.intent.ModifierId

/**
 * A [com.suave.keyboard.engine.intent.KeyIntent] after modifier transformation - still capability-
 * agnostic (it doesn't know how Ctrl or Esc get encoded as Android primitives, that's the action
 * compiler's job). [modifiers] never contains [ModifierId.SHIFT]: Shift has already been baked
 * into [TypedText.text] by the time text reaches here, since it changes what was typed rather
 * than how it's sent. For [TypedCommand], Shift stays in [modifiers] (there's no "shifted
 * backspace" text to bake it into).
 */
sealed class ResolvedIntent {
    data class TypedText(
        val text: String,
        val modifiers: Set<ModifierId> = emptySet(),
    ) : ResolvedIntent()

    data class TypedCommand(
        val id: CommandId,
        val modifiers: Set<ModifierId> = emptySet(),
    ) : ResolvedIntent()

    /** Switch to a named builtin or custom function layer (see [com.suave.keyboard.engine.intent.KeyIntent.SwitchLayer]). */
    data class SwitchLayer(
        val layerId: String,
    ) : ResolvedIntent()

    object Noop : ResolvedIntent()
}
