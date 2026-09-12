package com.suave.s12.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column min_swipe_length INTEGER NOT NULL default $DEFAULT_MIN_SWIPE_LENGTH",
            )
        }
    }

val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column pushup_size INTEGER NOT NULL default $DEFAULT_PUSHUP_SIZE",
            )
        }
    }

val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column hide_letters INTEGER NOT NULL default $DEFAULT_HIDE_LETTERS",
            )
        }
    }

val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column keyboard_layouts TEXT NOT NULL default '$DEFAULT_KEYBOARD_LAYOUT'",
            )
        }
    }

val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column key_borders INTEGER NOT NULL default $DEFAULT_KEY_BORDERS",
            )
        }
    }

val MIGRATION_6_7 =
    object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column spacebar_multitaps INTEGER NOT NULL default $DEFAULT_SPACEBAR_MULTITAPS",
            )
        }
    }

val MIGRATION_7_8 =
    object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column hide_symbols INTEGER NOT NULL default $DEFAULT_HIDE_SYMBOLS",
            )
        }
    }

val MIGRATION_8_9 =
    object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column last_version_code_viewed INTEGER NOT NULL default 0",
            )
        }
    }

val MIGRATION_9_10 =
    object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column slide_enabled INTEGER NOT NULL default $DEFAULT_SLIDE_ENABLED",
            )
            db.execSQL(
                "alter table AppSettings add column slide_sensitivity INTEGER NOT NULL default $DEFAULT_SLIDE_SENSITIVITY",
            )
        }
    }

val MIGRATION_10_11 =
    object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column backdrop_enabled INTEGER NOT NULL default $DEFAULT_BACKDROP_ENABLED",
            )
        }
    }

val MIGRATION_11_12 =
    object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column key_padding INTEGER NOT NULL default $DEFAULT_KEY_PADDING",
            )
            db.execSQL(
                "alter table AppSettings add column key_border_width INTEGER NOT NULL default $DEFAULT_KEY_BORDER_WIDTH",
            )
            db.execSQL(
                "alter table AppSettings add column key_radius INTEGER NOT NULL default $DEFAULT_KEY_RADIUS",
            )
        }
    }

val MIGRATION_12_13 =
    object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column slide_spacebar_deadzone_enabled INTEGER NOT NULL " +
                    "default $DEFAULT_SLIDE_SPACEBAR_DEADZONE_ENABLED",
            )
            db.execSQL(
                "alter table AppSettings add column slide_backspace_deadzone_enabled INTEGER NOT NULL " +
                    "default $DEFAULT_SLIDE_BACKSPACE_DEADZONE_ENABLED",
            )
            db.execSQL(
                "alter table AppSettings add column slide_cursor_movement_mode INTEGER NOT NULL " +
                    "default $DEFAULT_SLIDE_CURSOR_MOVEMENT_MODE",
            )
        }
    }

val MIGRATION_13_14 =
    object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column key_width INTEGER",
            )
        }
    }

val MIGRATION_14_15 =
    object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column drag_return_enabled INTEGER NOT NULL default $DEFAULT_DRAG_RETURN_ENABLED",
            )
            db.execSQL(
                "alter table AppSettings add column circular_drag_enabled INTEGER NOT NULL default $DEFAULT_CIRCULAR_DRAG_ENABLED",
            )
            db.execSQL(
                "alter table AppSettings add column clockwise_drag_action INTEGER NOT NULL default $DEFAULT_CLOCKWISE_DRAG_ACTION",
            )
            db.execSQL(
                "alter table AppSettings add column counterclockwise_drag_action INTEGER NOT NULL " +
                    "default $DEFAULT_COUNTERCLOCKWISE_DRAG_ACTION",
            )
        }
    }

val MIGRATION_15_16 =
    object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column ghost_keys_enabled INTEGER NOT NULL default $DEFAULT_GHOST_KEYS_ENABLED",
            )
        }
    }

val MIGRATION_16_17 =
    object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column key_modifications TEXT NOT NULL default ''",
            )
        }
    }

