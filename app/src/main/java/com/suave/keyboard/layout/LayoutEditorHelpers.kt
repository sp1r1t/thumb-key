package com.suave.keyboard.layout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.suave.keyboard.engine.gesture.GestureConfig
import com.suave.keyboard.engine.gesture.SlideAxis
import com.suave.keyboard.engine.gesture.Zone
import com.suave.keyboard.engine.gesture.withOccupiedDirections
import com.suave.keyboard.engine.intent.KeyFillRole
import com.suave.keyboard.engine.intent.KeyIntent
import com.suave.keyboard.engine.intent.KeyMapping
import com.suave.keyboard.engine.intent.KeyPosition
import com.suave.keyboard.engine.intent.Layout
import com.suave.keyboard.engine.intent.SlideBehavior
import com.suave.keyboard.engine.intent.layoutRows

private val EDITOR_DEFAULT_GESTURE = GestureConfig(minSwipeDistancePx = 64f)

/** Empty key with a center noop - the minimum the codec/engine accept. */
fun blankKeyMapping(
    columnSpan: Int = 1,
    fillRole: KeyFillRole = KeyFillRole.AUTO,
    slideAxis: SlideAxis? = null,
    slideBehavior: SlideBehavior? = null,
): KeyMapping {
    val intents = mapOf<Zone, KeyIntent>(Zone.Center to KeyIntent.Noop)
    return KeyMapping(
        gestureConfig =
            EDITOR_DEFAULT_GESTURE
                .copy(slideAxis = slideAxis)
                .withOccupiedDirections(intents),
        intents = intents,
        slideBehavior = slideBehavior,
        columnSpan = columnSpan.coerceAtLeast(1),
        fillRole = fillRole,
    )
}

/** Build a rectangular-ish grid from per-row key counts. */
fun blankLayout(rowSizes: List<Int>): Layout {
    require(rowSizes.isNotEmpty()) { "Need at least one row" }
    val out = linkedMapOf<KeyPosition, KeyMapping>()
    rowSizes.forEachIndexed { row, cols ->
        require(cols >= 1) { "Row $row must have at least one key" }
        repeat(cols) { col ->
            out[KeyPosition(row, col)] = blankKeyMapping()
        }
    }
    return out
}

fun blankNamedLayout(
    id: String,
    title: String,
    rowSizes: List<Int>,
): NamedLayout =
    NamedLayout(
        id = id,
        title = title,
        layout = blankLayout(rowSizes),
    )

fun KeyMapping.withUpdatedIntents(intents: Map<Zone, KeyIntent>): KeyMapping {
    val merged = intents.toMap()
    require(Zone.Center in merged) { "Key must define a center zone" }
    return copy(
        intents = merged,
        gestureConfig =
            gestureConfig
                .copy(slideAxis = gestureConfig.slideAxis)
                .withOccupiedDirections(merged),
    )
}

fun Layout.putKey(
    position: KeyPosition,
    mapping: KeyMapping,
): Layout = this + (position to mapping)

fun Layout.removeKey(position: KeyPosition): Layout = this - position

/**
 * Remove [position] and reindex that row (and later rows if the row becomes empty).
 * Dropping the last key in a row deletes the row.
 */
fun Layout.removeKeyAndCompact(position: KeyPosition): Layout {
    val rows = layoutRows(this).map { it.toMutableList() }.toMutableList()
    val rowIndex = rows.indexOfFirst { row -> row.any { it == position } }
    if (rowIndex < 0) return this
    rows[rowIndex].removeAll { it == position }
    if (rows[rowIndex].isEmpty()) {
        rows.removeAt(rowIndex)
    }
    return reindexRows(rows)
}

/**
 * Where a dragged key lands when dropped between keys (or at a row edge).
 * [col] is the insert index in that row: 0 = before the first key, [row].size = after the last.
 */
data class KeyInsertGap(
    val row: Int,
    val col: Int,
)

/**
 * Move [from] into [gap], compacting its old row (so that slot is gone and width redistributes).
 * Crossing rows changes both row lengths. Dropping the last key of a row deletes that row.
 */
fun Layout.moveKeyToGap(
    from: KeyPosition,
    gap: KeyInsertGap,
    maxKeysPerRow: Int = Int.MAX_VALUE,
): Layout {
    val rows = layoutRows(this).map { it.toMutableList() }.toMutableList()
    val fromRow = rows.indexOfFirst { row -> row.any { it == from } }
    if (fromRow < 0) return this
    val fromCol = rows[fromRow].indexOf(from)
    if (fromCol < 0) return this

    var targetRow = gap.row
    var insertCol = gap.col
    if (targetRow !in rows.indices) return this
    insertCol = insertCol.coerceIn(0, rows[targetRow].size)

    // Same place before any mutation: insert before self, or immediately after self.
    if (fromRow == targetRow && (insertCol == fromCol || insertCol == fromCol + 1)) {
        return this
    }

    rows[fromRow].removeAt(fromCol)

    if (fromRow == targetRow) {
        if (insertCol > fromCol) insertCol -= 1
    } else if (rows[fromRow].isEmpty()) {
        rows.removeAt(fromRow)
        if (targetRow > fromRow) targetRow -= 1
    }

    if (targetRow !in rows.indices) return this
    insertCol = insertCol.coerceIn(0, rows[targetRow].size)
    if (rows[targetRow].size >= maxKeysPerRow) return this

    rows[targetRow].add(insertCol, from)
    return reindexRows(rows)
}

