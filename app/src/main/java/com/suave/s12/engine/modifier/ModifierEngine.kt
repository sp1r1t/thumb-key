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

    /**
     * [freshlyActivatedByPressed] disambiguates [Gesture.Tap]'s two possible meanings once
     * [Gesture.Pressed] can activate a modifier provisionally before its own hold threshold
     * ever fires (see that branch below): "true" means *this exact press* is what turned the
     * modifier on, and a tap (no hold reached) should reclassify that into the old sticky
     * one-shot behavior; "false" (the default) means whatever is currently active got there
     * some other way (already active before this press even started), so a tap is an explicit
     * toggle-off. The caller (`KeyDispatcher`) is the one place that can know which is true,
     * since it alone sees the whole gesture sequence for one physical key.
     */
    fun applyModifierGesture(
        state: ModifierState,
        modifier: ModifierId,
        gesture: Gesture,
        behaviors: Map<ModifierId, ModifierBehavior> = DEFAULT_MODIFIER_BEHAVIORS,
        freshlyActivatedByPressed: Boolean = false,
    ): ModifierState {
        val behavior = behaviors[modifier] ?: ModifierBehavior(ActivationMode.HELD)
        val currentlyActive = state.active[modifier]

        return when (gesture) {
            // Activates immediately and provisionally, so a key pressed with the other hand
            // while this is still physically down - even within the long-press threshold
            // window - already sees it as active, instead of waiting for a confirmed Hold.
            // This is the fix for "hold Ctrl then immediately press a" typing as plain 'a':
            // Hold used to be the only thing that ever activated a modifier, and it doesn't
            // fire until the threshold elapses. If this press never reaches that threshold,
            // the Tap branch below reclassifies this into the old sticky one-shot behavior.
            Gesture.Pressed -> {
                if (currentlyActive == null) state.activate(modifier, ActivationMode.HELD) else state
            }

            is Gesture.Tap -> {
                when {
                    currentlyActive == null -> state.activate(modifier, ActivationMode.ONE_SHOT)
                    freshlyActivatedByPressed -> state.activate(modifier, ActivationMode.ONE_SHOT)
                    else -> state.deactivate(modifier)
                }
            }

            is Gesture.Hold -> {
                // Reclassifies per this modifier's hold behavior - a no-op for Ctrl/Alt/Esc
                // (holdActivates is already HELD, and Pressed already put them there), but
                // this is still what turns Shift's HELD into LOCKED (caps lock).
                state.activate(modifier, behavior.holdActivates)
            }

            // Already active from Hold (or Pressed); repeated ticks are a no-op - this is what
            // stops a held modifier from spamming its own toggle (the bug that hit Shift's
            // capslock-hold once repeat-on-hold applied to every key without excluding
            // modifiers).
            is Gesture.HoldRepeat -> {
                state
            }

            Gesture.Released -> {
                if (currentlyActive?.mode == ActivationMode.HELD) {
                    // Releasing a held modifier deactivates it immediately - the hold+release
                    // itself is the explicit, deliberate signal. A press that never reached a
                    // confirmed Hold never sees this branch anyway: Tap already reclassified it
                    // to ONE_SHOT moments earlier in the same gesture batch, so by the time
                    // Released runs here, mode is ONE_SHOT, not HELD.
                    state.deactivate(modifier)
                } else {
                    // LOCKED persists past release (caps lock); ONE_SHOT/inactive have nothing
                    // to do here - ONE_SHOT clears via consumeOneShots on the next typed key.
                    state
                }
            }

            Gesture.Cancelled, is Gesture.SlideStep, is Gesture.SwipeLocked -> {
                state
            }
        }
    }
}