val MIGRATION_17_18 =
    object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE AppSettings RENAME COLUMN key_size TO key_size_defunct",
            )
            db.execSQL(
                "ALTER TABLE AppSettings RENAME COLUMN key_width TO key_width_defunct",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN auto_size_keys INTEGER NOT NULL DEFAULT $DEFAULT_AUTO_SIZE_KEYS",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN non_square_keys INTEGER NOT NULL DEFAULT $DEFAULT_NON_SQUARE_KEYS",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN key_width_v18 INTEGER NOT NULL DEFAULT $DEFAULT_KEY_WIDTH",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN key_height_v18 INTEGER NOT NULL DEFAULT $DEFAULT_KEY_HEIGHT",
            )
            db.execSQL(
                "UPDATE AppSettings SET auto_size_keys = 0 WHERE key_size_defunct != $DEFAULT_KEY_HEIGHT",
            )
            db.execSQL(
                "UPDATE AppSettings SET non_square_keys = 1 WHERE key_width_defunct != $DEFAULT_KEY_WIDTH",
            )
            db.execSQL(
                "UPDATE AppSettings SET key_width_v18 = IFNULL(key_width_defunct, $DEFAULT_KEY_WIDTH)",
            )
            db.execSQL(
                "UPDATE AppSettings SET key_height_v18 = IFNULL(key_size_defunct, $DEFAULT_KEY_HEIGHT)",
            )
        }
    }

val MIGRATION_18_19 =
    object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column ignore_bottom_padding INTEGER NOT NULL default $DEFAULT_IGNORE_BOTTOM_PADDING",
            )
        }
    }

val MIGRATION_19_20 =
    object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column show_toast_on_layout_switch INTEGER NOT NULL default $DEFAULT_SHOW_TOAST_ON_LAYOUT_SWITCH",
            )
        }
    }
val MIGRATION_20_21 =
    object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column disable_fullscreen_editor INTEGER NOT NULL default $DEFAULT_DISABLE_FULLSCREEN_EDITOR",
            )
        }
    }
val MIGRATION_21_22 =
    object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "alter table AppSettings add column vibrate_on_slide INTEGER NOT NULL default $DEFAULT_VIBRATE_ON_SLIDE",
            )
        }
    }

val MIGRATION_22_23 =
    object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add clipboard settings columns (ClipboardItem table is in separate database)
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN clipboard_history_enabled INTEGER NOT NULL DEFAULT $DEFAULT_CLIPBOARD_HISTORY_ENABLED",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN clipboard_auto_cleanup_enabled INTEGER NOT NULL DEFAULT $DEFAULT_CLIPBOARD_AUTO_CLEANUP_ENABLED",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN clipboard_cleanup_after_minutes INTEGER NOT NULL DEFAULT $DEFAULT_CLIPBOARD_CLEANUP_AFTER_MINUTES",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN clipboard_size_limit_enabled INTEGER NOT NULL DEFAULT $DEFAULT_CLIPBOARD_SIZE_LIMIT_ENABLED",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN clipboard_max_size INTEGER NOT NULL DEFAULT $DEFAULT_CLIPBOARD_MAX_SIZE",
            )
        }
    }

val MIGRATION_23_24 =
    object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN position_padding INTEGER NOT NULL DEFAULT $DEFAULT_POSITION_PADDING",
            )
        }
    }

val MIGRATION_24_25 =
    object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN use_private_clipboard INTEGER NOT NULL DEFAULT $DEFAULT_USE_PRIVATE_CLIPBOARD",
            )
        }
    }

val MIGRATION_25_26 =
    object : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN show_on_screen_keyboard INTEGER NOT NULL DEFAULT $DEFAULT_SHOW_ON_SCREEN_KEYBOARD",
            )
        }
    }

val MIGRATION_26_27 =
    object : Migration(26, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN slide_hold_enabled INTEGER NOT NULL DEFAULT $DEFAULT_SLIDE_HOLD_ENABLED",
            )
        }
    }

val MIGRATION_27_28 =
    object : Migration(27, 28) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN esc_as_modifier INTEGER NOT NULL DEFAULT $DEFAULT_ESC_AS_MODIFIER",
            )
        }
    }

val MIGRATION_28_29 =
    object : Migration(28, 29) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN ctrl_as_modifier INTEGER NOT NULL DEFAULT $DEFAULT_CTRL_AS_MODIFIER",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN alt_as_modifier INTEGER NOT NULL DEFAULT $DEFAULT_ALT_AS_MODIFIER",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN shift_as_modifier INTEGER NOT NULL DEFAULT $DEFAULT_SHIFT_AS_MODIFIER",
            )
        }
    }

