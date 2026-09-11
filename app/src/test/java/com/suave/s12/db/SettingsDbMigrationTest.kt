package com.suave.s12.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class SettingsDbMigrationTest {
    @Test
    fun `migrate copies sqlite and sidecars then deletes the source`() {
        val ceDir = createTempDirectory("ce").toFile()
        val deDir = createTempDirectory("de").toFile()
        val ce = File(ceDir, APP_SETTINGS_DB_NAME)
        ce.writeText("db")
        File(ceDir, "$APP_SETTINGS_DB_NAME-wal").writeText("wal")
        File(ceDir, "$APP_SETTINGS_DB_NAME-shm").writeText("shm")
        val de = File(deDir, APP_SETTINGS_DB_NAME)

        assertTrue(migrateSettingsDbToDeviceProtected(ce, de))
        assertEquals("db", de.readText())
        assertEquals("wal", File(deDir, "$APP_SETTINGS_DB_NAME-wal").readText())
        assertEquals("shm", File(deDir, "$APP_SETTINGS_DB_NAME-shm").readText())
        assertFalse(ce.exists())
        assertFalse(File(ceDir, "$APP_SETTINGS_DB_NAME-wal").exists())
    }

    @Test
    fun `migrate overwrites a device-protected stub created before first unlock`() {
        val ceDir = createTempDirectory("ce").toFile()
        val deDir = createTempDirectory("de").toFile()
        val ce = File(ceDir, APP_SETTINGS_DB_NAME)
        val de = File(deDir, APP_SETTINGS_DB_NAME)
        ce.writeText("real-settings")
        de.writeText("empty-stub")

        assertTrue(migrateSettingsDbToDeviceProtected(ce, de))
        assertEquals("real-settings", de.readText())
        assertFalse(ce.exists())
    }

    @Test
    fun `migrate is a no-op when credential db is missing`() {
        val ce = File(createTempDirectory("ce").toFile(), APP_SETTINGS_DB_NAME)
        val de = File(createTempDirectory("de").toFile(), APP_SETTINGS_DB_NAME)

        assertFalse(migrateSettingsDbToDeviceProtected(ce, de))
        assertFalse(de.exists())
    }
}
