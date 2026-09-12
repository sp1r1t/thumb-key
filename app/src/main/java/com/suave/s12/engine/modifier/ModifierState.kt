package com.suave.s12.engine.modifier

import com.suave.s12.engine.intent.ModifierId

/**
 * How a modifier became active. The SAME three primitives apply uniformly to every modifier -
 * there is no separate Ctrl-hold-loop vs. Shift-capslock-longpress code path, only which
 * [ActivationMode] a given modifier's hold gesture targets (see [ModifierBehavior]).
 */
enum class ActivationMode {
    /** Stays active only while the physical key is held; see [ModifierEngine] for the release transition. */
    HELD,

    /** Applies to exactly the next non-modifier key resolved, then auto-reverts. */
    ONE_SHOT,

    /** Stays active until explicitly toggled off (e.g. caps lock), independent of hold state. */
    LOCKED,
}

data class ActiveModifier(
    val mode: ActivationMode,
)

/**
 * Which modifiers are currently active and how. Lives entirely outside layout rendering - no
 * key's identity or the set of rendered keys ever depends on this state, so nothing about
 * activating a modifier can tear down another key's in-flight gesture tracking (the structural
 * fix for the bug class the old engine hit: toggling Ctrl there swapped the whole rendered
 * keyset, which changed key identity mid-gesture).
 */
data class ModifierState(
    val active: Map<ModifierId, ActiveModifier> = emptyMap(),
) {
    fun isActive(id: ModifierId): Boolean = active.containsKey(id)

    fun activate(
        id: ModifierId,
        mode: ActivationMode,
    ): ModifierState = copy(active = active + (id to ActiveModifier(mode)))

    fun deactivate(id: ModifierId): ModifierState = copy(active = active - id)

    /**
     * Display-only view for letter-key legends: collapses HELD and ONE_SHOT to the same
     * instance so letter keys do not recompose across that transition, but keeps LOCKED
     * (caps lock) distinct so digraph legends can show "SCH" instead of "Sch".
     * Never pass this into dispatch - ONE_SHOT here is not consumed after a typed letter.
     */
    fun forLetterLegends(): ModifierState =
        when (active[ModifierId.SHIFT]?.mode) {
            null -> NONE
            ActivationMode.LOCKED -> CAPS_ON_FOR_LEGENDS
            ActivationMode.HELD, ActivationMode.ONE_SHOT -> SHIFT_ON_FOR_LEGENDS
        }

    companion object {
        val NONE = ModifierState()
        val SHIFT_ON_FOR_LEGENDS =
            ModifierState(mapOf(ModifierId.SHIFT to ActiveModifier(ActivationMode.ONE_SHOT)))
        val CAPS_ON_FOR_LEGENDS =
            ModifierState(mapOf(ModifierId.SHIFT to ActiveModifier(ActivationMode.LOCKED)))
    }
}
