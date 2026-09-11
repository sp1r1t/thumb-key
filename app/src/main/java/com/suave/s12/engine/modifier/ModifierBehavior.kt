package com.suave.s12.engine.modifier

import com.suave.s12.engine.intent.ModifierId

/**
 * Per-modifier policy. The same three fields apply to every [ModifierId] - there is no
 * Esc-only (or Shift-only) code path for "is this a modifier or a standalone key".
 *
 * - [holdActivates]: what a genuine hold (past the gesture recognizer's hold threshold)
 *   activates. A quick tap always activates [ActivationMode.ONE_SHOT] regardless of this.
 *   Ctrl/Alt/Esc hold while typing several other keys ([ActivationMode.HELD]); Shift's hold
 *   is caps lock ([ActivationMode.LOCKED]).
 * - [actsAsModifier]: when false, a [com.suave.s12.engine.intent.KeyIntent.ModifierPress] is
 *   rewritten to that modifier's [com.suave.s12.engine.intent.ModifierId.standaloneCommand] and
 *   dispatched like any other Command (repeat-on-hold included). When true, tap/hold/release
 *   go through [ModifierEngine.applyModifierGesture] as usual.
 * - [tapWhileQueuedSendsCommand]: when already ONE_SHOT from an earlier press, a new tap
 *   sends the standalone command instead of toggling the queued state off. This is the
 *   Esc+Esc hatch ("I actually just wanted Escape"), expressed as a flag any modifier can
 *   set rather than an Esc-specific branch.
 */
data class ModifierBehavior(
    val holdActivates: ActivationMode,
    val actsAsModifier: Boolean = true,
    val tapWhileQueuedSendsCommand: Boolean = false,
)

val DEFAULT_MODIFIER_BEHAVIORS: Map<ModifierId, ModifierBehavior> =
    mapOf(
        ModifierId.CTRL to ModifierBehavior(ActivationMode.HELD),
        ModifierId.ALT to ModifierBehavior(ActivationMode.HELD),
        ModifierId.ESC to ModifierBehavior(ActivationMode.HELD, tapWhileQueuedSendsCommand = true),
        ModifierId.SHIFT to ModifierBehavior(ActivationMode.LOCKED),
    )

/** Overlay per-modifier "acts as modifier" flags on [DEFAULT_MODIFIER_BEHAVIORS]. */
fun modifierBehaviors(actsAsModifier: Map<ModifierId, Boolean>): Map<ModifierId, ModifierBehavior> =
    DEFAULT_MODIFIER_BEHAVIORS.mapValues { (id, behavior) ->
        behavior.copy(actsAsModifier = actsAsModifier[id] ?: behavior.actsAsModifier)
    }
