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
        assertEquals(original.schemaVersion, restored.schemaVersion)
        assertEquals(original.light, restored.light)
        assertEquals(original.dark, restored.dark)

        val (light, dark) = restored.toColorSchemes()
        for (role in THEME_COLOR_ROLES) {
            assertEquals(role, original.light[role], light.toRoleMap()[role])
            assertEquals(role, original.dark[role], dark.toRoleMap()[role])
        }
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
