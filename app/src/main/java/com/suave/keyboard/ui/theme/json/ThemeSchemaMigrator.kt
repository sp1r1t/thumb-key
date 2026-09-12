package com.suave.keyboard.ui.theme.json

/**
 * Migrates a parsed theme JSON tree from [fromVersion] toward [THEME_SCHEMA_VERSION].
 * v1 has no transforms yet; unknown versions must be rejected by the caller before invoking.
 */
fun interface ThemeSchemaMigrator {
    fun migrate(
        document: ThemeDocument,
        fromVersion: Int,
    ): ThemeDocument
}

object IdentityThemeSchemaMigrator : ThemeSchemaMigrator {
    override fun migrate(
        document: ThemeDocument,
        fromVersion: Int,
    ): ThemeDocument {
        require(fromVersion == THEME_SCHEMA_VERSION) {
            "No migrator from schemaVersion $fromVersion to $THEME_SCHEMA_VERSION"
        }
        return document
    }
}
