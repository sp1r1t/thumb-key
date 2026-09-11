package com.suave.s12.engine.modifier

import com.suave.s12.engine.gesture.Gesture
import com.suave.s12.engine.intent.KeyIntent
import com.suave.s12.engine.intent.ModifierId

/**
 * Pure `(ModifierState, input) -> ModifierState'` / `-> ResolvedIntent` transforms. No touch
 * handling, no layout lookup, no Android APIs - the caller (the UI wiring in Step 5) is
 * responsible for routing each recognized [Gesture] to the right function here based on what
 * [KeyIntent] the layout has at that position:
 *
 * - A key whose intent is [KeyIntent.ModifierPress] routes its gestures through
 *   [applyModifierGesture].
 * - Any other key's intent routes through [resolve] to get the modifier-transformed result.
 *
 * Calling contract for one-shot consumption: call [consumeOneShots] exactly once per *physical
 * press* of a non-modifier key - on the first gesture resolved for that press (a [Gesture.Tap],
 * or the first [Gesture.Hold]) - not on every [Gesture.HoldRepeat] tick of the same press. A
 * one-shot modifier (e.g. a quick Ctrl tap) is meant to cover one physical key press, including
 * however many characters that press repeats while held, not just its first repeat tick.
 */
object ModifierEngine {
    fun resolve(
        state: ModifierState,
        intent: KeyIntent,
        shiftMappings: Map<String, String> = emptyMap(),
    ): ResolvedIntent =
        when (intent) {
            is KeyIntent.Text -> {
                // Ctrl/Alt/Esc-combo and raw-editor KeyEvent treatment only make sense for a
                // single character - there's no KeyEvent for "Ctrl+sch". Multi-character text
                // (Suave's "sch"/"ch" keys) always ignores those modifiers and just commits as
                // text, matching the layout's own pre-rewrite behavior. The check uses the
                // *original* text, before Shift's transform below - Shift can itself change
                // length (e.g. German "ß" -> "SS").
                val modifiersApply = intent.text.length == 1
                val text =
                    if (state.isActive(ModifierId.SHIFT)) {
                        shiftMappings[intent.text] ?: if (intent.text.length == 1) intent.text.uppercase() else intent.text
                    } else {
                        intent.text
                    }
                val modifiers = if (modifiersApply) state.active.keys - ModifierId.SHIFT else emptySet()
                ResolvedIntent.TypedText(text, modifiers)
            }

            is KeyIntent.Command -> {
                ResolvedIntent.TypedCommand(intent.id, state.active.keys)
            }

            // Neither routes through resolve() in practice - ModifierPress goes through
            // applyModifierGesture, LegacyAction is unwrapped directly by the UI wiring (Step 5)
            // before it would ever reach here. Both are inert if resolve() is called anyway.
            is KeyIntent.ModifierPress, is KeyIntent.LegacyAction -> {
                ResolvedIntent.Noop
            }

            KeyIntent.Noop -> {
                ResolvedIntent.Noop
            }
        }

    /** Clears every [ActivationMode.ONE_SHOT] modifier; [ActivationMode.HELD]/[ActivationMode.LOCKED] are untouched. */
    fun consumeOneShots(state: ModifierState): ModifierState {
        val remaining = state.active.filterValues { it.mode != ActivationMode.ONE_SHOT }
        return if (remaining.size == state.active.size) state else state.copy(active = remaining)
    }

    fun applyModifierGesture(
        state: ModifierState,
        modifier: ModifierId,
        gesture: Gesture,
        behaviors: Map<ModifierId, ModifierBehavior> = DEFAULT_MODIFIER_BEHAVIORS,
    ): ModifierState {
        val behavior = behaviors[modifier] ?: ModifierBehavior(ActivationMode.HELD)
        val currentlyActive = state.active[modifier]

        return when (gesture) {
            is Gesture.Tap -> {
                // A tap while already active is an explicit toggle-off; otherwise a plain tap
                // always activates ONE_SHOT - only a genuine hold (below) differs per modifier.
                if (currentlyActive != null) state.deactivate(modifier) else state.activate(modifier, ActivationMode.ONE_SHOT)
            }

            is Gesture.Hold -> {
                state.activate(modifier, behavior.holdActivates)
            }

            // Already active from Hold; repeated ticks are a no-op - this is what stops a held
            // modifier from spamming its own toggle (the bug that hit Shift's capslock-hold
            // once repeat-on-hold applied to every key without excluding modifiers).
            is Gesture.HoldRepeat -> {
                state
            }

            Gesture.Released -> {
                if (currentlyActive?.mode == ActivationMode.HELD) {
                    // Grace period: the very next key still gets it once, then auto-reverts -
                    // this is the explicit transition the old engine never had, where releasing
                    // Ctrl only updated a tracking flag that nothing ever consulted to turn the
                    // rendered mode back off.
                    state.activate(modifier, ActivationMode.ONE_SHOT)
                } else {
                    // LOCKED persists past release (caps lock); ONE_SHOT/inactive have nothing
                    // to do here - ONE_SHOT clears via consumeOneShots on the next typed key.
                    state
                }
            }

            Gesture.Pressed, Gesture.Cancelled, is Gesture.SlideStep, is Gesture.SwipeLocked -> {
                state
            }
        }
    }
}
