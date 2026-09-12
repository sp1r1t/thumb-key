package com.suave.keyboard.ui.theme.json

import com.suave.keyboard.ui.theme.suave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ThemeCodecTest {
    @Test
    fun `suave round-trips through JSON`() {
        val original = colorSchemesToThemeDocument("suave", "Suave", suave())
        val json = encodeThemeDocument(original)
        val restored = parseThemeDocument(json)

        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(THEME_SCHEMA_VERSION, restored.schemaVersion)
        assertEquals(original.light, restored.light)
        assertEquals(original.dark, restored.dark)

        for (role in THEME_COLOR_ROLES) {
            assertTrue(role, role in restored.light)
            assertTrue(role, role in restored.dark)
        }
    }

    @Test
    fun `v1 themes gain error and success roles on load`() {
        val v1Roles =
            mapOf(
                "primary" to "#FF1B1B1B",
                "onPrimary" to "#FFF4F4F4",
                "secondary" to "#FF5C5C5C",
                "onSecondary" to "#FFF4F4F4",
                "tertiary" to "#FF111111",
                "onTertiary" to "#FFF4F4F4",
                "background" to "#FFE6E6E6",
                "onBackground" to "#FF1B1B1B",
                "surface" to "#FFF3F3F3",
                "onSurface" to "#FF1B1B1B",
                "surfaceVariant" to "#FFE0E0E0",
                "onSurfaceVariant" to "#FF4A4A4A",
                "outline" to "#FFB5B5B5",
                "inversePrimary" to "#FFCFCFCF",
                "tertiaryContainer" to "#FFD2D2D2",
                "onTertiaryContainer" to "#FF1B1B1B",
            )
        val json =
            ThemeJsonFormat.encodeToString(
                ThemeDocument(
                    schemaVersion = 1,
                    id = "legacy",
                    title = "Legacy",
                    light = v1Roles,
                    dark = v1Roles,
                ),
            )
        val restored = parseThemeDocument(json)
        assertEquals(THEME_SCHEMA_VERSION, restored.schemaVersion)
        assertEquals("#FFB33B3B", restored.light["error"])
        assertEquals("#FF2E7D32", restored.light["success"])
        assertEquals(v1Roles["primary"], restored.light["primary"])
    }

    @Test
    fun `unknown schema version is rejected`() {
        val json =
            """
            {
              "schemaVersion": 99,
              "id": "x",
              "title": "X",
              "light": {},
              "dark": {}
            }
            """.trimIndent()
        try {
            parseThemeDocument(json)
            throw AssertionError("expected ThemeJsonException")
        } catch (e: ThemeJsonException) {
            assertTrue(e.message!!.contains("schemaVersion"))
        }
    }

    @Test
    fun `unknown color role is rejected`() {
        val base = colorSchemesToThemeDocument("suave", "Suave", suave())
        val bad =
            base.copy(
                light = base.light + ("notARole" to "#FFFFFFFF"),
            )
        try {
            encodeThemeDocument(bad)
            throw AssertionError("expected ThemeJsonException")
        } catch (e: ThemeJsonException) {
            assertTrue(e.message!!.contains("Unknown"))
        }
    }

    @Test
    fun `export suave asset when EXPORT_SUAVE_THEME is set`() {
        val out = System.getenv("EXPORT_SUAVE_THEME") ?: return
        File(out).parentFile?.mkdirs()
        File(out).writeText(encodeThemeDocument(colorSchemesToThemeDocument("suave", "Suave", suave())))
    }
}
