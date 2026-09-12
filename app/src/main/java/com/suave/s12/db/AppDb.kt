package com.suave.s12.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import com.suave.s12.BuildConfig
import com.suave.s12.layout.DEFAULT_LAYER_HEIGHTS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

const val DEFAULT_AUTO_SIZE_KEYS = 1
const val DEFAULT_NON_SQUARE_KEYS = 0
const val DEFAULT_KEY_WIDTH = 64
const val DEFAULT_KEY_HEIGHT = DEFAULT_KEY_WIDTH
const val DEFAULT_ANIMATION_SPEED = 250
const val DEFAULT_ANIMATION_HELPER_SPEED = 250
const val DEFAULT_POSITION = 0
const val DEFAULT_POSITION_PADDING = 0
const val DEFAULT_AUTO_CAPITALIZE = 1
const val DEFAULT_KEYBOARD_LAYOUT = 0
const val DEFAULT_THEME = 0
const val DEFAULT_THEME_COLOR = 0
const val DEFAULT_VIBRATE_ON_TAP = 1
const val DEFAULT_VIBRATE_ON_SLIDE = 1
const val DEFAULT_SOUND_ON_TAP = 0
const val DEFAULT_MIN_SWIPE_LENGTH = 40
const val DEFAULT_PUSHUP_SIZE = 0
const val DEFAULT_HIDE_LETTERS = 0
const val DEFAULT_HIDE_SYMBOLS = 0
const val DEFAULT_HIDE_NUMBERS = 0
const val DEFAULT_HIDE_MODIFIERS = 0
const val DEFAULT_HIDE_LAYER_SWITCHES = 0
const val DEFAULT_HIDE_SPECIALS = 0
const val DEFAULT_HIDE_NAVIGATION = 0
const val DEFAULT_HIDE_EDITING = 0
const val DEFAULT_HIDE_KEY_CATEGORIES = "LETTER"
const val DEFAULT_KEY_BORDERS = 1
const val DEFAULT_SPACEBAR_MULTITAPS = 1
const val DEFAULT_SLIDE_SENSITIVITY = 9
const val DEFAULT_SLIDE_ENABLED = 0
const val DEFAULT_SLIDE_CURSOR_MOVEMENT_MODE = 0
const val DEFAULT_SLIDE_SPACEBAR_DEADZONE_ENABLED = 1
const val DEFAULT_SLIDE_BACKSPACE_DEADZONE_ENABLED = 1
const val DEFAULT_BACKDROP_ENABLED = 0
const val DEFAULT_KEY_PADDING = 2
const val DEFAULT_KEY_PADDING_VERTICAL = DEFAULT_KEY_PADDING
const val DEFAULT_KEY_BORDER_WIDTH = 1
const val DEFAULT_KEY_RADIUS = 0
const val DEFAULT_DRAG_RETURN_ENABLED = 1
const val DEFAULT_CIRCULAR_DRAG_ENABLED = 1
const val DEFAULT_CLOCKWISE_DRAG_ACTION = 0
const val DEFAULT_COUNTERCLOCKWISE_DRAG_ACTION = 1
const val DEFAULT_GHOST_KEYS_ENABLED = 0
const val DEFAULT_SLIDE_HOLD_ENABLED = 0
const val DEFAULT_KEY_MODIFICATIONS = ""
const val DEFAULT_IGNORE_BOTTOM_PADDING = 0
const val DEFAULT_SHOW_TOAST_ON_LAYOUT_SWITCH = 1
const val DEFAULT_DISABLE_FULLSCREEN_EDITOR = 0
const val DEFAULT_CLIPBOARD_HISTORY_ENABLED = 0
const val DEFAULT_CLIPBOARD_AUTO_CLEANUP_ENABLED = 1
const val DEFAULT_CLIPBOARD_CLEANUP_AFTER_MINUTES = 120
const val DEFAULT_CLIPBOARD_SIZE_LIMIT_ENABLED = 1
const val DEFAULT_CLIPBOARD_MAX_SIZE = 20
const val MIN_CLIPBOARD_MAX_SIZE = 2
const val MAX_CLIPBOARD_MAX_SIZE = 100
const val DEFAULT_USE_PRIVATE_CLIPBOARD = 0
const val DEFAULT_CAPTURE_SYSTEM_CLIPBOARD = 1
const val DEFAULT_SHOW_ON_SCREEN_KEYBOARD = 0
const val DEFAULT_SHOW_DEBUG_BAR = 1
const val DEFAULT_ANIMATION_PRESS_HIGHLIGHT = 1
const val DEFAULT_ANIMATION_RELEASE_FLASH = 1
const val DEFAULT_ANIMATION_LETTER_DROP = 1

