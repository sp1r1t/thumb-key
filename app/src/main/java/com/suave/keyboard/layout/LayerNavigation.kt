package com.suave.keyboard.layout

/**
 * Which letter/number/custom grid an overlay (emoji, clipboard) should return to.
 * [origin] is always a base layer (MAIN, NUMERIC, or a custom function layer).
 */
data class LayerSession(
    val current: ActiveLayer = ActiveLayer.Main,
    val origin: ActiveLayer = ActiveLayer.Main,
) {
    /** Convenience for call sites that still key off the builtin enum when not on a custom. */
    val builtinOrNull: LayoutLayer?
        get() = (current as? ActiveLayer.Builtin)?.layer

    val customIdOrNull: String?
        get() = (current as? ActiveLayer.Custom)?.id
}

fun LayerSession.selectBase(requested: ActiveLayer): LayerSession {
    require(requested.isBaseOrCustom()) {
        "selectBase only takes MAIN, NUMERIC, or custom layers, got $requested"
    }
    return LayerSession(current = requested, origin = requested)
}

fun LayerSession.selectBuiltinBase(requested: LayoutLayer): LayerSession {
    require(requested == LayoutLayer.MAIN || requested == LayoutLayer.NUMERIC) {
        "selectBuiltinBase only takes MAIN or NUMERIC, got $requested"
    }
    return selectBase(ActiveLayer.Builtin(requested))
}

fun LayerSession.enterOverlay(overlay: LayoutLayer): LayerSession {
    require(overlay == LayoutLayer.EMOJI || overlay == LayoutLayer.CLIPBOARD) {
        "enterOverlay only takes EMOJI or CLIPBOARD, got $overlay"
    }
    val newOrigin =
        when (val c = current) {
            is ActiveLayer.Builtin ->
                when (c.layer) {
                    LayoutLayer.MAIN, LayoutLayer.NUMERIC -> c
                    LayoutLayer.EMOJI, LayoutLayer.CLIPBOARD -> origin
                }
            is ActiveLayer.Custom -> c
        }
    return copy(current = ActiveLayer.Builtin(overlay), origin = newOrigin)
}

fun LayerSession.leaveOverlay(): LayerSession = copy(current = origin)

fun LayerSession.toggleEmoji(available: Boolean): LayerSession {
    if (!available) return this
    return if (current == ActiveLayer.Emoji) leaveOverlay() else enterOverlay(LayoutLayer.EMOJI)
}

fun LayerSession.toggleClipboard(available: Boolean): LayerSession {
    if (!available) return this
    return if (current == ActiveLayer.Clipboard) {
        leaveOverlay()
    } else {
        enterOverlay(LayoutLayer.CLIPBOARD)
    }
}

/** Toggle a custom (or numeric/main) base layer: enter it, or return to MAIN if already there. */
fun LayerSession.toggleBase(target: ActiveLayer): LayerSession {
    require(target.isBaseOrCustom()) { "toggleBase requires a base layer, got $target" }
    return if (current == target) {
        selectBase(ActiveLayer.Main)
    } else {
        selectBase(target)
    }
}

private fun ActiveLayer.isBaseOrCustom(): Boolean =
    when (this) {
        is ActiveLayer.Custom -> true
        is ActiveLayer.Builtin -> layer == LayoutLayer.MAIN || layer == LayoutLayer.NUMERIC
    }

// Back-compat wrappers used by existing tests/call sites.
fun LayerSession.selectBaseLayer(requested: LayoutLayer): LayerSession = selectBuiltinBase(requested)

val LayerSession.layer: LayoutLayer
    get() = builtinOrNull ?: LayoutLayer.MAIN
