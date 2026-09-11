package com.suave.s12.engine.action

import com.suave.s12.engine.modifier.ResolvedIntent

/** Converts a modifier-resolved intent (the discrete lane's output) into a [SemanticAction]. */
object IntentCompiler {
    fun compile(resolved: ResolvedIntent): SemanticAction =
        when (resolved) {
            is ResolvedIntent.TypedCharacter -> SemanticAction.TypeCharacter(resolved.char, resolved.modifiers)
            is ResolvedIntent.TypedCommand -> SemanticAction.TypeCommand(resolved.id, resolved.modifiers)
            ResolvedIntent.Noop -> SemanticAction.Noop
        }
}
