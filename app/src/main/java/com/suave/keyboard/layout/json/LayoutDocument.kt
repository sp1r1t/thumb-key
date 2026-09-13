package com.suave.keyboard.layout.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** Current layout document schema version. Bump when making breaking JSON changes. */
const val LAYOUT_SCHEMA_VERSION = 1

@Serializable
data class LayoutDocument(
    val schemaVersion: Int,
    val id: String,
    val title: String,
    val homeLayerId: String,
    val layers: List<LayerDocument> = emptyList(),
    val caseMaps: CaseMapsDocument = CaseMapsDocument(),
    val spaceMultitapCycle: List<String>? = null,
    /** Forward-compat sink: unknown top-level fields are ignored by kotlinx when not listed. */
    val extras: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class CaseMapsDocument(
    val shift: Map<String, String> = emptyMap(),
    val capsLock: Map<String, String> = emptyMap(),
)

@Serializable
data class LayerDocument(
    val id: String,
    val title: String,
    val icon: String = "Functions",
    val overlay: Boolean = false,
    val rows: List<List<CellDocument>> = emptyList(),
)

@Serializable
sealed class CellDocument {
    @Serializable
    @SerialName("key")
    data class Key(
        val columnSpan: Float = 1f,
        val slide: SlideDocument? = null,
        val style: KeyStyleDocument? = null,
        val zones: Map<String, ZoneActionDocument> = emptyMap(),
    ) : CellDocument()

    @Serializable
    @SerialName("spacer")
    data class Spacer(
        val columnSpan: Float = 1f,
    ) : CellDocument()

    @Serializable
    @SerialName("emojiPicker")
    data class EmojiPicker(
        val rowSpan: Int = 1,
        val columnSpan: Float = 1f,
    ) : CellDocument()

    @Serializable
    @SerialName("clipboardHistory")
    data class ClipboardHistory(
        val rowSpan: Int = 1,
        val columnSpan: Float = 1f,
    ) : CellDocument()
}

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
        val case: TextCaseDocument? = null,
    ) : ZoneActionDocument()

    @Serializable
    @SerialName("command")
    data class Command(
        val id: String,
        val label: String? = null,
        val repeatsOnHold: Boolean? = null,
    ) : ZoneActionDocument()

    @Serializable
    @SerialName("modifier")
    data class Modifier(
        val id: String,
    ) : ZoneActionDocument()

    @Serializable
    @SerialName("switchLayer")
    data class SwitchLayer(
        val layerId: String,
    ) : ZoneActionDocument()

    @Serializable
    @SerialName("noop")
    data object Noop : ZoneActionDocument()
}

/**
 * Per-modifier case override. Serialized as a small object:
 * omit field = inherit, JSON null = disable, string = fixed.
 */
@Serializable(with = TextCaseDocumentSerializer::class)
data class TextCaseDocument(
    val shift: TriStateString = TriStateString.Inherit,
    val capsLock: TriStateString = TriStateString.Inherit,
)

sealed class TriStateString {
    data object Inherit : TriStateString()

    data object Disable : TriStateString()

    data class Value(
        val value: String,
    ) : TriStateString()
}

object TextCaseDocumentSerializer : KSerializer<TextCaseDocument> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("TextCaseDocument") {
            element<JsonElement>("shift", isOptional = true)
            element<JsonElement>("capsLock", isOptional = true)
        }

    override fun serialize(
        encoder: Encoder,
        value: TextCaseDocument,
    ) {
        val json =
            encoder as? JsonEncoder
                ?: error("TextCaseDocumentSerializer requires JsonEncoder")
        val obj =
            buildJsonObject {
                putTriState("shift", value.shift)
                putTriState("capsLock", value.capsLock)
            }
        json.encodeJsonElement(obj)
    }

    override fun deserialize(decoder: Decoder): TextCaseDocument {
        val json =
            decoder as? JsonDecoder
                ?: error("TextCaseDocumentSerializer requires JsonDecoder")
        val obj = json.decodeJsonElement().jsonObject
        return TextCaseDocument(
            shift = obj["shift"].toTriState(),
            capsLock = obj["capsLock"].toTriState(),
        )
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putTriState(
        key: String,
        value: TriStateString,
    ) {
        when (value) {
            TriStateString.Inherit -> Unit
            TriStateString.Disable -> put(key, JsonNull)
            is TriStateString.Value -> put(key, value.value)
        }
    }

    private fun JsonElement?.toTriState(): TriStateString =
        when (this) {
            null -> TriStateString.Inherit
            JsonNull -> TriStateString.Disable
            is JsonPrimitive -> TriStateString.Value(content)
            else -> throw IllegalArgumentException("case override must be string or null")
        }
}
