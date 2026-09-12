package com.suave.keyboard.engine.action

import com.suave.keyboard.engine.intent.CommandId
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Successive spacebar taps within 1s cycle punctuation the same way stock ThumbKey did:
 * space -> ", " -> ". " -> "? " -> "! " -> ": " -> "; " (then wrap).
 *
 * Continuity is time + "last outcome was a space multitap step" only. We deliberately do not
 * consult [com.suave.keyboard.IMEService.didCursorMove]: after commitText many editors (Samsung
 * especially) leave that flag sticky-true even when ignore-next was set, which made every
 * second tap restart at a plain space. Other keys / slides / holds clear the cycle via
 * [reset] / [noteOtherAction].
 */
class SpacebarMultitapTracker {
    private var tapIndex: Int = 0
    private var lastSpaceTap: TimeMark? = null

    fun onSpaceTap(enabled: Boolean): SemanticAction {
        if (!enabled) {
            reset()
            return SemanticAction.TypeText(" ")
        }
        val continueCycle = lastSpaceTap?.let { mark -> mark.elapsedNow() < 1.seconds } == true
        tapIndex = if (continueCycle) tapIndex + 1 else 0
        lastSpaceTap = TimeSource.Monotonic.markNow()
        val step = CYCLE[tapIndex % CYCLE.size]
        // First step matches the layout's center intent (CommitText " "), not KEYCODE_SPACE.
        return if (step == null) {
            SemanticAction.TypeText(" ")
        } else {
            SemanticAction.ReplaceLastText(step.text, step.trimCount)
        }
    }

    /** Call when a non-tap space action (hold/repeat) or any other key action runs. */
    fun reset() {
        tapIndex = 0
        lastSpaceTap = null
    }

    /**
     * If [action] is not a space-tap outcome from this tracker, clear the cycle so the next
     * space starts fresh.
     */
    fun noteOtherAction(action: SemanticAction) {
        val isSpaceOutcome = action.isPlainSpaceTap() || action is SemanticAction.ReplaceLastText
        if (!isSpaceOutcome) reset()
    }

    private data class Step(
        val text: String,
        val trimCount: Int,
    )

    companion object {
        /** Index 0 is a plain space (null); the rest match stock SPACEBAR_NEXT_TAP_ACTIONS. */
        private val CYCLE: List<Step?> =
            listOf(
                null,
                Step(", ", trimCount = 1),
                Step(". ", trimCount = 2),
                Step("? ", trimCount = 2),
                Step("! ", trimCount = 2),
                Step(": ", trimCount = 2),
                Step("; ", trimCount = 2),
            )
    }
}

/**
 * True for a bare space insertion: either [TypeText] `" "` (Suave / stock layout center) or
 * [TypeCommand] [CommandId.SPACE].
 */
fun SemanticAction.isPlainSpaceTap(): Boolean =
    when (this) {
        is SemanticAction.TypeText -> text == " " && modifiers.isEmpty()
        is SemanticAction.TypeCommand -> id == CommandId.SPACE && modifiers.isEmpty()
        else -> false
    }
