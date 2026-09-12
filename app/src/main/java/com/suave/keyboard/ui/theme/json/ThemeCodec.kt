package com.suave.keyboard.ui.theme.json

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.suave.keyboard.ui.theme.SemanticExtras
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val ThemeJsonFormat =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = true
    }

class ThemeJsonException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

fun parseThemeDocument(
    json: String,
    migrator: ThemeSchemaMigrator = DefaultThemeSchemaMigrator,
): ThemeDocument {
    val raw =
        try {
            ThemeJsonFormat.decodeFromString(ThemeDocument.serializer(), json)
        } catch (e: Exception) {
            throw ThemeJsonException("Invalid theme JSON: ${e.message}", e)
        }
    val document =
        if (raw.schemaVersion == THEME_SCHEMA_VERSION) {
            raw
        } else {
            try {
                migrator.migrate(raw, raw.schemaVersion).copy(schemaVersion = THEME_SCHEMA_VERSION)
            } catch (e: Exception) {
                throw ThemeJsonException(
                    "Unsupported theme schemaVersion ${raw.schemaVersion} (current is $THEME_SCHEMA_VERSION)",
                    e,
                )
            }
        }
    validateThemeDocument(document)
    return document
}

fun encodeThemeDocument(document: ThemeDocument): String {
    validateThemeDocument(document)
    return ThemeJsonFormat.encodeToString(document)
}

fun validateThemeDocument(document: ThemeDocument) {
    if (document.id.isBlank()) throw ThemeJsonException("Theme id must not be blank")
    if (document.id == "dynamic") {
        throw ThemeJsonException("Theme id \"dynamic\" is reserved for Material You")
    }
    if (document.title.isBlank()) throw ThemeJsonException("Theme title must not be blank")
    validateRoleMap(document.light, "light")
    validateRoleMap(document.dark, "dark")
}

private fun validateRoleMap(
    map: Map<String, String>,
    label: String,
) {
    val unknown = map.keys - THEME_COLOR_ROLES.toSet()
    if (unknown.isNotEmpty()) {
        throw ThemeJsonException("Unknown $label color roles: ${unknown.sorted().joinToString()}")
    }
    val missing = THEME_COLOR_ROLES.filter { it !in map }
    if (missing.isNotEmpty()) {
        throw ThemeJsonException("Missing $label color roles: ${missing.joinToString()}")
    }
    for (role in THEME_COLOR_ROLES) {
        parseArgbHex(map.getValue(role), "$label.$role")
    }
}

fun ThemeDocument.toColorSchemes(): Pair<ColorScheme, ColorScheme> =
    Pair(light.toLightColorScheme(), dark.toDarkColorScheme())

fun ThemeDocument.toSemanticExtras(): Pair<SemanticExtras, SemanticExtras> =
    Pair(SemanticExtras.fromRoleMap(light), SemanticExtras.fromRoleMap(dark))

fun colorSchemesToThemeDocument(
    id: String,
    title: String,
    schemes: Pair<ColorScheme, ColorScheme>,
    lightExtras: SemanticExtras = SemanticExtras.SoftLight,
    darkExtras: SemanticExtras = SemanticExtras.SoftDark,
): ThemeDocument =
    ThemeDocument(
        schemaVersion = THEME_SCHEMA_VERSION,
        id = id,
        title = title,
        light = schemes.first.toRoleMap() + lightExtras.toRoleMap(),
        dark = schemes.second.toRoleMap() + darkExtras.toRoleMap(),
    )

fun ColorScheme.toRoleMap(): Map<String, String> =
    linkedMapOf(
        "primary" to primary.toArgbHex(),
        "onPrimary" to onPrimary.toArgbHex(),
        "secondary" to secondary.toArgbHex(),
        "onSecondary" to onSecondary.toArgbHex(),
        "tertiary" to tertiary.toArgbHex(),
        "onTertiary" to onTertiary.toArgbHex(),
        "background" to background.toArgbHex(),
        "onBackground" to onBackground.toArgbHex(),
        "surface" to surface.toArgbHex(),
        "onSurface" to onSurface.toArgbHex(),
        "surfaceVariant" to surfaceVariant.toArgbHex(),
        "onSurfaceVariant" to onSurfaceVariant.toArgbHex(),
        "outline" to outline.toArgbHex(),
        "inversePrimary" to inversePrimary.toArgbHex(),
        "tertiaryContainer" to tertiaryContainer.toArgbHex(),
        "onTertiaryContainer" to onTertiaryContainer.toArgbHex(),
        "error" to error.toArgbHex(),
        "onError" to onError.toArgbHex(),
        "errorContainer" to errorContainer.toArgbHex(),
        "onErrorContainer" to onErrorContainer.toArgbHex(),
    )

