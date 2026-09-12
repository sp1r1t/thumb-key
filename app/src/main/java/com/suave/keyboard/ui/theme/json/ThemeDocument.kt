package com.suave.keyboard.ui.theme.json

import kotlinx.serialization.Serializable

/** Current theme document schema version. Bump when making breaking JSON changes. */
const val THEME_SCHEMA_VERSION = 2

/**
 * Color roles the keyboard theme document must define for both light and dark.
 * Material error* map onto ColorScheme; success* are Suave [com.suave.keyboard.ui.theme.SemanticExtras].
 */
val THEME_COLOR_ROLES =
    listOf(
        "primary",
        "onPrimary",
        "secondary",
        "onSecondary",
        "tertiary",
        "onTertiary",
        "background",
        "onBackground",
        "surface",
        "onSurface",
        "surfaceVariant",
        "onSurfaceVariant",
        "outline",
        "inversePrimary",
        "tertiaryContainer",
        "onTertiaryContainer",
        "error",
        "onError",
        "errorContainer",
        "onErrorContainer",
        "success",
        "onSuccess",
        "successContainer",
        "onSuccessContainer",
    )

@Serializable
data class ThemeDocument(
    val schemaVersion: Int,
    val id: String,
    val title: String,
    val light: Map<String, String> = emptyMap(),
    val dark: Map<String, String> = emptyMap(),
)