// Default true (matches the modifier behavior this engine had before this setting existed): a
// quick Esc tap queues as a Meta-via-Escape combo prefix for the next key, and Esc+Esc (tapping
// again while that's still queued) sends a real Escape instead. False makes Esc a plain
// standalone key that always just sends a real Escape, with no combo behavior at all.
const val DEFAULT_ESC_AS_MODIFIER = 1
const val DEFAULT_CTRL_AS_MODIFIER = 1
const val DEFAULT_ALT_AS_MODIFIER = 1
const val DEFAULT_SHIFT_AS_MODIFIER = 1

@Entity
data class AppSettings(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(
        name = "theme",
        defaultValue = DEFAULT_THEME.toString(),
    )
    val theme: Int,
    @ColumnInfo(
        name = "theme_color",
        defaultValue = DEFAULT_THEME_COLOR.toString(),
    )
    val themeColor: Int,
    @ColumnInfo(
        name = "hide_letters",
        defaultValue = DEFAULT_HIDE_LETTERS.toString(),
    )
    val hideLetters: Int,
    @ColumnInfo(
        name = "hide_symbols",
        defaultValue = DEFAULT_HIDE_SYMBOLS.toString(),
    )
    val hideSymbols: Int = DEFAULT_HIDE_SYMBOLS,
    @ColumnInfo(
        name = "hide_numbers",
        defaultValue = DEFAULT_HIDE_NUMBERS.toString(),
    )
    val hideNumbers: Int = DEFAULT_HIDE_NUMBERS,
    @ColumnInfo(
        name = "hide_modifiers",
        defaultValue = DEFAULT_HIDE_MODIFIERS.toString(),
    )
    val hideModifiers: Int = DEFAULT_HIDE_MODIFIERS,
    @ColumnInfo(
        name = "hide_layer_switches",
        defaultValue = DEFAULT_HIDE_LAYER_SWITCHES.toString(),
    )
    val hideLayerSwitches: Int = DEFAULT_HIDE_LAYER_SWITCHES,
    @ColumnInfo(
        name = "hide_specials",
        defaultValue = DEFAULT_HIDE_SPECIALS.toString(),
    )
    val hideSpecials: Int = DEFAULT_HIDE_SPECIALS,
    @ColumnInfo(
        name = "hide_navigation",
        defaultValue = DEFAULT_HIDE_NAVIGATION.toString(),
    )
    val hideNavigation: Int = DEFAULT_HIDE_NAVIGATION,
    @ColumnInfo(
        name = "hide_editing",
        defaultValue = DEFAULT_HIDE_EDITING.toString(),
    )
    val hideEditing: Int = DEFAULT_HIDE_EDITING,
    @ColumnInfo(
        name = "hide_key_categories",
        defaultValue = DEFAULT_HIDE_KEY_CATEGORIES,
    )
    val hideKeyCategories: String = DEFAULT_HIDE_KEY_CATEGORIES,
    @ColumnInfo(
        name = "ignore_bottom_padding",
        defaultValue = DEFAULT_IGNORE_BOTTOM_PADDING.toString(),
    )
    val ignoreBottomPadding: Int,
    @ColumnInfo(
        name = "disable_fullscreen_editor",
        defaultValue = DEFAULT_DISABLE_FULLSCREEN_EDITOR.toString(),
    )
    val disableFullscreenEditor: Int,
    @ColumnInfo(
        name = "key_height",
        defaultValue = DEFAULT_KEY_HEIGHT.toString(),
    )
    val keyHeight: Int,
    @ColumnInfo(
        name = "layer_heights",
        defaultValue = DEFAULT_LAYER_HEIGHTS,
    )
    val layerHeights: String = DEFAULT_LAYER_HEIGHTS,
    @ColumnInfo(
        name = "vibrate_on_tap",
        defaultValue = DEFAULT_VIBRATE_ON_TAP.toString(),
    )
    val vibrateOnTap: Int,
    @ColumnInfo(
        name = "vibrate_on_slide",
        defaultValue = DEFAULT_VIBRATE_ON_SLIDE.toString(),
    )
    val vibrateOnSlide: Int,
    @ColumnInfo(
        name = "min_swipe_length",
        defaultValue = DEFAULT_MIN_SWIPE_LENGTH.toString(),
    )
    val minSwipeLength: Int,
    @ColumnInfo(
        name = "esc_as_modifier",
        defaultValue = DEFAULT_ESC_AS_MODIFIER.toString(),
    )
    val escAsModifier: Int,
    @ColumnInfo(
        name = "ctrl_as_modifier",
        defaultValue = DEFAULT_CTRL_AS_MODIFIER.toString(),
    )
    val ctrlAsModifier: Int = DEFAULT_CTRL_AS_MODIFIER,
    @ColumnInfo(
        name = "alt_as_modifier",
        defaultValue = DEFAULT_ALT_AS_MODIFIER.toString(),
    )
    val altAsModifier: Int = DEFAULT_ALT_AS_MODIFIER,
    @ColumnInfo(
        name = "shift_as_modifier",
        defaultValue = DEFAULT_SHIFT_AS_MODIFIER.toString(),
    )
    val shiftAsModifier: Int = DEFAULT_SHIFT_AS_MODIFIER,
    @ColumnInfo(
        name = "keyboard_layout",
        defaultValue = DEFAULT_KEYBOARD_LAYOUT.toString(),
    )
    val keyboardLayout: Int,
    @ColumnInfo(
        name = "keyboard_layouts",
        defaultValue = "$DEFAULT_KEYBOARD_LAYOUT",
    )
    val keyboardLayouts: String,
    @ColumnInfo(
        name = "show_toast_on_layout_switch",
        defaultValue = DEFAULT_SHOW_TOAST_ON_LAYOUT_SWITCH.toString(),
    )
    val showToastOnLayoutSwitch: Int,
    @ColumnInfo(
        name = "position",
        defaultValue = DEFAULT_POSITION.toString(),
    )
    val position: Int,
    @ColumnInfo(
        name = "last_version_code_viewed",
        defaultValue = "0",
    )
    val lastVersionCodeViewed: Int,
    @ColumnInfo(
        name = "clipboard_history_enabled",
        defaultValue = DEFAULT_CLIPBOARD_HISTORY_ENABLED.toString(),
    )
    val clipboardHistoryEnabled: Int,
    @ColumnInfo(
        name = "clipboard_auto_cleanup_enabled",
        defaultValue = DEFAULT_CLIPBOARD_AUTO_CLEANUP_ENABLED.toString(),
    )
    val clipboardAutoCleanupEnabled: Int,
    @ColumnInfo(
        name = "clipboard_cleanup_after_minutes",
        defaultValue = DEFAULT_CLIPBOARD_CLEANUP_AFTER_MINUTES.toString(),
    )
    val clipboardCleanupAfterMinutes: Int,
    @ColumnInfo(
        name = "clipboard_size_limit_enabled",
        defaultValue = DEFAULT_CLIPBOARD_SIZE_LIMIT_ENABLED.toString(),
    )
    val clipboardSizeLimitEnabled: Int,
    @ColumnInfo(
        name = "clipboard_max_size",
        defaultValue = DEFAULT_CLIPBOARD_MAX_SIZE.toString(),
    )
    val clipboardMaxSize: Int,
    @ColumnInfo(
        name = "use_private_clipboard",
        defaultValue = DEFAULT_USE_PRIVATE_CLIPBOARD.toString(),
    )
    val usePrivateClipboard: Int,
    @ColumnInfo(
        name = "capture_system_clipboard",
        defaultValue = DEFAULT_CAPTURE_SYSTEM_CLIPBOARD.toString(),
    )
    val captureSystemClipboard: Int = DEFAULT_CAPTURE_SYSTEM_CLIPBOARD,
    @ColumnInfo(
        name = "show_on_screen_keyboard",
        defaultValue = DEFAULT_SHOW_ON_SCREEN_KEYBOARD.toString(),
    )
    val showOnScreenKeyboard: Int,
    @ColumnInfo(
        name = "show_debug_bar",
        defaultValue = DEFAULT_SHOW_DEBUG_BAR.toString(),
    )
    val showDebugBar: Int = DEFAULT_SHOW_DEBUG_BAR,
    @ColumnInfo(
        name = "backdrop_enabled",
        defaultValue = DEFAULT_BACKDROP_ENABLED.toString(),
    )
    val backdropEnabled: Int = DEFAULT_BACKDROP_ENABLED,
    @ColumnInfo(
        name = "key_padding",
        defaultValue = DEFAULT_KEY_PADDING.toString(),
    )
    val keyPadding: Int = DEFAULT_KEY_PADDING,
    @ColumnInfo(
        name = "key_padding_vertical",
        defaultValue = DEFAULT_KEY_PADDING_VERTICAL.toString(),
    )
    val keyPaddingVertical: Int = DEFAULT_KEY_PADDING_VERTICAL,
    @ColumnInfo(
        name = "key_border_width",
        defaultValue = DEFAULT_KEY_BORDER_WIDTH.toString(),
    )
    val keyBorderWidth: Int = DEFAULT_KEY_BORDER_WIDTH,
    @ColumnInfo(
        name = "key_radius",
        defaultValue = DEFAULT_KEY_RADIUS.toString(),
    )
    val keyRadius: Int = DEFAULT_KEY_RADIUS,
    @ColumnInfo(
        name = "pushup_size",
        defaultValue = DEFAULT_PUSHUP_SIZE.toString(),
    )
    val pushupSize: Int = DEFAULT_PUSHUP_SIZE,
    @ColumnInfo(
        name = "animation_press_highlight",
        defaultValue = DEFAULT_ANIMATION_PRESS_HIGHLIGHT.toString(),
    )
    val animationPressHighlight: Int = DEFAULT_ANIMATION_PRESS_HIGHLIGHT,
    @ColumnInfo(
        name = "animation_release_flash",
        defaultValue = DEFAULT_ANIMATION_RELEASE_FLASH.toString(),
    )
    val animationReleaseFlash: Int = DEFAULT_ANIMATION_RELEASE_FLASH,
    @ColumnInfo(
        name = "animation_letter_drop",
        defaultValue = DEFAULT_ANIMATION_LETTER_DROP.toString(),
    )
    val animationLetterDrop: Int = DEFAULT_ANIMATION_LETTER_DROP,
)

