package com.suave.keyboard.layout.json

/**
 * Migrates a parsed layout JSON tree from [fromVersion] toward [LAYOUT_SCHEMA_VERSION].
 * v1 has no transforms yet; unknown versions must be rejected by the caller before invoking.
 */
fun interface LayoutSchemaMigrator {
    fun migrate(
        document: LayoutDocument,
        fromVersion: Int,
    ): LayoutDocument
}

object IdentityLayoutSchemaMigrator : LayoutSchemaMigrator {
    override fun migrate(
        document: LayoutDocument,
        fromVersion: Int,
    ): LayoutDocument {
        require(fromVersion == LAYOUT_SCHEMA_VERSION) {
            "No migrator from schemaVersion $fromVersion to $LAYOUT_SCHEMA_VERSION"
        }
        return document
    }
}
