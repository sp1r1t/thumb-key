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
 *
 * [columnSpan] is how many grid columns this key occupies in its row (Enter is 2 on Suave so
 * the 4-key bottom row still fills the same width as the 5-key letter rows). The renderer
 * weights keys by this value; it is layout data, not a special-case in the screen.
 */
data class KeyMapping(
    val gestureConfig: GestureConfig,
    val intents: Map<Zone, KeyIntent>,
    val slideBehavior: SlideBehavior? = null,
    val columnSpan: Int = 1,
) {
    init {
        require(columnSpan >= 1) { "columnSpan must be at least 1, got $columnSpan" }
    }
}

/**
 * A layout is pure data: position -> gesture shape + zone -> intent. No mode branching, no
 * imperative logic - a layout file should be a flat table of these, nothing else. Nothing
 * about which [KeyIntent] a zone holds is privileged: text, commands, and modifiers are all
 * just entries in [KeyMapping.intents].
 */
typealias Layout = Map<KeyPosition, KeyMapping>

/** Groups [Layout] keys into rows (sorted), each row's keys sorted by column. Irregular
 *  grids (a bottom row with fewer keys, a missing cell) fall out of the data rather than a
 *  hardcoded row/column range in the renderer. */
fun layoutRows(layout: Layout): List<List<KeyPosition>> =
    layout.keys
        .groupBy { it.row }
        .toSortedMap()
        .map { (_, positions) -> positions.sortedBy { it.col } }