private fun Map<String, String>.toLightColorScheme(): ColorScheme {
    val roles = toColorRoles()
    return lightColorScheme(
        primary = roles.primary,
        onPrimary = roles.onPrimary,
        secondary = roles.secondary,
        onSecondary = roles.onSecondary,
        tertiary = roles.tertiary,
        onTertiary = roles.onTertiary,
        background = roles.background,
        onBackground = roles.onBackground,
        surface = roles.surface,
        onSurface = roles.onSurface,
        surfaceVariant = roles.surfaceVariant,
        onSurfaceVariant = roles.onSurfaceVariant,
        outline = roles.outline,
        inversePrimary = roles.inversePrimary,
        tertiaryContainer = roles.tertiaryContainer,
        onTertiaryContainer = roles.onTertiaryContainer,
        error = roles.error,
        onError = roles.onError,
        errorContainer = roles.errorContainer,
        onErrorContainer = roles.onErrorContainer,
        surfaceTint = roles.primary,
    )
}

private fun Map<String, String>.toDarkColorScheme(): ColorScheme {
    val roles = toColorRoles()
    return darkColorScheme(
        primary = roles.primary,
        onPrimary = roles.onPrimary,
        secondary = roles.secondary,
        onSecondary = roles.onSecondary,
        tertiary = roles.tertiary,
        onTertiary = roles.onTertiary,
        background = roles.background,
        onBackground = roles.onBackground,
        surface = roles.surface,
        onSurface = roles.onSurface,
        surfaceVariant = roles.surfaceVariant,
        onSurfaceVariant = roles.onSurfaceVariant,
        outline = roles.outline,
        inversePrimary = roles.inversePrimary,
        tertiaryContainer = roles.tertiaryContainer,
        onTertiaryContainer = roles.onTertiaryContainer,
        error = roles.error,
        onError = roles.onError,
        errorContainer = roles.errorContainer,
        onErrorContainer = roles.onErrorContainer,
        surfaceTint = roles.primary,
    )
}

private data class ColorRoles(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val inversePrimary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
)

private fun Map<String, String>.toColorRoles(): ColorRoles =
    ColorRoles(
        primary = parseArgbHex(getValue("primary")),
        onPrimary = parseArgbHex(getValue("onPrimary")),
        secondary = parseArgbHex(getValue("secondary")),
        onSecondary = parseArgbHex(getValue("onSecondary")),
        tertiary = parseArgbHex(getValue("tertiary")),
        onTertiary = parseArgbHex(getValue("onTertiary")),
        background = parseArgbHex(getValue("background")),
        onBackground = parseArgbHex(getValue("onBackground")),
        surface = parseArgbHex(getValue("surface")),
        onSurface = parseArgbHex(getValue("onSurface")),
        surfaceVariant = parseArgbHex(getValue("surfaceVariant")),
        onSurfaceVariant = parseArgbHex(getValue("onSurfaceVariant")),
        outline = parseArgbHex(getValue("outline")),
        inversePrimary = parseArgbHex(getValue("inversePrimary")),
        tertiaryContainer = parseArgbHex(getValue("tertiaryContainer")),
        onTertiaryContainer = parseArgbHex(getValue("onTertiaryContainer")),
        error = parseArgbHex(getValue("error")),
        onError = parseArgbHex(getValue("onError")),
        errorContainer = parseArgbHex(getValue("errorContainer")),
        onErrorContainer = parseArgbHex(getValue("onErrorContainer")),
    )

fun Color.toArgbHex(): String = String.format("#%08X", toArgb())

fun parseArgbHex(
    value: String,
    label: String = "color",
): Color {
    val raw = value.trim().removePrefix("#")
    val argb =
        when (raw.length) {
            6 -> ("FF$raw").toLongOrNull(16)
            8 -> raw.toLongOrNull(16)
            else -> null
        } ?: throw ThemeJsonException("Invalid $label hex: $value")
    return Color(argb.toInt())
}

/** Soft defaults used when migrating v1 themes that lacked error/success roles. */
internal val DefaultLightSemanticRoles: Map<String, String> =
    linkedMapOf(
        "error" to "#FFB33B3B",
        "onError" to "#FFFFFFFF",
        "errorContainer" to "#FFF5D6D6",
        "onErrorContainer" to "#FF3F1010",
    ) + SemanticExtras.SoftLight.toRoleMap()

internal val DefaultDarkSemanticRoles: Map<String, String> =
    linkedMapOf(
        "error" to "#FFFFB4AB",
        "onError" to "#FF690005",
        "errorContainer" to "#FF93000A",
        "onErrorContainer" to "#FFFFDAD6",
    ) + SemanticExtras.SoftDark.toRoleMap()
