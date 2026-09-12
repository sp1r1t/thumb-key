package com.suave.keyboard.ui.theme.json

/**
 * Migrates a parsed theme JSON tree from [fromVersion] toward [THEME_SCHEMA_VERSION].
 * Unknown versions must be rejected by the caller when migration throws.
 */
fun interface ThemeSchemaMigrator {
    fun migrate(
        document: ThemeDocument,
        fromVersion: Int,
    ): ThemeDocument
}

object DefaultThemeSchemaMigrator : ThemeSchemaMigrator {
    override fun migrate(
        document: ThemeDocument,
        fromVersion: Int,
    ): ThemeDocument =
        when (fromVersion) {
            THEME_SCHEMA_VERSION -> document
            1 -> migrateV1ToV2(document)
            else ->
                error("No migrator from schemaVersion $fromVersion to $THEME_SCHEMA_VERSION")
        }
}

/** v1 lacked error/success roles; fill from Suave defaults without overwriting existing keys. */
private fun migrateV1ToV2(document: ThemeDocument): ThemeDocument =
    document.copy(
        schemaVersion = THEME_SCHEMA_VERSION,
        light = DefaultLightSemanticRoles + document.light,
        dark = DefaultDarkSemanticRoles + document.dark,
    )

/** @deprecated Prefer [DefaultThemeSchemaMigrator]; kept for call sites that passed Identity. */
@Deprecated("Use DefaultThemeSchemaMigrator", ReplaceWith("DefaultThemeSchemaMigrator"))
object IdentityThemeSchemaMigrator : ThemeSchemaMigrator by DefaultThemeSchemaMigrator