data class LayoutsUpdate(
    val id: Int,
    @ColumnInfo(
        name = "keyboard_layout",
    )
    val keyboardLayout: Int,
    @ColumnInfo(
        name = "keyboard_layouts",
    )
    val keyboardLayouts: String,
)

data class LookAndFeelUpdate(
    val id: Int,
    @ColumnInfo(name = "theme")
    val theme: Int,
    @ColumnInfo(name = "theme_color")
    val themeColor: Int,
    @ColumnInfo(name = "hide_letters")
    val hideLetters: Int,
    @ColumnInfo(name = "hide_symbols")
    val hideSymbols: Int,
    @ColumnInfo(name = "hide_numbers")
    val hideNumbers: Int,
    @ColumnInfo(name = "hide_modifiers")
    val hideModifiers: Int,
    @ColumnInfo(name = "hide_layer_switches")
    val hideLayerSwitches: Int,
    @ColumnInfo(name = "hide_specials")
    val hideSpecials: Int,
    @ColumnInfo(name = "hide_navigation")
    val hideNavigation: Int,
    @ColumnInfo(name = "hide_editing")
    val hideEditing: Int,
    @ColumnInfo(name = "hide_key_categories")
    val hideKeyCategories: String,
    @ColumnInfo(name = "ignore_bottom_padding")
    val ignoreBottomPadding: Int,
    @ColumnInfo(name = "disable_fullscreen_editor")
    val disableFullscreenEditor: Int,
    @ColumnInfo(name = "key_height")
    val keyHeight: Int,
    @ColumnInfo(name = "layer_heights")
    val layerHeights: String,
    @ColumnInfo(name = "vibrate_on_tap")
    val vibrateOnTap: Int,
    @ColumnInfo(name = "vibrate_on_slide")
    val vibrateOnSlide: Int,
    @ColumnInfo(name = "backdrop_enabled")
    val backdropEnabled: Int,
    @ColumnInfo(name = "key_padding")
    val keyPadding: Int,
    @ColumnInfo(name = "key_padding_vertical")
    val keyPaddingVertical: Int,
    @ColumnInfo(name = "key_border_width")
    val keyBorderWidth: Int,
    @ColumnInfo(name = "key_radius")
    val keyRadius: Int,
    @ColumnInfo(name = "pushup_size")
    val pushupSize: Int,
    @ColumnInfo(name = "animation_press_highlight")
    val animationPressHighlight: Int,
    @ColumnInfo(name = "animation_release_flash")
    val animationReleaseFlash: Int,
    @ColumnInfo(name = "animation_letter_drop")
    val animationLetterDrop: Int,
)

