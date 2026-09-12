package com.suave.keyboard.layout.json

import com.suave.keyboard.layout.BuiltinLayouts
import com.suave.keyboard.layout.CustomLayerIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LayoutCodecTest {
    @Test
    fun `s12 round-trips through JSON`() {
        val original = BuiltinLayouts.S12
        val json = encodeNamedLayout(original)
        val restored = decodeNamedLayout(json)

        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(original.layout.keys, restored.layout.keys)
        assertEquals(original.numericLayout?.keys, restored.numericLayout?.keys)
        assertEquals(original.emojiBottomRow?.keys, restored.emojiBottomRow?.keys)
        assertEquals(original.clipboardBottomRow?.keys, restored.clipboardBottomRow?.keys)
        assertEquals(original.shiftMappings, restored.shiftMappings)
        assertEquals(original.capsLockMappings, restored.capsLockMappings)
        assertEquals(original.layerHeights, restored.layerHeights)
        assertEquals(original.layerContent, restored.layerContent)

        for (pos in original.layout.keys) {
            val a = original.layout.getValue(pos)
            val b = restored.layout.getValue(pos)
            assertEquals("intents at $pos", a.intents, b.intents)
            assertEquals("slide at $pos", a.slideBehavior, b.slideBehavior)
            assertEquals("span at $pos", a.columnSpan, b.columnSpan)
            assertEquals("slideAxis at $pos", a.gestureConfig.slideAxis, b.gestureConfig.slideAxis)
            assertEquals(
                "occupied at $pos",
                a.gestureConfig.occupiedDirections,
                b.gestureConfig.occupiedDirections,
            )
        }
    }

    @Test
    fun `uneven rows and span survive encode`() {
        val json =
            """
            {
              "schemaVersion": 1,
              "id": "uneven",
              "title": "Uneven",
              "rows": [
                [
                  { "zones": { "center": { "type": "text", "value": "a" } } },
                  { "zones": { "center": { "type": "text", "value": "b" } } }
                ],
                [
                  {
                    "columnSpan": 2,
                    "zones": { "center": { "type": "command", "id": "ENTER" } }
                  }
                ]
              ]
            }
            """.trimIndent()
        val layout = decodeNamedLayout(json)
        assertEquals(2, layout.layout.keys.count { it.row == 0 })
        assertEquals(1, layout.layout.keys.count { it.row == 1 })
        assertEquals(2, layout.layout.getValue(com.suave.keyboard.engine.intent.KeyPosition(1, 0)).columnSpan)
    }

    @Test
    fun `unknown schema version is rejected`() {
        val json =
            """
            { "schemaVersion": 99, "id": "x", "title": "X", "rows": [] }
            """.trimIndent()
        try {
            decodeNamedLayout(json)
            throw AssertionError("expected LayoutJsonException")
        } catch (e: LayoutJsonException) {
            assertTrue(e.message!!.contains("schemaVersion"))
        }
    }

    @Test
    fun `extraLayers and switchLayer round-trip`() {
        val json =
            """
            {
              "schemaVersion": 1,
              "id": "extra",
              "title": "Extra",
              "rows": [
                [
                  {
                    "zones": {
                      "center": { "type": "switchLayer", "layerId": "custom_fn1" }
                    }
                  }
                ]
              ],
              "extraLayers": [
                {
                  "id": "custom_fn1",
                  "title": "Fn",
                  "icon": "Star",
                  "rows": [
                    [
                      { "zones": { "center": { "type": "text", "value": "!" } } }
                    ]
                  ]
                }
              ]
            }
            """.trimIndent()
        val layout = decodeNamedLayout(json)
        assertEquals(1, layout.customLayers.size)
        assertEquals("Fn", layout.customLayers.single().title)
        assertEquals(CustomLayerIcon.Star, layout.customLayers.single().icon)
        val center = layout.layout.getValue(com.suave.keyboard.engine.intent.KeyPosition(0, 0))
        assertEquals(
            com.suave.keyboard.engine.intent.KeyIntent.SwitchLayer("custom_fn1"),
            center.intents[com.suave.keyboard.engine.gesture.Zone.Center],
        )
        val restored = decodeNamedLayout(encodeNamedLayout(layout))
        assertEquals(layout.customLayers, restored.customLayers)
        assertEquals(center.intents, restored.layout.getValue(com.suave.keyboard.engine.intent.KeyPosition(0, 0)).intents)
    }

    @Test
    fun `export s12 asset when EXPORT_S12_LAYOUT is set`() {
        val out = System.getenv("EXPORT_S12_LAYOUT") ?: return
        File(out).parentFile?.mkdirs()
        File(out).writeText(encodeNamedLayout(BuiltinLayouts.S12))
    }
}
