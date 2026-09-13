package com.suave.keyboard.layout.json

import com.suave.keyboard.engine.gesture.Direction
import com.suave.keyboard.engine.gesture.GestureConfig
import com.suave.keyboard.engine.gesture.SlideAxis
import com.suave.keyboard.engine.gesture.Zone
import com.suave.keyboard.engine.gesture.withOccupiedDirections
import com.suave.keyboard.engine.intent.CaseOverride
import com.suave.keyboard.engine.intent.CommandId
import com.suave.keyboard.engine.intent.KeyFillRole
import com.suave.keyboard.engine.intent.KeyIntent
import com.suave.keyboard.engine.intent.KeyMapping
import com.suave.keyboard.engine.intent.KeyPosition
import com.suave.keyboard.engine.intent.Layout
import com.suave.keyboard.engine.intent.ModifierId
import com.suave.keyboard.engine.intent.SlideBehavior
import com.suave.keyboard.engine.intent.TextCaseOverrides
import com.suave.keyboard.engine.intent.layoutRows
import com.suave.keyboard.layout.LayerContent
import com.suave.keyboard.layout.LayerDefinition
import com.suave.keyboard.layout.LayerIcon
import com.suave.keyboard.layout.MAX_LAYERS
import com.suave.keyboard.layout.NamedLayout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val DEFAULT_GESTURE = GestureConfig(minSwipeDistancePx = 64f)

@OptIn(ExperimentalSerializationApi::class)
val LayoutJsonFormat =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = true
        prettyPrintIndent = "  "
        classDiscriminator = "type"
        explicitNulls = true
    }

class LayoutJsonException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

fun parseLayoutDocument(
    json: String,
    migrator: LayoutSchemaMigrator = IdentityLayoutSchemaMigrator,
): LayoutDocument {
    val raw =
        try {
            LayoutJsonFormat.decodeFromString(LayoutDocument.serializer(), json)
        } catch (e: Exception) {
            throw LayoutJsonException("Invalid layout JSON: ${e.message}", e)
        }
    if (raw.schemaVersion != LAYOUT_SCHEMA_VERSION) {
        try {
            return migrator.migrate(raw, raw.schemaVersion).copy(schemaVersion = LAYOUT_SCHEMA_VERSION)
        } catch (e: Exception) {
            throw LayoutJsonException(
                "Unsupported layout schemaVersion ${raw.schemaVersion} (current is $LAYOUT_SCHEMA_VERSION)",
                e,
            )
        }
    }
    return raw
}

fun encodeLayoutDocument(document: LayoutDocument): String = LayoutJsonFormat.encodeToString(document)

fun LayoutDocument.toNamedLayout(): NamedLayout {
    require(id.isNotBlank()) { "Layout id must not be blank" }
    require(title.isNotBlank()) { "Layout title must not be blank" }
    require(homeLayerId.isNotBlank()) { "homeLayerId must not be blank" }
    require(layers.isNotEmpty()) { "layers must not be empty" }
    require(layers.size <= MAX_LAYERS) { "At most $MAX_LAYERS layers" }
    val compiled = layers.map { it.toLayerDefinition() }
    val ids = compiled.map { it.id }
    require(ids.distinct().size == ids.size) { "Layer ids must be unique" }
    require(homeLayerId in ids) { "homeLayerId '$homeLayerId' must match a layer id" }
    return NamedLayout(
        id = id,
        title = title,
        homeLayerId = homeLayerId,
        layers = compiled,
        shiftMappings = caseMaps.shift,
        capsLockMappings = caseMaps.capsLock,
        spaceMultitapCycle = spaceMultitapCycle,
    )
}

fun NamedLayout.toLayoutDocument(): LayoutDocument =
    LayoutDocument(
        schemaVersion = LAYOUT_SCHEMA_VERSION,
        id = id,
        title = title,
        homeLayerId = homeLayerId,
        layers = layers.map { it.toLayerDocument() },
        caseMaps =
            CaseMapsDocument(
                shift = shiftMappings,
                capsLock = capsLockMappings,
            ),
        spaceMultitapCycle = spaceMultitapCycle,
    )

fun decodeNamedLayout(json: String): NamedLayout = parseLayoutDocument(json).toNamedLayout()

fun encodeNamedLayout(layout: NamedLayout): String = encodeLayoutDocument(layout.toLayoutDocument())

