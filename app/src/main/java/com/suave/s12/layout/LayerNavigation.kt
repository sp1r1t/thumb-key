package com.suave.s12.layout

/**
 * Which letter/number grid an overlay (emoji, clipboard) should return to. [origin] is always
 * [LayoutLayer.MAIN] or [LayoutLayer.NUMERIC]; overlays remember it instead of always dropping
 * back to letters.
 */
data class LayerSession(
    val layer: LayoutLayer = LayoutLayer.MAIN,
    val origin: LayoutLayer = LayoutLayer.MAIN,
)

fun LayerSession.selectBaseLayer(requested: LayoutLayer): LayerSession {
    require(requested == LayoutLayer.MAIN || requested == LayoutLayer.NUMERIC) {
        "selectBaseLayer only takes MAIN or NUMERIC, got $requested"
    }
    return LayerSession(layer = requested, origin = requested)
}

fun LayerSession.enterOverlay(overlay: LayoutLayer): LayerSession {
    require(overlay == LayoutLayer.EMOJI || overlay == LayoutLayer.CLIPBOARD) {
        "enterOverlay only takes EMOJI or CLIPBOARD, got $overlay"
    }
    val newOrigin =
        when (layer) {
            LayoutLayer.MAIN, LayoutLayer.NUMERIC -> layer
            LayoutLayer.EMOJI, LayoutLayer.CLIPBOARD -> origin
        }
    return copy(layer = overlay, origin = newOrigin)
}

fun LayerSession.leaveOverlay(): LayerSession = copy(layer = origin)

fun LayerSession.toggleEmoji(available: Boolean): LayerSession {
    if (!available) return this
    return if (layer == LayoutLayer.EMOJI) leaveOverlay() else enterOverlay(LayoutLayer.EMOJI)
}

fun LayerSession.toggleClipboard(available: Boolean): LayerSession {
    if (!available) return this
    return if (layer == LayoutLayer.CLIPBOARD) leaveOverlay() else enterOverlay(LayoutLayer.CLIPBOARD)
}
