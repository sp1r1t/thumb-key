package com.suave.s12.engine.modifier

import com.suave.s12.engine.intent.ModifierId

/**
 * Per-modifier policy for what a genuine hold (past the gesture recognizer's hold threshold)
 * activates. A quick tap always activates [ActivationMode.ONE_SHOT] regardless of this - only a
 * hold's target state differs per modifier: Ctrl/Alt/Esc hold while typing several other keys
 * ([ActivationMode.HELD]); Shift's hold is caps lock ([ActivationMode.LOCKED]).
 */
data class ModifierBehavior(
    val holdActivates: ActivationMode,
)

val DEFAULT_MODIFIER_BEHAVIORS: Map<ModifierId, ModifierBehavior> =
    mapOf(
        ModifierId.CTRL to ModifierBehavior(ActivationMode.HELD),
        ModifierId.ALT to ModifierBehavior(ActivationMode.HELD),
        ModifierId.ESC to ModifierBehavior(ActivationMode.HELD),
        ModifierId.SHIFT to ModifierBehavior(ActivationMode.LOCKED),
    )