val MIGRATION_29_30 =
    object : Migration(29, 30) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS AppSettings_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    theme INTEGER NOT NULL DEFAULT $DEFAULT_THEME,
                    theme_color INTEGER NOT NULL DEFAULT $DEFAULT_THEME_COLOR,
                    hide_letters INTEGER NOT NULL DEFAULT $DEFAULT_HIDE_LETTERS,
                    ignore_bottom_padding INTEGER NOT NULL DEFAULT $DEFAULT_IGNORE_BOTTOM_PADDING,
                    disable_fullscreen_editor INTEGER NOT NULL DEFAULT $DEFAULT_DISABLE_FULLSCREEN_EDITOR,
                    key_height INTEGER NOT NULL DEFAULT $DEFAULT_KEY_HEIGHT,
                    vibrate_on_tap INTEGER NOT NULL DEFAULT $DEFAULT_VIBRATE_ON_TAP,
                    vibrate_on_slide INTEGER NOT NULL DEFAULT $DEFAULT_VIBRATE_ON_SLIDE,
                    min_swipe_length INTEGER NOT NULL DEFAULT $DEFAULT_MIN_SWIPE_LENGTH,
                    esc_as_modifier INTEGER NOT NULL DEFAULT $DEFAULT_ESC_AS_MODIFIER,
                    ctrl_as_modifier INTEGER NOT NULL DEFAULT $DEFAULT_CTRL_AS_MODIFIER,
                    alt_as_modifier INTEGER NOT NULL DEFAULT $DEFAULT_ALT_AS_MODIFIER,
                    shift_as_modifier INTEGER NOT NULL DEFAULT $DEFAULT_SHIFT_AS_MODIFIER,
                    keyboard_layout INTEGER NOT NULL DEFAULT $DEFAULT_KEYBOARD_LAYOUT,
                    keyboard_layouts TEXT NOT NULL DEFAULT '$DEFAULT_KEYBOARD_LAYOUT',
                    show_toast_on_layout_switch INTEGER NOT NULL DEFAULT $DEFAULT_SHOW_TOAST_ON_LAYOUT_SWITCH,
                    position INTEGER NOT NULL DEFAULT $DEFAULT_POSITION,
                    last_version_code_viewed INTEGER NOT NULL DEFAULT 0,
                    clipboard_history_enabled INTEGER NOT NULL DEFAULT $DEFAULT_CLIPBOARD_HISTORY_ENABLED,
                    clipboard_auto_cleanup_enabled INTEGER NOT NULL DEFAULT $DEFAULT_CLIPBOARD_AUTO_CLEANUP_ENABLED,
                    clipboard_cleanup_after_minutes INTEGER NOT NULL DEFAULT $DEFAULT_CLIPBOARD_CLEANUP_AFTER_MINUTES,
                    clipboard_size_limit_enabled INTEGER NOT NULL DEFAULT $DEFAULT_CLIPBOARD_SIZE_LIMIT_ENABLED,
                    clipboard_max_size INTEGER NOT NULL DEFAULT $DEFAULT_CLIPBOARD_MAX_SIZE,
                    use_private_clipboard INTEGER NOT NULL DEFAULT $DEFAULT_USE_PRIVATE_CLIPBOARD,
                    show_on_screen_keyboard INTEGER NOT NULL DEFAULT $DEFAULT_SHOW_ON_SCREEN_KEYBOARD
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO AppSettings_new (
                    id, theme, theme_color, hide_letters, ignore_bottom_padding,
                    disable_fullscreen_editor, key_height, vibrate_on_tap, vibrate_on_slide,
                    min_swipe_length, esc_as_modifier, ctrl_as_modifier, alt_as_modifier,
                    shift_as_modifier, keyboard_layout, keyboard_layouts,
                    show_toast_on_layout_switch, position, last_version_code_viewed,
                    clipboard_history_enabled, clipboard_auto_cleanup_enabled,
                    clipboard_cleanup_after_minutes, clipboard_size_limit_enabled,
                    clipboard_max_size, use_private_clipboard, show_on_screen_keyboard
                )
                SELECT
                    id, theme, theme_color, hide_letters, ignore_bottom_padding,
                    disable_fullscreen_editor, key_height_v18, vibrate_on_tap, vibrate_on_slide,
                    min_swipe_length, esc_as_modifier, ctrl_as_modifier, alt_as_modifier,
                    shift_as_modifier, keyboard_layout, keyboard_layouts,
                    show_toast_on_layout_switch, position, last_version_code_viewed,
                    clipboard_history_enabled, clipboard_auto_cleanup_enabled,
                    clipboard_cleanup_after_minutes, clipboard_size_limit_enabled,
                    clipboard_max_size, use_private_clipboard, show_on_screen_keyboard
                FROM AppSettings
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE AppSettings")
            db.execSQL("ALTER TABLE AppSettings_new RENAME TO AppSettings")
        }
    }

val MIGRATION_30_31 =
    object : Migration(30, 31) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN expand_emoji_picker INTEGER NOT NULL DEFAULT 1",
            )
        }
    }

