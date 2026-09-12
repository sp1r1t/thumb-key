package com.suave.keyboard.layout.json

import com.suave.keyboard.engine.gesture.Direction
import com.suave.keyboard.engine.gesture.GestureConfig
import com.suave.keyboard.engine.gesture.SlideAxis
import com.suave.keyboard.engine.gesture.Zone
import com.suave.keyboard.engine.gesture.withOccupiedDirections
import com.suave.keyboard.engine.intent.CommandId
import com.suave.keyboard.engine.intent.KeyFillRole
import com.suave.keyboard.engine.intent.KeyIntent
import com.suave.keyboard.engine.intent.KeyMapping
import com.suave.keyboard.engine.intent.KeyPosition
import com.suave.keyboard.engine.intent.Layout
import com.suave.keyboard.engine.intent.ModifierId
import com.suave.keyboard.engine.intent.SlideBehavior
import com.suave.keyboard.engine.intent.layoutRows
import com.suave.keyboard.layout.CustomLayer
import com.suave.keyboard.layout.CustomLayerIcon
import com.suave.keyboard.layout.LayerContent
import com.suave.keyboard.layout.LayoutLayer
import com.suave.keyboard.layout.MAX_CUSTOM_LAYERS
import com.suave.keyboard.layout.NamedLayout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val DEFAULT_GESTURE = GestureConfig(minSwipeDistancePx = 64f)

val LayoutJsonFormat =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = true
        prettyPrintIndent = "  "
        classDiscriminator = "type"
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
        // Reject until a migrator for this version exists (identity only handles current).
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
    return NamedLayout(
        id = id,
        title = title,
        layout = rows.toLayout(),
        numericLayout = numeric?.takeIf { it.isNotEmpty() }?.toLayout(),
        emojiBottomRow = emojiBottomRow?.takeIf { it.isNotEmpty() }?.let { listOf(it).toLayout() },
        clipboardBottomRow = clipboardBottomRow?.takeIf { it.isNotEmpty() }?.let { listOf(it).toLayout() },
        shiftMappings = shiftMappings,
        capsLockMappings = capsLockMappings,
        layerHeights = layerHeights.toLayerHeights(),
        layerContent = layerContent.toLayerContent(),
        spaceMultitapCycle = spaceMultitapCycle,
        customLayers = extraLayers.toCustomLayers(),
    )
}

fun NamedLayout.toLayoutDocument(): LayoutDocument =
    LayoutDocument(
        schemaVersion = LAYOUT_SCHEMA_VERSION,
        id = id,
        title = title,
        rows = layout.toKeyRows(),
        numeric = numericLayout?.toKeyRows(),
        emojiBottomRow = emojiBottomRow?.toSingleRow(),
        clipboardBottomRow = clipboardBottomRow?.toSingleRow(),
        shiftMappings = shiftMappings,
        capsLockMappings = capsLockMappings,
        layerHeights =
            layerHeights
                .filterKeys { it != LayoutLayer.MAIN }
                .mapKeys { it.key.name },
        layerContent =
            layerContent
                .mapNotNull { (layer, content) ->
                    content.toJsonName()?.let { layer.name to it }
                }.toMap(),
        spaceMultitapCycle = spaceMultitapCycle,
        extraLayers = customLayers.toExtraLayerDocuments(),
    )

fun decodeNamedLayout(json: String): NamedLayout = parseLayoutDocument(json).toNamedLayout()

fun encodeNamedLayout(layout: NamedLayout): String = encodeLayoutDocument(layout.toLayoutDocument())

private fun List<List<KeyDocument>>.toLayout(): Layout {
    val out = linkedMapOf<KeyPosition, KeyMapping>()
    for ((rowIndex, row) in withIndex()) {
        for ((colIndex, key) in row.withIndex()) {
            out[KeyPosition(rowIndex, colIndex)] = key.toKeyMapping()
        }
    }
    return out
}

private fun Layout.toKeyRows(): List<List<KeyDocument>> =
    layoutRows(this).map { row ->
        row.map { pos -> getValue(pos).toKeyDocument() }
    }

