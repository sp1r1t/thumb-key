package com.suave.keyboard.engine.gesture

import kotlin.math.atan2

/**
 * Occupied swipe directions as a bit mask over [Direction.ordinal]. Empty (0) means tap-only:
 * movement past the swipe threshold never locks a zone (see docs/swipe-zone-inference.md).
 */
typealias SwipeMask = Int

fun Direction.toSwipeBit(): Int = 1 shl ordinal

fun SwipeMask.hasDirection(direction: Direction): Boolean = (this and direction.toSwipeBit()) != 0

fun swipeMask(vararg directions: Direction): SwipeMask =
    directions.fold(0) { acc, direction -> acc or direction.toSwipeBit() }

val CARDINAL_SWIPE_MASK: SwipeMask =
    swipeMask(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)

val ALL_SWIPE_MASK: SwipeMask = (1 shl Direction.entries.size) - 1

/** Bit mask of directional zones present in [intents]. Center is ignored. */
fun occupiedSwipeMask(intents: Map<Zone, *>): SwipeMask =
    intents.keys
        .filterIsInstance<Zone.Directional>()
        .fold(0) { acc, zone -> acc or zone.direction.toSwipeBit() }

fun GestureConfig.withOccupiedDirections(intents: Map<Zone, *>): GestureConfig =
    copy(occupiedDirections = occupiedSwipeMask(intents))

/**
 * Compass directions in ascending angle order for the recognizer's atan2 frame
 * (0deg = RIGHT, 90deg = UP).
 */
private val CIRCLE_ORDER =
    arrayOf(
        Direction.RIGHT,
        Direction.UP_RIGHT,
        Direction.UP,
        Direction.UP_LEFT,
        Direction.LEFT,
        Direction.DOWN_LEFT,
        Direction.DOWN,
        Direction.DOWN_RIGHT,
    )

/**
 * Resolve a movement vector to an occupied swipe direction, or null when the angle is
 * unclaimed (missing zone whose neighbour share was not absorbed). See
 * docs/swipe-zone-inference.md: 8 x 45deg base, missing zones split one hop 50:50.
 */
fun resolveSwipeDirection(
    dx: Float,
    dy: Float,
    occupied: SwipeMask,
): Direction? {
    if (occupied == 0) return null

    val angleDeg = Math.toDegrees(atan2(-dy.toDouble(), dx.toDouble()))
    val normalized = (angleDeg + 360.0) % 360.0

    // Same 8-way bins as a fully occupied key (45deg each, RIGHT straddling 0).
    val baseIndex =
        when {
            normalized < 22.5 || normalized >= 337.5 -> 0 // RIGHT
            normalized < 67.5 -> 1
            normalized < 112.5 -> 2
            normalized < 157.5 -> 3
            normalized < 202.5 -> 4
            normalized < 247.5 -> 5
            normalized < 292.5 -> 6
            else -> 7
        }
    val base = CIRCLE_ORDER[baseIndex]
    if (occupied.hasDirection(base)) return base

    val centerAngle = baseIndex * 45.0
    var relative = normalized - centerAngle
    if (relative > 180.0) relative -= 360.0
    if (relative < -180.0) relative += 360.0

    val neighborIndex =
        if (relative < 0.0) {
            (baseIndex + 7) % 8
        } else {
            (baseIndex + 1) % 8
        }
    val neighbor = CIRCLE_ORDER[neighborIndex]
    return neighbor.takeIf { occupied.hasDirection(it) }
}