data class BehaviorUpdate(
    val id: Int,
    @ColumnInfo(name = "min_swipe_length")
    val minSwipeLength: Int,
    @ColumnInfo(name = "esc_as_modifier")
    val escAsModifier: Int,
    @ColumnInfo(name = "ctrl_as_modifier")
    val ctrlAsModifier: Int,
    @ColumnInfo(name = "alt_as_modifier")
    val altAsModifier: Int,
    @ColumnInfo(name = "shift_as_modifier")
    val shiftAsModifier: Int,
)

data class ClipboardSettingsUpdate(
    val id: Int,
    @ColumnInfo(name = "clipboard_history_enabled")
    val clipboardHistoryEnabled: Int,
    @ColumnInfo(name = "clipboard_auto_cleanup_enabled")
    val clipboardAutoCleanupEnabled: Int,
    @ColumnInfo(name = "clipboard_cleanup_after_minutes")
    val clipboardCleanupAfterMinutes: Int,
    @ColumnInfo(name = "clipboard_size_limit_enabled")
    val clipboardSizeLimitEnabled: Int,
    @ColumnInfo(name = "clipboard_max_size")
    val clipboardMaxSize: Int,
    @ColumnInfo(
        name = "use_private_clipboard",
    )
    val usePrivateClipboard: Int,
    @ColumnInfo(name = "capture_system_clipboard")
    val captureSystemClipboard: Int,
)

