package com.suave.keyboard.layout

/**
 * Which letter/number/custom grid an overlay should return to.
 * [origin] is always a non-overlay layer.
 */
data class LayerSession(
    val current: ActiveLayer = ActiveLayer.Main,
    val origin: ActiveLayer = ActiveLayer.Main,
)

fun LayerSession.selectBase(requested: ActiveLayer): LayerSession =
    LayerSession(current = requested, origin = requested)

fun LayerSession.leaveOverlay(): LayerSession = copy(current = origin)

fun LayerSession.switchTo(
    target: ActiveLayer,
    layout: NamedLayout,
): LayerSession {
    val def = layout.layer(target) ?: return this
    return if (def.overlay) {
        val newOrigin = if (layout.isOverlay(current)) origin else current
        copy(current = target, origin = newOrigin)
    } else {
        selectBase(target)
    }
}

fun LayerSession.toggleOverlay(
    target: ActiveLayer,
    available: Boolean,
    layout: NamedLayout,
): LayerSession {
    if (!available) return this
    return if (current == target) leaveOverlay() else switchTo(target, layout)
}

fun LayerSession.toggleEmoji(
    available: Boolean,
    layout: NamedLayout,
): LayerSession = toggleOverlay(ActiveLayer.Emoji, available, layout)

fun LayerSession.toggleClipboard(
    available: Boolean,
    layout: NamedLayout,
): LayerSession = toggleOverlay(ActiveLayer.Clipboard, available, layout)

/** Toggle a base layer: enter it, or return to [home] if already there. */
fun LayerSession.toggleBase(
    target: ActiveLayer,
    home: ActiveLayer = ActiveLayer.Main,
): LayerSession =
    if (current == target) {
        selectBase(home)
    } else {
        selectBase(target)
    }

/**
 * Starting layer for a newly focused editor. Number/phone/datetime open numeric when that
 * layer exists; otherwise home.
 */
fun layerSessionForEditor(
    layout: NamedLayout,
    prefersNumeric: Boolean,
): LayerSession {
    val numeric = ActiveLayer.Numeric
    val base =
        if (prefersNumeric && layout.layer(numeric) != null) {
            numeric
        } else {
            layout.homeActive()
        }
    return LayerSession().selectBase(base)
}