val MIGRATION_31_32 =
    object : Migration(31, 32) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS AppSettings_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    theme INTEGER NOT NULL DEFAULT $DEFAULT_THEME,
                    theme_color INTEGER NOT NULL DEFAULT $DEFAULT_THEME_COLOR,
                    hide_letters INTEGER NOT NULL DEFAULT $DEFAULT_HIDE_LETTERS,
                    ignore_bottom_padding INTEGER NOT NULL DEFAULT $DEFAULT_IGNORE_BOTTOM_PADDING,
                    disable_fullscreen_editor INTEGER NOT NULL DEFAULT $DEFAULT_DISABLE_FULLSCREEN_EDITOR,
                    key_height INTEGER NOT NULL DEFAULT $DEFAULT_KEY_HEIGHT,
                    layer_heights TEXT NOT NULL DEFAULT '',
                    vibrate_on_tap INTEGER NOT NULL DEFAULT $DEFAULT_VIBRATE_ON_TAP,
                    vibrate_on_slide INTEGER NOT NULL DEFAULT $DEFAULT_VIBRATE_ON_SLIDE,
                    min_swipe_length INTEGER NOT NULL DEFAULT $DEFAULT_MIN_SWIPE_LENGTH,
                    esc_as_modifier INTEGER NOT NULL DEFAULT $DEFAULT_ESC_AS_MODIFIER,
                    ctrl_as_modifier INTEGER NOT NULL DEFAULT $DEFAULT_CTRL_AS_MODIFIER,
                    alt_as_modifier INTEGER NOT NULL DEFAULT $DEFAULT_ALT_AS_MODIFIER,
                    shift_as_modifier INTEGER NOT NULL DEFAULT $DEFAULT_SHIFT_AS_MODIFIER,
                    keyboard_layout INTEGER NOT NULL DEFAULT $DEFAULT_KEYBOARD_LAYOUT,
                    keyboard_layouts TEXT NOT NULL DEFAULT '$DEFAULT_KEYBOARD_LAYOUT',
                    show_toast_on_layout_switch INTEGER NOT NULL DEFAULT $DEFAULT_SHOW_TOAST_ON_LAYOUT_SWITCH,
                    position INTEGER NOT NULL DEFAULT $DEFAULT_POSITION,
                    last_version_code_viewed INTEGER NOT NULL DEFAULT 0,
                    clipboard_history_enabled INTEGER NOT NULL DEFAULT $DEFAULT_CLIPBOARD_HISTORY_ENABLED,
                    clipboard_auto_cleanup_enabled INTEGER NOT NULL DEFAULT $DEFAULT_CLIPBOARD_AUTO_CLEANUP_ENABLED,
                    clipboard_cleanup_after_minutes INTEGER NOT NULL DEFAULT $DEFAULT_CLIPBOARD_CLEANUP_AFTER_MINUTES,
                    clipboard_size_limit_enabled INTEGER NOT NULL DEFAULT $DEFAULT_CLIPBOARD_SIZE_LIMIT_ENABLED,
                    clipboard_max_size INTEGER NOT NULL DEFAULT $DEFAULT_CLIPBOARD_MAX_SIZE,
                    use_private_clipboard INTEGER NOT NULL DEFAULT $DEFAULT_USE_PRIVATE_CLIPBOARD,
                    show_on_screen_keyboard INTEGER NOT NULL DEFAULT $DEFAULT_SHOW_ON_SCREEN_KEYBOARD
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO AppSettings_new (
                    id, theme, theme_color, hide_letters, ignore_bottom_padding,
                    disable_fullscreen_editor, key_height, layer_heights, vibrate_on_tap,
                    vibrate_on_slide, min_swipe_length, esc_as_modifier, ctrl_as_modifier,
                    alt_as_modifier, shift_as_modifier, keyboard_layout, keyboard_layouts,
                    show_toast_on_layout_switch, position, last_version_code_viewed,
                    clipboard_history_enabled, clipboard_auto_cleanup_enabled,
                    clipboard_cleanup_after_minutes, clipboard_size_limit_enabled,
                    clipboard_max_size, use_private_clipboard, show_on_screen_keyboard
                )
                SELECT
                    id, theme, theme_color, hide_letters, ignore_bottom_padding,
                    disable_fullscreen_editor, key_height,
                    CASE WHEN expand_emoji_picker = 0 THEN 'EMOJI=4' ELSE '' END,
                    vibrate_on_tap, vibrate_on_slide, min_swipe_length, esc_as_modifier,
                    ctrl_as_modifier, alt_as_modifier, shift_as_modifier, keyboard_layout,
                    keyboard_layouts, show_toast_on_layout_switch, position,
                    last_version_code_viewed, clipboard_history_enabled,
                    clipboard_auto_cleanup_enabled, clipboard_cleanup_after_minutes,
                    clipboard_size_limit_enabled, clipboard_max_size, use_private_clipboard,
                    show_on_screen_keyboard
                FROM AppSettings
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE AppSettings")
            db.execSQL("ALTER TABLE AppSettings_new RENAME TO AppSettings")
        }
    }

