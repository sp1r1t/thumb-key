package com.suave.s12.ui.engine

import com.suave.s12.engine.action.SemanticAction

data class KeyAnimationSettings(
    val pressHighlight: Boolean = true,
    val releaseFlash: Boolean = true,
    val letterDrop: Boolean = true,
) {
    val playsRelease: Boolean get() = releaseFlash || letterDrop
}

/** Thumb-Key only animated committed characters, never commands or cursor moves. */
fun typedTextForReleaseAnimation(action: SemanticAction): String? =
    (action as? SemanticAction.TypeText)?.text?.takeIf { it.isNotEmpty() }