data class OtherSettingsUpdate(
    val id: Int,
    @ColumnInfo(name = "show_on_screen_keyboard")
    val showOnScreenKeyboard: Int,
    @ColumnInfo(name = "show_debug_bar")
    val showDebugBar: Int,
)

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM AppSettings limit 1")
    fun getSettings(): LiveData<AppSettings>

    @Query("SELECT * FROM AppSettings limit 1")
    fun getSettingsSync(): AppSettings?

    @Update
    suspend fun updateAppSettings(appSettings: AppSettings)

    @Update(entity = AppSettings::class)
    fun updateLayouts(layouts: LayoutsUpdate)

    @Update(entity = AppSettings::class)
    fun updateLookAndFeel(lookAndFeel: LookAndFeelUpdate)

    @Update(entity = AppSettings::class)
    fun updateBehavior(behavior: BehaviorUpdate)

    @Update(entity = AppSettings::class)
    fun updateClipboardSettings(clipboardSettings: ClipboardSettingsUpdate)

    @Update(entity = AppSettings::class)
    fun updateOtherSettings(otherSettings: OtherSettingsUpdate)

    @Query("UPDATE AppSettings SET last_version_code_viewed = :versionCode")
    suspend fun updateLastVersionCode(versionCode: Int)
}

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class AppSettingsRepository(
    private val appSettingsDao: AppSettingsDao,
) {
    private val _changelog = MutableStateFlow("")
    val changelog = _changelog.asStateFlow()

    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    val appSettings = appSettingsDao.getSettings()

    @WorkerThread
    suspend fun update(appSettings: AppSettings) {
        appSettingsDao.updateAppSettings(appSettings)
    }

    @WorkerThread
    fun updateLayouts(layouts: LayoutsUpdate) {
        appSettingsDao.updateLayouts(layouts)
    }

    @WorkerThread
    fun updateLookAndFeel(lookAndFeel: LookAndFeelUpdate) {
        appSettingsDao.updateLookAndFeel(lookAndFeel)
    }

    @WorkerThread
    fun updateBehavior(behavior: BehaviorUpdate) {
        appSettingsDao.updateBehavior(behavior)
    }

    @WorkerThread
    fun updateClipboardSettings(clipboardSettings: ClipboardSettingsUpdate) {
        appSettingsDao.updateClipboardSettings(clipboardSettings)
    }

    @WorkerThread
    fun updateOtherSettings(otherSettings: OtherSettingsUpdate) {
        appSettingsDao.updateOtherSettings(otherSettings)
    }

    @WorkerThread
    suspend fun updateLastVersionCodeViewed(versionCode: Int) {
        appSettingsDao.updateLastVersionCode(versionCode)
    }

    @WorkerThread
    suspend fun updateChangelog(ctx: Context) {
        withContext(Dispatchers.IO) {
            try {
                val releasesStr =
                    ctx.assets
                        .open("RELEASES.md")
                        .bufferedReader()
                        .use { it.readText() }
                _changelog.value = releasesStr
            } catch (e: Exception) {
                Log.e("thumb-key", "Failed to load changelog: $e")
            }
        }
    }
}

