package com.suave.s12.engine.intent

import com.suave.s12.utils.KeyAction

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

    /**
     * Escape hatch: forwards straight to the old `KeyAction`/`performKeyAction` pipeline. Exists
     * only for the app-integration keys this phase deliberately bridges rather than redesigns -
     * settings navigation, clipboard, emoji picker, numeric-layout switch, IME/language switch -
     * see the plan's "explicitly out of scope for Phase 1". Every other KeyIntent variant is
     * fully owned by the new engine; this is the one place old code is still reachable, and only
     * the UI wiring (Step 5) ever unwraps it - [com.suave.s12.engine.modifier.ModifierEngine]
     * treats it as inert, the same as [ModifierPress].
     */
    data class LegacyAction(
        val action: KeyAction,
    ) : KeyIntent()

    object Noop : KeyIntent()
}