private fun LayerDocument.toLayerDefinition(): LayerDefinition {
    require(id.isNotBlank()) { "Layer id must not be blank" }
    require(title.isNotBlank()) { "Layer title must not be blank" }
    val icon =
        try {
            LayerIcon.valueOf(icon)
        } catch (e: IllegalArgumentException) {
            throw LayoutJsonException("Unknown layer icon: $icon", e)
        }

    var content: LayerContent = LayerContent.None
    var contentRows = 0
    var contentColumnSpan = 1f
    val keyRows = ArrayList<List<CellDocument>>()

    for ((rowIndex, row) in rows.withIndex()) {
        if (row.isEmpty()) {
            throw LayoutJsonException("Layer '$id' row $rowIndex is empty")
        }
        val panelCells =
            row.filter { it is CellDocument.EmojiPicker || it is CellDocument.ClipboardHistory }
        val gridCells =
            row.filter { it is CellDocument.Key || it is CellDocument.Spacer }
        when {
            panelCells.isNotEmpty() && gridCells.isNotEmpty() ->
                throw LayoutJsonException(
                    "Layer '$id' row $rowIndex mixes content panels with keys (full-width strips only)",
                )
            panelCells.size > 1 ->
                throw LayoutJsonException(
                    "Layer '$id' row $rowIndex has multiple content cells",
                )
            panelCells.size == 1 -> {
                if (content != LayerContent.None) {
                    throw LayoutJsonException("Layer '$id' has more than one content strip")
                }
                when (val cell = panelCells.single()) {
                    is CellDocument.EmojiPicker -> {
                        require(cell.rowSpan >= 1) { "rowSpan must be >= 1" }
                        require(cell.columnSpan > 0f) { "columnSpan must be > 0" }
                        content = LayerContent.EmojiPicker
                        contentRows = cell.rowSpan
                        contentColumnSpan = cell.columnSpan
                    }
                    is CellDocument.ClipboardHistory -> {
                        require(cell.rowSpan >= 1) { "rowSpan must be >= 1" }
                        require(cell.columnSpan > 0f) { "columnSpan must be > 0" }
                        content = LayerContent.ClipboardHistory
                        contentRows = cell.rowSpan
                        contentColumnSpan = cell.columnSpan
                    }
                    else -> error("unreachable")
                }
            }
            else -> {
                if (gridCells.size != row.size) {
                    throw LayoutJsonException("Layer '$id' row $rowIndex has unknown cell types")
                }
                keyRows.add(gridCells)
            }
        }
    }

    val keyGrid =
        if (keyRows.isEmpty()) {
            linkedMapOf(KeyPosition(0, 0) to blankNoopKey())
        } else {
            keyRows.toKeyLayout()
        }

    return LayerDefinition(
        id = id,
        title = title,
        icon = icon,
        overlay = overlay,
        keyGrid = keyGrid,
        content = content,
        contentRows = contentRows,
        contentColumnSpan = contentColumnSpan,
    )
}

private fun blankNoopKey(): KeyMapping {
    val intents = mapOf<Zone, KeyIntent>(Zone.Center to KeyIntent.Noop)
    return KeyMapping(
        gestureConfig = DEFAULT_GESTURE.withOccupiedDirections(intents),
        intents = intents,
    )
}

private fun LayerDefinition.toLayerDocument(): LayerDocument {
    val rows = ArrayList<List<CellDocument>>()
    if (content != LayerContent.None && contentRows > 0) {
        val cell: CellDocument =
            when (content) {
                LayerContent.EmojiPicker ->
                    CellDocument.EmojiPicker(rowSpan = contentRows, columnSpan = contentColumnSpan)
                LayerContent.ClipboardHistory ->
                    CellDocument.ClipboardHistory(rowSpan = contentRows, columnSpan = contentColumnSpan)
                LayerContent.None -> error("unreachable")
            }
        rows.add(listOf(cell))
    }
    for (row in layoutRows(keyGrid)) {
        rows.add(
            row.map { pos ->
                val mapping = keyGrid.getValue(pos)
                if (mapping.fillRole == KeyFillRole.SPACER) {
                    CellDocument.Spacer(columnSpan = mapping.columnSpan)
                } else {
                    mapping.toKeyCell()
                }
            },
        )
    }
    return LayerDocument(
        id = id,
        title = title,
        icon = icon.name,
        overlay = overlay,
        rows = rows,
    )
}

