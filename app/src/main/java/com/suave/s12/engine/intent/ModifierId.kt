package com.suave.s12.engine.intent

/**
 * The complete set of modifiers this engine knows about. Lives in `intent` rather than
 * `modifier` because a [KeyIntent.ModifierPress] needs to name one without depending on the
 * modifier package's activation-state machinery - the modifier engine depends on this
 * vocabulary, not the other way around.
 */
enum class ModifierId { CTRL, ALT, SHIFT, ESC }
