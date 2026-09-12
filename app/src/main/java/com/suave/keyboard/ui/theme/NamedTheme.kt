package com.suave.keyboard.ui.theme

import androidx.compose.material3.ColorScheme
import com.suave.keyboard.ui.theme.json.ThemeDocument
import com.suave.keyboard.ui.theme.json.colorSchemesToThemeDocument
import com.suave.keyboard.ui.theme.json.toColorSchemes

/**
 * A named light/dark palette Suave can apply. [DYNAMIC_ID] is not represented here;
 * Material You is resolved separately in [SuaveTheme].
 */
data class NamedTheme(
    val id: String,
    val title: String,
    val light: ColorScheme,
    val dark: ColorScheme,
    val builtin: Boolean = true,
) {
    val schemes: Pair<ColorScheme, ColorScheme> get() = Pair(light, dark)

    fun toDocument(): ThemeDocument = colorSchemesToThemeDocument(id, title, schemes)

    companion object {
        const val DYNAMIC_ID = "dynamic"
        const val DEFAULT_ID = "suave"

        fun fromDocument(
            document: ThemeDocument,
            builtin: Boolean,
        ): NamedTheme {
            val (light, dark) = document.toColorSchemes()
            return NamedTheme(
                id = document.id,
                title = document.title,
                light = light,
                dark = dark,
                builtin = builtin,
            )
        }
    }
}
