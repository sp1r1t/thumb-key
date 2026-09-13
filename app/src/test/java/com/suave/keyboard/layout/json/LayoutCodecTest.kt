package com.suave.keyboard.layout.json

import com.suave.keyboard.layout.ActiveLayer
import com.suave.keyboard.layout.LayerContent
import com.suave.keyboard.layout.NamedLayout
import com.suave.keyboard.layout.S12_CAPS_LOCK_MAPPINGS
import com.suave.keyboard.layout.S12_SHIFT_MAPPINGS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LayoutCodecTest {
    @Test
    fun `s12 asset round-trips through JSON`() {
        val original = loadS12Asset()
        val json = encodeNamedLayout(original)
        val restored = decodeNamedLayout(json)

        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(original.homeLayerId, restored.homeLayerId)
        assertEquals(original.layers.map { it.id }, restored.layers.map { it.id })
        assertEquals(original.shiftMappings, restored.shiftMappings)
        assertEquals(original.capsLockMappings, restored.capsLockMappings)

        for (layer in original.layers) {
            val other = restored.requireLayer(layer.id)
            assertEquals(layer.content, other.content)
            assertEquals(layer.contentRows, other.contentRows)
            assertEquals(layer.overlay, other.overlay)
            assertEquals(layer.keyGrid.keys, other.keyGrid.keys)
            for (pos in layer.keyGrid.keys) {
                val a = layer.keyGrid.getValue(pos)
                val b = other.keyGrid.getValue(pos)
                assertEquals("intents ${layer.id}@$pos", a.intents, b.intents)
                assertEquals("span ${layer.id}@$pos", a.columnSpan, b.columnSpan)
            }
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
              "homeLayerId": "main",
              "layers": [
                {
                  "id": "main",
                  "title": "ABC",
                  "icon": "Abc",
                  "rows": [
                    [
                      { "type": "key", "zones": { "center": { "type": "text", "value": "a" } } },
                      { "type": "key", "zones": { "center": { "type": "text", "value": "b" } } }
                    ],
                    [
                      {
                        "type": "key",
                        "columnSpan": 2,
                        "zones": { "center": { "type": "command", "id": "ENTER" } }
                      }
                    ]
                  ]
                }
              ]
            }
            """.trimIndent()
        val layout = decodeNamedLayout(json)
        val grid = layout.homeLayer().keyGrid
        assertEquals(2, grid.keys.count { it.row == 0 })
        assertEquals(1, grid.keys.count { it.row == 1 })
        assertEquals(2f, grid.getValue(com.suave.keyboard.engine.intent.KeyPosition(1, 0)).columnSpan)
    }

    @Test
    fun `caseMaps and per-text case overrides round-trip`() {
        val json =
            """
            {
              "schemaVersion": 1,
              "id": "case",
              "title": "Case",
              "homeLayerId": "main",
              "caseMaps": { "shift": { "ß": "SS" }, "capsLock": { "ß": "ẞ" } },
              "layers": [
                {
                  "id": "main",
                  "title": "ABC",
                  "icon": "Abc",
                  "rows": [
                    [
                      {
                        "type": "key",
                        "zones": {
                          "center": {
                            "type": "text",
                            "value": "ß",
                            "case": { "shift": "ẞ", "capsLock": null }
                          }
                        }
                      }
                    ]
                  ]
                }
              ]
            }
            """.trimIndent()
        val layout = decodeNamedLayout(json)
        assertEquals(mapOf("ß" to "SS"), layout.shiftMappings)
        val text =
            layout.homeLayer().keyGrid.values.first().intents.values.first()
                as com.suave.keyboard.engine.intent.KeyIntent.Text
        assertEquals(
            com.suave.keyboard.engine.intent.CaseOverride.Fixed("ẞ"),
            text.case.shift,
        )
        assertEquals(
            com.suave.keyboard.engine.intent.CaseOverride.Disable,
            text.case.capsLock,
        )
        val again = decodeNamedLayout(encodeNamedLayout(layout))
        val text2 =
            again.homeLayer().keyGrid.values.first().intents.values.first()
                as com.suave.keyboard.engine.intent.KeyIntent.Text
        assertEquals(text.case, text2.case)
    }

    @Test
    fun `unknown schema version is rejected`() {
        val json =
            """
            { "schemaVersion": 99, "id": "x", "title": "X", "homeLayerId": "main", "layers": [] }
            """.trimIndent()
        try {
            decodeNamedLayout(json)
            throw AssertionError("expected LayoutJsonException")
        } catch (e: LayoutJsonException) {
            assertTrue(e.message!!.contains("schemaVersion"))
        }
    }

    @Test
    fun `extra layer and switchLayer survive encode`() {
        val json =
            """
            {
              "schemaVersion": 1,
              "id": "x",
              "title": "X",
              "homeLayerId": "main",
              "layers": [
                {
                  "id": "main",
                  "title": "ABC",
                  "icon": "Abc",
                  "rows": [
                    [
                      {
                        "type": "key",
                        "zones": {
                          "center": { "type": "switchLayer", "layerId": "symbols" }
                        }
                      }
                    ]
                  ]
                },
                {
                  "id": "symbols",
                  "title": "Symbols",
                  "icon": "Functions",
                  "rows": [
                    [
                      { "type": "key", "zones": { "center": { "type": "text", "value": "#" } } }
                    ]
                  ]
                }
              ]
            }
            """.trimIndent()
        val layout = decodeNamedLayout(json)
        assertEquals(2, layout.layers.size)
        assertNotNull(layout.layer("symbols"))
        val again = decodeNamedLayout(encodeNamedLayout(layout))
        assertEquals(layout.layers.map { it.id }, again.layers.map { it.id })
    }

    @Test
    fun `s12 asset has content strips and case maps`() {
        val s12 = loadS12Asset()
        assertEquals(ActiveLayer.MAIN, s12.homeLayerId)
        assertEquals(LayerContent.EmojiPicker, s12.requireLayer(ActiveLayer.EMOJI).content)
        assertEquals(5, s12.requireLayer(ActiveLayer.EMOJI).contentRows)
        assertEquals(LayerContent.ClipboardHistory, s12.requireLayer(ActiveLayer.CLIPBOARD).content)
        assertEquals(S12_SHIFT_MAPPINGS, s12.shiftMappings)
        assertEquals(S12_CAPS_LOCK_MAPPINGS, s12.capsLockMappings)
    }
    @Test
    fun `spacer and fractional columnSpan decode`() {
        val json =
            """
            {
              "schemaVersion": 1,
              "id": "pad",
              "title": "Pad",
              "homeLayerId": "main",
              "layers": [
                {
                  "id": "main",
                  "title": "ABC",
                  "icon": "Abc",
                  "rows": [
                    [
                      { "type": "spacer", "columnSpan": 0.5 },
                      {
                        "type": "key",
                        "columnSpan": 1.5,
                        "zones": { "center": { "type": "text", "value": "a" } }
                      },
                      { "type": "spacer", "columnSpan": 0.5 }
                    ]
                  ]
                }
              ]
            }
            """.trimIndent()
        val layout = decodeNamedLayout(json)
        val grid = layout.homeLayer().keyGrid
        assertEquals(3, grid.size)
        assertEquals(
            com.suave.keyboard.engine.intent.KeyFillRole.SPACER,
            grid.getValue(com.suave.keyboard.engine.intent.KeyPosition(0, 0)).fillRole,
        )
        assertEquals(0.5f, grid.getValue(com.suave.keyboard.engine.intent.KeyPosition(0, 0)).columnSpan)
        assertEquals(1.5f, grid.getValue(com.suave.keyboard.engine.intent.KeyPosition(0, 1)).columnSpan)
        val again = decodeNamedLayout(encodeNamedLayout(layout))
        assertEquals(
            com.suave.keyboard.engine.intent.KeyFillRole.SPACER,
            again.homeLayer().keyGrid.getValue(com.suave.keyboard.engine.intent.KeyPosition(0, 0)).fillRole,
        )
    }

    @Test
    fun `layout key heights round-trip and omit when unset`() {
        val withHeights =
            loadS12Asset().copy(keyHeight = 70, landscapeKeyHeight = 42)
        val encoded = encodeNamedLayout(withHeights)
        assertTrue(encoded.contains("\"keyHeight\": 70"))
        assertTrue(encoded.contains("\"landscapeKeyHeight\": 42"))
        val restored = decodeNamedLayout(encoded)
        assertEquals(70, restored.keyHeight)
        assertEquals(42, restored.landscapeKeyHeight)

        val without = encodeNamedLayout(loadS12Asset().copy(keyHeight = null, landscapeKeyHeight = null))
        assertTrue(!without.contains("\"keyHeight\""))
        assertTrue(!without.contains("\"landscapeKeyHeight\""))
    }

    @Test
    fun `landscape floating round-trips`() {
        val on = loadS12Asset().copy(landscapeFloating = true)
        val encoded = encodeNamedLayout(on)
        assertTrue(encoded.contains("\"landscapeFloating\": true"))
        assertTrue(decodeNamedLayout(encoded).landscapeFloating)
        val offJson = encodeNamedLayout(loadS12Asset().copy(landscapeFloating = false))
        assertTrue(!offJson.contains("landscapeFloating"))
        assertTrue(!decodeNamedLayout(offJson).landscapeFloating)
    }

    @Test
    fun `landscape floating by app round-trips and resolves`() {
        val layout =
            loadS12Asset().copy(
                landscapeFloating = true,
                landscapeFloatingByApp = mapOf("com.example.maps" to false),
            )
        assertTrue(layout.effectiveLandscapeFloating(null))
        assertTrue(layout.effectiveLandscapeFloating("com.other"))
        assertTrue(!layout.effectiveLandscapeFloating("com.example.maps"))
        val toggled = layout.withToggledLandscapeFloatingForApp("com.example.maps")
        assertTrue(toggled.effectiveLandscapeFloating("com.example.maps"))
        assertTrue(toggled.landscapeFloatingByApp.isEmpty())
        val encoded = encodeNamedLayout(layout)
        assertTrue(encoded.contains("landscapeFloatingByApp"))
        val restored = decodeNamedLayout(encoded)
        assertEquals(mapOf("com.example.maps" to false), restored.landscapeFloatingByApp)
    }

    @Test
    fun `new layout assets decode`() {
        for (name in listOf("simple.json", "terminal.json", "unexpected.json")) {
            val layout = loadLayoutAsset(name)
            assertTrue(layout.id.isNotBlank())
            assertTrue(layout.layers.isNotEmpty())
            assertNotNull(layout.homeLayer())
        }
    }

    @Test
    fun `tags round-trip and normalize`() {
        val tagged =
            loadS12Asset().copy(tags = listOf(" EN ", "thumbkey", "en"))
        val encoded = encodeNamedLayout(tagged)
        assertTrue(encoded.contains("\"tags\""))
        val restored = decodeNamedLayout(encoded)
        assertEquals(listOf("en", "thumbkey"), restored.tags)
        val fromAsset = loadS12Asset()
        assertTrue(fromAsset.tags.contains("suave"))
        val empty = encodeNamedLayout(loadS12Asset().copy(tags = emptyList()))
        assertTrue(!empty.contains("\"tags\""))
    }
}

internal fun loadS12Asset(): NamedLayout = loadLayoutAsset("s12.json")

internal fun loadLayoutAsset(fileName: String): NamedLayout {
    val candidates =
        listOf(
            File("app/src/main/assets/layouts/$fileName"),
            File("src/main/assets/layouts/$fileName"),
        )
    val file = candidates.firstOrNull { it.isFile }
        ?: error("$fileName not found (cwd=${File(".").absolutePath})")
    return decodeNamedLayout(file.readText())
}
