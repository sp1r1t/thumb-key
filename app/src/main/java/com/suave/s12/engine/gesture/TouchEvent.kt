package com.suave.s12.engine.gesture

/** A single raw pointer sample for one physical key's touch lifetime. */
data class TouchEvent(
    val x: Float,
    val y: Float,
    val timeMs: Long,
    val phase: TouchPhase,
)

enum class TouchPhase { DOWN, MOVE, UP, CANCEL }

/**
 * Input to [GestureRecognizer.process]. Touch events carry real pointer samples; ticks let the
 * caller advance wall-clock time explicitly (driven by a coroutine timer in production, fed
 * directly with chosen millis in a test) so hold/repeat timing is deterministic and testable
 * without real delays.
 */
sealed class RecognizerInput {
    data class Touch(
        val event: TouchEvent,
    ) : RecognizerInput()

    data class Tick(
        val nowMs: Long,
    ) : RecognizerInput()
}
