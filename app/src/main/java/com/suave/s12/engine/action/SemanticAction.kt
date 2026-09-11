package com.suave.s12.engine.action

import com.suave.s12.engine.intent.CommandId
import com.suave.s12.engine.intent.ModifierId

enum class CursorDirection { LEFT, RIGHT, UP, DOWN }

/**
 * What to do, independent of how the current editor can receive it - that degradation decision
 * belongs to `engine/output` (see `EditorCapabilities`), not here. This is where the two
 * pipeline lanes converge: [TypeText]/[TypeCommand] come from the discrete
 * gesture -> intent -> modifier lane (via [com.suave.s12.engine.modifier.ResolvedIntent]),
 * while [MoveCursor]/[ExtendSelection] come directly from the continuous slide lane
 * ([com.suave.s12.engine.gesture.Gesture.SlideStep] bypasses intent/modifier entirely - sliding
 * a key isn't affected by which modifiers happen to be active).
 *
 * [MoveCursor]/[ExtendSelection]'s `resetAnchor` tells `engine/output` whether to trust its own
 * cached notion of the current cursor/selection position (false - the common case for repeated
 * calls within one continuous slide) or re-derive it from the editor first (true - the first step
 * of a new gesture, or a single discrete tap like Shift+Arrow). This exists because
 * `InputConnection.getExtractedText()` right after this same class's own `setSelection()` isn't
 * guaranteed to reflect it yet - re-querying on every step of a fast slide (which can emit several
 * steps within one synchronous gesture batch) intermittently read back stale, pre-gesture
 * positions, which looked like "selects one character, then resets" on every subsequent step.
 */
sealed class SemanticAction {
    data class TypeText(
        val text: String,
        val modifiers: Set<ModifierId> = emptySet(),
    ) : SemanticAction()

    data class TypeCommand(
        val id: CommandId,
        val modifiers: Set<ModifierId> = emptySet(),
    ) : SemanticAction()

    data class MoveCursor(
        val direction: CursorDirection,
        val resetAnchor: Boolean = false,
    ) : SemanticAction()

    data class ExtendSelection(
        val direction: CursorDirection,
        val resetAnchor: Boolean = false,
    ) : SemanticAction()

    object Noop : SemanticAction()
}