private fun List<List<CellDocument>>.toKeyLayout(): Layout {
    val out = linkedMapOf<KeyPosition, KeyMapping>()
    for ((rowIndex, row) in withIndex()) {
        for ((colIndex, cell) in row.withIndex()) {
            out[KeyPosition(rowIndex, colIndex)] =
                when (cell) {
                    is CellDocument.Key -> cell.toKeyMapping()
                    is CellDocument.Spacer -> {
                        require(cell.columnSpan > 0f) { "spacer columnSpan must be > 0" }
                        blankNoopKey().copy(
                            columnSpan = cell.columnSpan,
                            fillRole = KeyFillRole.SPACER,
                        )
                    }
                    else -> throw LayoutJsonException("Unexpected cell in key row: $cell")
                }
        }
    }
    return out
}

private fun CellDocument.Key.toKeyMapping(): KeyMapping {
    require(columnSpan > 0f) { "columnSpan must be > 0, got $columnSpan" }
    val intents = linkedMapOf<Zone, KeyIntent>()
    val displayLabels = linkedMapOf<Zone, String>()
    val repeatOverrides = linkedMapOf<Zone, Boolean>()
    for ((name, action) in zones) {
        val zone = zoneFromName(name)
        when (action) {
            is ZoneActionDocument.Text -> {
                intents[zone] =
                    KeyIntent.Text(
                        text = action.value,
                        case = action.case.toTextCaseOverrides(),
                    )
                action.label?.let { displayLabels[zone] = it }
                action.repeatsOnHold?.let { repeatOverrides[zone] = it }
            }
            is ZoneActionDocument.Command -> {
                intents[zone] = KeyIntent.Command(commandIdFromName(action.id))
                action.label?.let { displayLabels[zone] = it }
                action.repeatsOnHold?.let { repeatOverrides[zone] = it }
            }
            is ZoneActionDocument.Modifier -> {
                intents[zone] = KeyIntent.ModifierPress(modifierIdFromName(action.id))
            }
            is ZoneActionDocument.Noop -> {
                intents[zone] = KeyIntent.Noop
            }
            is ZoneActionDocument.SwitchLayer -> {
                require(action.layerId.isNotBlank()) { "switchLayer.layerId must not be blank" }
                intents[zone] = KeyIntent.SwitchLayer(action.layerId)
            }
        }
    }
    require(Zone.Center in intents) { "Key must define a center zone" }
    val slideAxis = slide?.let { slideAxisFromName(it.axis) }
    val slideBehavior = slide?.let { slideBehaviorFromName(it.behavior) }
    val gesture =
        DEFAULT_GESTURE
            .copy(slideAxis = slideAxis)
            .withOccupiedDirections(intents)
    return KeyMapping(
        gestureConfig = gesture,
        intents = intents,
        slideBehavior = slideBehavior,
        columnSpan = columnSpan,
        fillRole = fillRoleFromName(style?.fill),
        displayLabels = displayLabels,
        repeatOverrides = repeatOverrides,
    )
}

private fun KeyMapping.toKeyCell(): CellDocument.Key {
    val zones = linkedMapOf<String, ZoneActionDocument>()
    for ((zone, intent) in intents) {
        val name = zoneToName(zone)
        val label = displayLabels[zone]
        val repeat = repeatOverrides[zone]
        zones[name] =
            when (intent) {
                is KeyIntent.Text ->
                    ZoneActionDocument.Text(
                        value = intent.text,
                        label = label,
                        repeatsOnHold = repeat,
                        case = intent.case.toTextCaseDocument(),
                    )
                is KeyIntent.Command ->
                    ZoneActionDocument.Command(
                        id = intent.id.name,
                        label = label,
                        repeatsOnHold = repeat,
                    )
                is KeyIntent.ModifierPress ->
                    ZoneActionDocument.Modifier(id = intent.modifier.name)
                is KeyIntent.SwitchLayer ->
                    ZoneActionDocument.SwitchLayer(layerId = intent.layerId)
                is KeyIntent.Noop -> ZoneActionDocument.Noop
            }
    }
    val slide =
        gestureConfig.slideAxis?.let { axis ->
            SlideDocument(
                axis = axis.name,
                behavior = (slideBehavior ?: SlideBehavior.MOVE_CURSOR).name,
            )
        }
    val style =
        when (fillRole) {
            KeyFillRole.AUTO -> null
            KeyFillRole.LETTER -> KeyStyleDocument(fill = "letter")
            KeyFillRole.CONTROL -> KeyStyleDocument(fill = "control")
            KeyFillRole.SPACER -> KeyStyleDocument(fill = "spacer")
        }
    return CellDocument.Key(
        columnSpan = columnSpan,
        slide = slide,
        style = style,
        zones = zones,
    )
}