@Database(
    version = 39,
    entities = [AppSettings::class],
    exportSchema = true,
)
abstract class AppDB : RoomDatabase() {
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var instance: AppDB? = null

        fun getDatabase(context: Context): AppDB {
            val appContext = context.applicationContext
            val deviceProtected = appContext.createDeviceProtectedStorageContext()
            return synchronized(this) {
                if (isCredentialStorageUnlocked(appContext)) {
                    renameLegacySettingsDb(
                        appContext.getDatabasePath(LEGACY_APP_SETTINGS_DB_NAME),
                        appContext.getDatabasePath(APP_SETTINGS_DB_NAME),
                    )
                }
                renameLegacySettingsDb(
                    deviceProtected.getDatabasePath(LEGACY_APP_SETTINGS_DB_NAME),
                    deviceProtected.getDatabasePath(APP_SETTINGS_DB_NAME),
                )
                if (isCredentialStorageUnlocked(appContext) &&
                    migrateSettingsDbToDeviceProtected(
                        appContext.getDatabasePath(APP_SETTINGS_DB_NAME),
                        deviceProtected.getDatabasePath(APP_SETTINGS_DB_NAME),
                    )
                ) {
                    instance?.close()
                    instance = null
                }
                instance ?: buildDatabase(deviceProtected).also { instance = it }
            }
        }