val MIGRATION_32_33 =
    object : Migration(32, 33) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN show_debug_bar INTEGER NOT NULL DEFAULT $DEFAULT_SHOW_DEBUG_BAR",
            )
        }
    }

val MIGRATION_33_34 =
    object : Migration(33, 34) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN backdrop_enabled INTEGER NOT NULL DEFAULT $DEFAULT_BACKDROP_ENABLED",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN key_padding INTEGER NOT NULL DEFAULT $DEFAULT_KEY_PADDING",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN key_border_width INTEGER NOT NULL DEFAULT $DEFAULT_KEY_BORDER_WIDTH",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN key_radius INTEGER NOT NULL DEFAULT $DEFAULT_KEY_RADIUS",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN pushup_size INTEGER NOT NULL DEFAULT $DEFAULT_PUSHUP_SIZE",
            )
        }
    }

val MIGRATION_34_35 =
    object : Migration(34, 35) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // hide_symbols existed in early Thumb-Key, then MIGRATION_29_30 dropped it when
            // the table was rebuilt. Re-add it with the rest of the per-category hide flags.
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN hide_symbols INTEGER NOT NULL DEFAULT $DEFAULT_HIDE_SYMBOLS",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN hide_numbers INTEGER NOT NULL DEFAULT $DEFAULT_HIDE_NUMBERS",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN hide_modifiers INTEGER NOT NULL DEFAULT $DEFAULT_HIDE_MODIFIERS",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN hide_layer_switches INTEGER NOT NULL DEFAULT $DEFAULT_HIDE_LAYER_SWITCHES",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN hide_specials INTEGER NOT NULL DEFAULT $DEFAULT_HIDE_SPECIALS",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN hide_navigation INTEGER NOT NULL DEFAULT $DEFAULT_HIDE_NAVIGATION",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN hide_editing INTEGER NOT NULL DEFAULT $DEFAULT_HIDE_EDITING",
            )
        }
    }

val MIGRATION_35_36 =
    object : Migration(35, 36) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN animation_press_highlight INTEGER NOT NULL DEFAULT $DEFAULT_ANIMATION_PRESS_HIGHLIGHT",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN animation_release_flash INTEGER NOT NULL DEFAULT $DEFAULT_ANIMATION_RELEASE_FLASH",
            )
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN animation_letter_drop INTEGER NOT NULL DEFAULT $DEFAULT_ANIMATION_LETTER_DROP",
            )
        }
    }

val MIGRATION_36_37 =
    object : Migration(36, 37) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN hide_key_categories TEXT NOT NULL DEFAULT '$DEFAULT_HIDE_KEY_CATEGORIES'",
            )
        }
    }

val MIGRATION_37_38 =
    object : Migration(37, 38) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN key_padding_vertical INTEGER NOT NULL DEFAULT $DEFAULT_KEY_PADDING_VERTICAL",
            )
            // Keep the current square gutter: whoever already changed key_padding should not
            // suddenly get a different vertical gap.
            db.execSQL("UPDATE AppSettings SET key_padding_vertical = key_padding")
        }
    }

val MIGRATION_38_39 =
    object : Migration(38, 39) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN capture_system_clipboard INTEGER NOT NULL DEFAULT $DEFAULT_CAPTURE_SYSTEM_CLIPBOARD",
            )
        }
    }

val MIGRATION_39_40 =
    object : Migration(39, 40) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN vibrate_on_hold_repeat INTEGER NOT NULL DEFAULT $DEFAULT_VIBRATE_ON_HOLD_REPEAT",
            )
            // Hold-repeat used to share the tap toggle. Copy so a silent keyboard stays silent
            // until the user splits the two.
            db.execSQL("UPDATE AppSettings SET vibrate_on_hold_repeat = vibrate_on_tap")
        }
    }

val MIGRATION_40_41 =
    object : Migration(40, 41) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE AppSettings ADD COLUMN distinct_letter_control_colors INTEGER NOT NULL DEFAULT $DEFAULT_DISTINCT_LETTER_CONTROL_COLORS",
            )
        }
    }