private fun TextCaseDocument?.toTextCaseOverrides(): TextCaseOverrides {
    if (this == null) return TextCaseOverrides.DEFAULT
    return TextCaseOverrides(
        shift = shift.toCaseOverride(),
        capsLock = capsLock.toCaseOverride(),
    )
}

private fun TriStateString.toCaseOverride(): CaseOverride =
    when (this) {
        TriStateString.Inherit -> CaseOverride.Inherit
        TriStateString.Disable -> CaseOverride.Disable
        is TriStateString.Value -> CaseOverride.Fixed(value)
    }

private fun TextCaseOverrides.toTextCaseDocument(): TextCaseDocument? {
    if (isDefault) return null
    return TextCaseDocument(
        shift = shift.toTriState(),
        capsLock = capsLock.toTriState(),
    )
}

private fun CaseOverride.toTriState(): TriStateString =
    when (this) {
        CaseOverride.Inherit -> TriStateString.Inherit
        CaseOverride.Disable -> TriStateString.Disable
        is CaseOverride.Fixed -> TriStateString.Value(value)
    }

private fun zoneFromName(name: String): Zone =
    when (name) {
        "center" -> Zone.Center
        "up" -> Zone.Directional(Direction.UP)
        "down" -> Zone.Directional(Direction.DOWN)
        "left" -> Zone.Directional(Direction.LEFT)
        "right" -> Zone.Directional(Direction.RIGHT)
        "upLeft" -> Zone.Directional(Direction.UP_LEFT)
        "upRight" -> Zone.Directional(Direction.UP_RIGHT)
        "downLeft" -> Zone.Directional(Direction.DOWN_LEFT)
        "downRight" -> Zone.Directional(Direction.DOWN_RIGHT)
        else -> throw LayoutJsonException("Unknown zone name: $name")
    }

private fun zoneToName(zone: Zone): String =
    when (zone) {
        Zone.Center -> "center"
        is Zone.Directional ->
            when (zone.direction) {
                Direction.UP -> "up"
                Direction.DOWN -> "down"
                Direction.LEFT -> "left"
                Direction.RIGHT -> "right"
                Direction.UP_LEFT -> "upLeft"
                Direction.UP_RIGHT -> "upRight"
                Direction.DOWN_LEFT -> "downLeft"
                Direction.DOWN_RIGHT -> "downRight"
            }
    }

private fun commandIdFromName(name: String): CommandId =
    try {
        CommandId.valueOf(name)
    } catch (e: IllegalArgumentException) {
        throw LayoutJsonException("Unknown command id: $name", e)
    }

private fun modifierIdFromName(name: String): ModifierId =
    try {
        ModifierId.valueOf(name)
    } catch (e: IllegalArgumentException) {
        throw LayoutJsonException("Unknown modifier id: $name", e)
    }

private fun slideAxisFromName(name: String): SlideAxis =
    try {
        SlideAxis.valueOf(name)
    } catch (e: IllegalArgumentException) {
        throw LayoutJsonException("Unknown slide axis: $name", e)
    }

private fun slideBehaviorFromName(name: String): SlideBehavior =
    try {
        SlideBehavior.valueOf(name)
    } catch (e: IllegalArgumentException) {
        throw LayoutJsonException("Unknown slide behavior: $name", e)
    }

private fun fillRoleFromName(name: String?): KeyFillRole =
    when (name?.lowercase()) {
        null, "auto" -> KeyFillRole.AUTO
        "letter" -> KeyFillRole.LETTER
        "control" -> KeyFillRole.CONTROL
        "spacer" -> KeyFillRole.SPACER
        else -> throw LayoutJsonException("Unknown style.fill: $name")
    }