        private fun buildDatabase(deviceProtected: Context): AppDB =
            Room
                        .databaseBuilder(
                            deviceProtected,
                            AppDB::class.java,
                            APP_SETTINGS_DB_NAME,
                        ).allowMainThreadQueries()
                        .addMigrations(
                            MIGRATION_1_2,
                            MIGRATION_2_3,
                            MIGRATION_3_4,
                            MIGRATION_4_5,
                            MIGRATION_5_6,
                            MIGRATION_6_7,
                            MIGRATION_7_8,
                            MIGRATION_8_9,
                            MIGRATION_9_10,
                            MIGRATION_10_11,
                            MIGRATION_11_12,
                            MIGRATION_12_13,
                            MIGRATION_13_14,
                            MIGRATION_14_15,
                            MIGRATION_15_16,
                            MIGRATION_16_17,
                            MIGRATION_17_18,
                            MIGRATION_18_19,
                            MIGRATION_19_20,
                            MIGRATION_20_21,
                            MIGRATION_21_22,
                            MIGRATION_22_23,
                            MIGRATION_23_24,
                            MIGRATION_24_25,
                            MIGRATION_25_26,
                            MIGRATION_26_27,
                            MIGRATION_27_28,
                            MIGRATION_28_29,
                            MIGRATION_29_30,
                            MIGRATION_30_31,
                            MIGRATION_31_32,
                            MIGRATION_32_33,
                            MIGRATION_33_34,
                            MIGRATION_34_35,
                            MIGRATION_35_36,
                            MIGRATION_36_37,
                            MIGRATION_37_38,
                            MIGRATION_38_39,
                        )
                        // Necessary because it can't insert data on creation
                        .addCallback(
                            object : Callback() {
                                override fun onOpen(db: SupportSQLiteDatabase) {
                                    super.onCreate(db)
                                    Executors.newSingleThreadExecutor().execute {
                                        db.insert(
                                            "AppSettings",
                                            // Ensures it won't overwrite the existing data
                                            CONFLICT_IGNORE,
                                            ContentValues(2).apply {
                                                put("id", 1)
                                                put(
                                                    "show_on_screen_keyboard",
                                                    if (BuildConfig.DEBUG) 1 else DEFAULT_SHOW_ON_SCREEN_KEYBOARD,
                                                )
                                            },
                                        )
                                    }
                                }
                            },
                        ).build()
    }
}

class AppSettingsViewModel(
    private val repository: AppSettingsRepository,
) : ViewModel() {
    val appSettings = repository.appSettings
    val changelog = repository.changelog

    fun update(appSettings: AppSettings) =
        viewModelScope.launch {
            repository.update(appSettings)
        }

    fun updateLayouts(layouts: LayoutsUpdate) =
        viewModelScope.launch {
            repository.updateLayouts(layouts)
        }

    fun updateLookAndFeel(lookAndFeel: LookAndFeelUpdate) =
        viewModelScope.launch {
            repository.updateLookAndFeel(lookAndFeel)
        }

    fun updateBehavior(behavior: BehaviorUpdate) =
        viewModelScope.launch {
            repository.updateBehavior(behavior)
        }

    fun updateClipboardSettings(clipboardSettings: ClipboardSettingsUpdate) =
        viewModelScope.launch {
            repository.updateClipboardSettings(clipboardSettings)
        }

    fun updateOtherSettings(otherSettings: OtherSettingsUpdate) =
        viewModelScope.launch {
            repository.updateOtherSettings(otherSettings)
        }

    fun updateLastVersionCodeViewed(versionCode: Int) =
        viewModelScope.launch {
            repository.updateLastVersionCodeViewed(versionCode)
        }

    fun updateChangelog(ctx: Context) =
        viewModelScope.launch {
            repository.updateChangelog(ctx)
        }
}

class AppSettingsViewModelFactory(
    private val repository: AppSettingsRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppSettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