/** Move the row at [fromIndex] so it lands at [toIndex] (other rows shift). */
fun Layout.moveRow(
    fromIndex: Int,
    toIndex: Int,
): Layout {
    val rows = layoutRows(this).map { it.toList() }.toMutableList()
    if (fromIndex !in rows.indices || toIndex !in rows.indices || fromIndex == toIndex) {
        return this
    }
    val moved = rows.removeAt(fromIndex)
    rows.add(toIndex, moved)
    return reindexRows(rows)
}

/** Delete the whole row at [rowIndex] and reindex later rows. */
fun Layout.removeRow(rowIndex: Int): Layout {
    val rows = layoutRows(this).map { it.toList() }.toMutableList()
    if (rowIndex !in rows.indices) return this
    rows.removeAt(rowIndex)
    return reindexRows(rows)
}

private fun Layout.reindexRows(rows: List<List<KeyPosition>>): Layout {
    val out = linkedMapOf<KeyPosition, KeyMapping>()
    rows.forEachIndexed { newRow, positions ->
        positions.forEachIndexed { newCol, oldPos ->
            out[KeyPosition(newRow, newCol)] = getValue(oldPos)
        }
    }
    return out
}

/** Exchange the full mappings at two positions (intents, span, slide, labels, and the rest). */
fun Layout.swapKeys(
    a: KeyPosition,
    b: KeyPosition,
): Layout {
    if (a == b) return this
    val mappingA = getValue(a)
    val mappingB = getValue(b)
    return this + (a to mappingB) + (b to mappingA)
}

/**
 * Remembers keys trimmed by [resizeRows] so stepping a row/key count back restores the
 * previous mappings instead of blank pads.
 */
class LayoutResizeMemory {
    private val stash = linkedMapOf<KeyPosition, KeyMapping>()

    fun remember(
        position: KeyPosition,
        mapping: KeyMapping,
    ) {
        stash[position] = mapping
    }

    fun take(position: KeyPosition): KeyMapping? = stash[position]

    fun clear() {
        stash.clear()
    }
}

/**
 * Rebuild a layer grid so each row has exactly [rowSizes] keys (pad / trim).
 * When [memory] is set, trimmed keys are stashed and reused if that cell comes back.
 */
fun Layout.resizeRows(
    rowSizes: List<Int>,
    memory: LayoutResizeMemory? = null,
): Layout {
    val rows = layoutRows(this)
    if (memory != null) {
        rows.forEachIndexed { rowIndex, row ->
            val keep = rowSizes.getOrElse(rowIndex) { 0 }.coerceAtLeast(0)
            row.drop(keep).forEach { pos ->
                memory.remember(pos, getValue(pos))
            }
        }
        rows.drop(rowSizes.size).forEach { row ->
            row.forEach { pos -> memory.remember(pos, getValue(pos)) }
        }
    }
    val out = linkedMapOf<KeyPosition, KeyMapping>()
    rowSizes.forEachIndexed { rowIndex, cols ->
        val existing = rows.getOrNull(rowIndex).orEmpty()
        repeat(cols) { col ->
            val pos = KeyPosition(rowIndex, col)
            val oldPos = existing.getOrNull(col)
            out[pos] =
                when {
                    oldPos != null -> getValue(oldPos)
                    memory != null -> memory.take(pos) ?: blankKeyMapping()
                    else -> blankKeyMapping()
                }
        }
    }
    return out
}

fun NamedLayout.replaceGrid(
    layer: LayoutLayer,
    grid: Layout,
): NamedLayout =
    when (layer) {
        LayoutLayer.MAIN -> copy(layout = grid)
        LayoutLayer.NUMERIC -> copy(numericLayout = grid)
        LayoutLayer.EMOJI -> copy(emojiBottomRow = grid)
        LayoutLayer.CLIPBOARD -> copy(clipboardBottomRow = grid)
    }

fun NamedLayout.gridOrEmpty(layer: LayoutLayer): Layout =
    when (layer) {
        LayoutLayer.MAIN -> layout
        LayoutLayer.NUMERIC -> numericLayout ?: emptyMap()
        LayoutLayer.EMOJI -> emojiBottomRow ?: emptyMap()
        LayoutLayer.CLIPBOARD -> clipboardBottomRow ?: emptyMap()
    }

/**
 * Undo/redo stack for [NamedLayout] edits in the layout editor. [canUndo]/[canRedo] are
 * Compose state so the action buttons recompose when the stacks change.
 */
class LayoutDraftHistory(
    private val maxSize: Int = 80,
) {
    private val undoStack = ArrayDeque<NamedLayout>()
    private val redoStack = ArrayDeque<NamedLayout>()

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    fun peekUndo(): NamedLayout? = undoStack.lastOrNull()

    private fun refresh() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    fun recordBeforeChange(current: NamedLayout) {
        undoStack.addLast(current)
        while (undoStack.size > maxSize) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        refresh()
    }

    fun undo(current: NamedLayout): NamedLayout? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        refresh()
        return previous
    }

    fun redo(current: NamedLayout): NamedLayout? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        refresh()
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        refresh()
    }
}

fun NamedLayout.sameExceptTitle(other: NamedLayout): Boolean = copy(title = other.title) == other
