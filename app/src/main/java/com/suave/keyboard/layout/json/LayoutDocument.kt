package com.suave.keyboard.layout.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Current layout document schema version. Bump when making breaking JSON changes. */
const val LAYOUT_SCHEMA_VERSION = 1

@Serializable
data class LayoutDocument(
    val schemaVersion: Int,
    val id: String,
    val title: String,
    val rows: List<List<KeyDocument>> = emptyList(),
    val numeric: List<List<KeyDocument>>? = null,
    val emojiBottomRow: List<KeyDocument>? = null,
    val clipboardBottomRow: List<KeyDocument>? = null,
    val shiftMappings: Map<String, String> = emptyMap(),
    val capsLockMappings: Map<String, String> = emptyMap(),
    val layerHeights: Map<String, Int> = emptyMap(),
    val layerContent: Map<String, String> = emptyMap(),
    val spaceMultitapCycle: List<String>? = null,
    /** Forward-compat sink: unknown top-level fields are ignored by kotlinx when not listed. */
    val extras: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class KeyDocument(
    val columnSpan: Int = 1,
    val slide: SlideDocument? = null,
    val style: KeyStyleDocument? = null,
    val zones: Map<String, ZoneActionDocument> = emptyMap(),
)

@Serializable
data class SlideDocument(
    val axis: String,
    val behavior: String,
)

@Serializable
data class KeyStyleDocument(
    val fill: String = "auto",
)

@Serializable
sealed class ZoneActionDocument {
    @Serializable
    @SerialName("text")
    data class Text(
        val value: String,
        val label: String? = null,
        val repeatsOnHold: Boolean? = null,
    ) : ZoneActionDocument()

    @Serializable
    @SerialName("command")
    data class Command(
        val id: String,
        val repeatsOnHold: Boolean? = null,
    ) : ZoneActionDocument()

    @Serializable
    @SerialName("modifier")
    data class Modifier(
        val id: String,
    ) : ZoneActionDocument()

    @Serializable
    @SerialName("noop")
    data object Noop : ZoneActionDocument()
}
