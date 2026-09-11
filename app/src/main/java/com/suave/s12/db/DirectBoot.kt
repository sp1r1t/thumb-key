package com.suave.s12.db

import android.content.Context
import android.os.UserManager
import java.io.File

const val APP_SETTINGS_DB_NAME = "suave"
const val LEGACY_APP_SETTINGS_DB_NAME = "thumbkey"

private val SQLITE_SIDECARS = arrayOf("", "-wal", "-shm", "-journal")

fun isCredentialStorageUnlocked(context: Context): Boolean =
    context.getSystemService(UserManager::class.java)?.isUserUnlocked ?: true

/**
 * Settings have to live in device-protected storage so the IME can read them on the lock
 * screen after a reboot (Direct Boot). Clipboard history stays in credential-encrypted
 * storage. Returns true when a CE copy was moved onto DE, so Room must be closed and reopened.
 */
fun migrateSettingsDbToDeviceProtected(
    credentialDb: File,
    deviceProtectedDb: File,
): Boolean = moveDatabaseFiles(credentialDb, deviceProtectedDb)

/**
 * Move a leftover Thumb-Key-era `thumbkey` file onto `suave`. If `suave` is already there,
 * delete the leftover so the two names cannot drift apart.
 */
fun renameLegacySettingsDb(
    legacyDb: File,
    currentDb: File,
): Boolean {
    if (currentDb.exists()) {
        if (legacyDb.exists()) deleteDatabaseFiles(legacyDb)
        return false
    }
    return moveDatabaseFiles(legacyDb, currentDb)
}

internal fun moveDatabaseFiles(
    from: File,
    to: File,
): Boolean {
    if (!from.exists()) return false
    copyDatabaseFiles(from, to)
    deleteDatabaseFiles(from)
    return true
}

internal fun copyDatabaseFiles(
    from: File,
    to: File,
) {
    to.parentFile?.mkdirs()
    for (suffix in SQLITE_SIDECARS) {
        val src = File(from.path + suffix)
        if (!src.exists()) continue
        src.copyTo(File(to.path + suffix), overwrite = true)
    }
}

internal fun deleteDatabaseFiles(file: File) {
    for (suffix in SQLITE_SIDECARS) {
        File(file.path + suffix).delete()
    }
}
