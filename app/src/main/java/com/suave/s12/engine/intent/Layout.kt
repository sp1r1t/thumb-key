package com.suave.s12.engine.intent

import com.suave.s12.engine.gesture.GestureConfig
import com.suave.s12.engine.gesture.Zone

/** A physical key's position in the keyboard grid. Stable across every modifier/mode. */
data class KeyPosition(
    val row: Int,
    val col: Int,
)

/**
 * What sliding a key means - the continuous lane bypasses [KeyIntent] entirely (see
 * [SemanticAction] for why), so this is the only place a layout says what a given key's slide
 * does. Not every key with a [com.suave.s12.engine.gesture.SlideAxis] configured needs both
 * cases handled identically: spacebar moves the cursor, backspace extends a selection that gets
 * deleted on release.
 */
enum class SlideBehavior { MOVE_CURSOR, SELECT_AND_DELETE }

/**
 * One physical key: its gesture shape, and what each zone means. [Gesture.Tap], [Gesture.Hold]
 * and [Gesture.HoldRepeat] for the same [Zone] all resolve through the same [intents] entry -
 * repeat-on-hold isn't a distinct layout concept, it's the gesture recognizer emitting the same
 * zone's intent multiple times (see [GestureRecognizer]). A zone with no entry does nothing.
 */
data class KeyMapping(
    val gestureConfig: GestureConfig,
    val intents: Map<Zone, KeyIntent>,
    val slideBehavior: SlideBehavior? = null,
)

/**
 * A layout is pure data: position -> gesture shape + zone -> intent. No mode branching, no
 * imperative logic - a layout file should be a flat table of these, nothing else.
 */
typealias Layout = Map<KeyPosition, KeyMapping>