private fun Layout.toSingleRow(): List<KeyDocument> {
    val rows = toKeyRows()
    require(rows.size <= 1) { "Overlay bottom row must be a single row, got ${rows.size}" }
    return rows.firstOrNull().orEmpty()
}

private fun KeyDocument.toKeyMapping(): KeyMapping {
    require(columnSpan >= 1) { "columnSpan must be at least 1, got $columnSpan" }
    val intents = linkedMapOf<Zone, KeyIntent>()
    val displayLabels = linkedMapOf<Zone, String>()
    val repeatOverrides = linkedMapOf<Zone, Boolean>()
    for ((name, action) in zones) {
        val zone = zoneFromName(name)
        when (action) {
            is ZoneActionDocument.Text -> {
                intents[zone] = KeyIntent.Text(action.value)
                action.label?.let { displayLabels[zone] = it }
                action.repeatsOnHold?.let { repeatOverrides[zone] = it }
            }
            is ZoneActionDocument.Command -> {
                intents[zone] = KeyIntent.Command(commandIdFromName(action.id))
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

private fun KeyMapping.toKeyDocument(): KeyDocument {
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
                    )
                is KeyIntent.Command ->
                    ZoneActionDocument.Command(
                        id = intent.id.name,
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
        }
    return KeyDocument(
        columnSpan = columnSpan,
        slide = slide,
        style = style,
        zones = zones,
    )
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
        else -> throw LayoutJsonException("Unknown style.fill: $name")
    }

private fun Map<String, Int>.toLayerHeights(): Map<LayoutLayer, Int> =
    mapNotNull { (name, rows) ->
        val layer =
            try {
                LayoutLayer.valueOf(name)
            } catch (_: IllegalArgumentException) {
                return@mapNotNull null
            }
        if (rows > 0) layer to rows else null
    }.toMap()

private fun Map<String, String>.toLayerContent(): Map<LayoutLayer, LayerContent> =
    mapNotNull { (name, content) ->
        val layer =
            try {
                LayoutLayer.valueOf(name)
            } catch (_: IllegalArgumentException) {
                return@mapNotNull null
            }
        val parsed =
            when (content) {
                "none" -> LayerContent.None
                "emojiPicker" -> LayerContent.EmojiPicker
                "clipboardHistory" -> LayerContent.ClipboardHistory
                else -> throw LayoutJsonException("Unknown layerContent: $content")
            }
        layer to parsed
    }.toMap()

private fun LayerContent.toJsonName(): String? =
    when (this) {
        LayerContent.None -> null
        LayerContent.EmojiPicker -> "emojiPicker"
        LayerContent.ClipboardHistory -> "clipboardHistory"
    }

private fun List<ExtraLayerDocument>.toCustomLayers(): List<CustomLayer> {
    val out = ArrayList<CustomLayer>(size.coerceAtMost(MAX_CUSTOM_LAYERS))
    val seen = HashSet<String>()
    for (doc in this) {
        if (out.size >= MAX_CUSTOM_LAYERS) break
        if (doc.id.isBlank() || doc.title.isBlank()) continue
        if (!seen.add(doc.id)) continue
        // Builtin names are reserved for LayoutLayer switches.
        val reserved =
            try {
                LayoutLayer.valueOf(doc.id)
                true
            } catch (_: IllegalArgumentException) {
                false
            }
        if (reserved) continue
        val icon =
            try {
                CustomLayerIcon.valueOf(doc.icon)
            } catch (_: IllegalArgumentException) {
                CustomLayerIcon.Functions
            }
        val grid = doc.rows.takeIf { it.isNotEmpty() }?.toLayout() ?: continue
        out.add(CustomLayer(id = doc.id, title = doc.title, icon = icon, layout = grid))
    }
    return out
}

private fun List<CustomLayer>.toExtraLayerDocuments(): List<ExtraLayerDocument> =
    take(MAX_CUSTOM_LAYERS).map { layer ->
        ExtraLayerDocument(
            id = layer.id,
            title = layer.title,
            icon = layer.icon.name,
            rows = layer.layout.toKeyRows(),
        )
    }
