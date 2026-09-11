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
    ) : SemanticAction()

    data class ExtendSelection(
        val direction: CursorDirection,
    ) : SemanticAction()

    object Noop : SemanticAction()
}
