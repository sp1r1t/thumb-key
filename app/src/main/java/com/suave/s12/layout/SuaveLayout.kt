package com.suave.s12.layout

import com.suave.s12.engine.gesture.Direction
import com.suave.s12.engine.gesture.GestureConfig
import com.suave.s12.engine.gesture.SlideAxis
import com.suave.s12.engine.gesture.SwipeDirections
import com.suave.s12.engine.gesture.Zone
import com.suave.s12.engine.intent.CommandId
import com.suave.s12.engine.intent.KeyIntent
import com.suave.s12.engine.intent.KeyMapping
import com.suave.s12.engine.intent.KeyPosition
import com.suave.s12.engine.intent.Layout
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.engine.intent.SlideBehavior

/**
 * Suave's shift mapping: irregular capitalizations/combos that don't just uppercase (German
 * umlauts, "sch"/"ch" digraphs, "ß" -> "SS"), reused verbatim from the pre-rewrite layout. Fed to
 * [com.suave.s12.engine.modifier.ModifierEngine.resolve] as `shiftMappings`, not baked into the
 * layout itself - Shift is the modifier engine's job.
 */
val SUAVE_SHIFT_MAPPINGS: Map<String, String> =
    mapOf(
        "ö" to "Ö",
        "ä" to "Ä",
        "ü" to "Ü",
        "ß" to "SS",
        "sch" to "Sch",
        "ch" to "Ch",
    )

// minSwipeDistancePx is a placeholder here - Step 5's wiring overrides it per the user's swipe-
// threshold setting (`config.copy(minSwipeDistancePx = ...)`) when it turns this Layout into live
// GestureRecognizers; everything else here is intrinsic to each key, not user-tunable.
private val EIGHT_WAY_KEY = GestureConfig(minSwipeDistancePx = 64f, directions = SwipeDirections.EIGHT_WAY)
private val FOUR_WAY_KEY = GestureConfig(minSwipeDistancePx = 64f, directions = SwipeDirections.FOUR_WAY)

/**
 * Maps a key-definition token to what it means. Unlike the pre-rewrite `generateSuaveLayout`,
 * this has no mode parameter and no branching on Ctrl/Alt/Shift/Esc state at all - every token
 * means exactly one thing, always; modifier transformation happens later, entirely inside
 * [com.suave.s12.engine.modifier.ModifierEngine]. Anything not recognized as a special token is
 * just typed text (case-insensitive on the token itself, so "selectAll" and "selectall" are the
 * same key - German letters like "S" stay meaningful because they're multi-character or
 * mixed-case tokens like "sch" fall through untouched; the single letters this layout actually
 * uses are all lowercase already).
 */
private fun token(text: String): KeyIntent =
    when (text.lowercase()) {
        "shift" -> KeyIntent.ModifierPress(ModifierId.SHIFT)

        "ctrl" -> KeyIntent.ModifierPress(ModifierId.CTRL)

        "alt" -> KeyIntent.ModifierPress(ModifierId.ALT)

        "esc" -> KeyIntent.ModifierPress(ModifierId.ESC)

        "backspace" -> KeyIntent.Command(CommandId.BACKSPACE)

        // "return" and "enter" are the same Command: there is no KeyEvent-level primitive for
        // "smart submit" vs a literal newline. Well-behaved single-line vs multiline fields
        // already interpret one Enter KeyEvent correctly on their own.
        "return", "enter" -> KeyIntent.Command(CommandId.ENTER)

        "tab" -> KeyIntent.Command(CommandId.TAB)

        "left" -> KeyIntent.Command(CommandId.ARROW_LEFT)

        "right" -> KeyIntent.Command(CommandId.ARROW_RIGHT)

        "up" -> KeyIntent.Command(CommandId.ARROW_UP)

        "down" -> KeyIntent.Command(CommandId.ARROW_DOWN)

        "emoji" -> KeyIntent.Command(CommandId.TOGGLE_EMOJI_MODE)

        "numeric" -> KeyIntent.Command(CommandId.TOGGLE_NUMERIC_MODE)

        "abc" -> KeyIntent.Command(CommandId.TOGGLE_ABC_MODE)

        "copy" -> KeyIntent.Command(CommandId.COPY)

        "selectall" -> KeyIntent.Command(CommandId.SELECT_ALL)

        "cut" -> KeyIntent.Command(CommandId.CUT)

        "undo" -> KeyIntent.Command(CommandId.UNDO)

        "redo" -> KeyIntent.Command(CommandId.REDO)

        "paste" -> KeyIntent.Command(CommandId.PASTE)

        "clipboard" -> KeyIntent.Command(CommandId.TOGGLE_CLIPBOARD_HISTORY)

        "settings" -> KeyIntent.Command(CommandId.GOTO_SETTINGS)

        "hide" -> KeyIntent.Command(CommandId.TOGGLE_HIDE_LETTERS)

        "ime" -> KeyIntent.Command(CommandId.SWITCH_IME)

        "voice" -> KeyIntent.Command(CommandId.SWITCH_IME_VOICE)

        "lang" -> KeyIntent.Command(CommandId.SWITCH_LANGUAGE)

        "move" -> KeyIntent.Command(CommandId.MOVE_KEYBOARD)

        else -> KeyIntent.Text(text)
    }

private fun key(
    center: String,
    top: String? = null,
    topLeft: String? = null,
    topRight: String? = null,
    left: String? = null,
    right: String? = null,
    bottom: String? = null,
    bottomLeft: String? = null,
    bottomRight: String? = null,
    gesture: GestureConfig = EIGHT_WAY_KEY,
    slideBehavior: SlideBehavior? = null,
    columnSpan: Int = 1,
): KeyMapping {
    val intents =
        buildMap {
            put(Zone.Center, token(center))
            top?.let { put(Zone.Directional(Direction.UP), token(it)) }
            bottom?.let { put(Zone.Directional(Direction.DOWN), token(it)) }
            left?.let { put(Zone.Directional(Direction.LEFT), token(it)) }
            right?.let { put(Zone.Directional(Direction.RIGHT), token(it)) }
            topLeft?.let { put(Zone.Directional(Direction.UP_LEFT), token(it)) }
            topRight?.let { put(Zone.Directional(Direction.UP_RIGHT), token(it)) }
            bottomLeft?.let { put(Zone.Directional(Direction.DOWN_LEFT), token(it)) }
            bottomRight?.let { put(Zone.Directional(Direction.DOWN_RIGHT), token(it)) }
        }
    return KeyMapping(gesture, intents, slideBehavior, columnSpan)
}

private val SUAVE_BACKSPACE =
    key(
        "backspace",
        top = "'",
        bottom = "\"",
        gesture = FOUR_WAY_KEY.copy(slideAxis = SlideAxis.HORIZONTAL),
        slideBehavior = SlideBehavior.SELECT_AND_DELETE,
    )
private val SUAVE_SPACE =
    key(
        " ",
        top = "up",
        bottom = "down",
        left = "left",
        right = "right",
        gesture = FOUR_WAY_KEY.copy(slideAxis = SlideAxis.HORIZONTAL),
        slideBehavior = SlideBehavior.MOVE_CURSOR,
    )
private val SUAVE_CTRL = key("ctrl", right = "alt", top = "esc", gesture = FOUR_WAY_KEY)
private val SUAVE_EMOJI_KEY =
    key(
        "emoji",
        top = "settings",
        topLeft = "hide",
        bottom = "ime",
        bottomLeft = "voice",
        left = "lang",
        right = "move",
    )
private val SUAVE_NUMERIC_KEY =
    key(
        "numeric",
        top = "copy",
        topLeft = "selectAll",
        topRight = "cut",
        left = "clipboard",
        bottomLeft = "undo",
        bottomRight = "redo",
        bottom = "paste",
    )
private val SUAVE_ABC_KEY =
    key(
        "abc",
        top = "copy",
        topLeft = "selectAll",
        topRight = "cut",
        left = "clipboard",
        bottomLeft = "undo",
        bottomRight = "redo",
        bottom = "paste",
    )
private val SUAVE_ENTER = key("return", top = "tab", gesture = FOUR_WAY_KEY, columnSpan = 2)

/**
 * Suave, ported to pure data: position + gesture -> intent, nothing else. Compare to the
 * pre-rewrite `generateSuaveLayout` (~360 lines of mode-branching Kotlin) - this is what "a
 * layout should be a flat table of data" actually looks like once modifier composition isn't the
 * layout's job. Row/col indices match the original grid; Enter's [KeyMapping.columnSpan] of 2 is
 * what makes the 4-key bottom row fill the same width as the 5-key letter rows.
 */
val SUAVE_LAYOUT: Layout =
    mapOf(
        // Row 0
        KeyPosition(0, 0) to key("o", top = "1", right = "2", bottom = "ö"),
        KeyPosition(0, 1) to key("r", top = "4", right = "5", bottom = "w", bottomLeft = "?", bottomRight = ",", left = "3"),
        KeyPosition(0, 2) to SUAVE_BACKSPACE,
        KeyPosition(0, 3) to key("t", top = "7", right = "8", bottom = "p", bottomLeft = ".", bottomRight = "!", left = "6"),
        KeyPosition(0, 4) to key("h", top = "0", bottom = "q", left = "9"),
        // Row 1
        KeyPosition(1, 0) to key("a", right = "ä", bottom = "+"),
        KeyPosition(1, 1) to key("e", top = "v", topRight = "€", right = "c", bottom = "f", bottomRight = "ch", left = "z"),
        KeyPosition(1, 2) to SUAVE_SPACE,
        KeyPosition(1, 3) to key("n", top = "b", right = "k", bottom = "m", left = "g"),
        KeyPosition(1, 4) to key("s", top = "~", topLeft = "$", bottomLeft = "sch", left = "ß"),
        // Row 2
        KeyPosition(2, 0) to key("u", top = "ü", topRight = "(", right = "[", bottomRight = "{"),
        KeyPosition(2, 1) to key("i", topLeft = "<", right = "x", bottom = "#", bottomLeft = "@", bottomRight = "$"),
        KeyPosition(2, 2) to
            key(
                "shift",
                top = "-",
                left = "—",
                topLeft = ";",
                topRight = ":",
                right = "_",
                bottom = "^",
                bottomLeft = "%",
                bottomRight = "&",
            ),
        KeyPosition(2, 3) to key("d", top = "j", topRight = ">", bottom = "=", bottomLeft = "*", bottomRight = "/", left = "y"),
        KeyPosition(2, 4) to key("l", top = "|", topLeft = ")", bottom = "\\", bottomLeft = "}", left = "]"),
        // Row 3
        KeyPosition(3, 0) to SUAVE_CTRL,
        KeyPosition(3, 1) to SUAVE_EMOJI_KEY,
        KeyPosition(3, 2) to SUAVE_NUMERIC_KEY,
        KeyPosition(3, 3) to SUAVE_ENTER,
    )

/**
 * Suave's numeric layer: a full grid of the same shape as [SUAVE_LAYOUT], with abc on the
 * cluster that numeric occupies on the main layer. Superscripts/subscripts are real typed
 * characters; combining diacritics from the old overlay are omitted until keys can show a
 * display label distinct from the committed text.
 */
val SUAVE_NUMERIC_LAYOUT: Layout =
    mapOf(
        KeyPosition(0, 0) to key("1", top = "\u00B9", bottom = "\u2081", gesture = FOUR_WAY_KEY),
        KeyPosition(0, 1) to key("2", top = "\u00B2", bottom = "\u2082", gesture = FOUR_WAY_KEY),
        KeyPosition(0, 2) to SUAVE_BACKSPACE,
        KeyPosition(0, 3) to key("3", top = "\u00B3", bottom = "\u2083", gesture = FOUR_WAY_KEY),
        KeyPosition(0, 4) to key("4", top = "\u2074", bottom = "\u2084", gesture = FOUR_WAY_KEY),
        KeyPosition(1, 0) to key("5", top = "\u2075", bottom = "\u2085", gesture = FOUR_WAY_KEY),
        KeyPosition(1, 1) to key("6", top = "\u2076", topRight = "\u20AC", bottom = "\u2086"),
        KeyPosition(1, 2) to SUAVE_SPACE,
        KeyPosition(1, 3) to key("7", top = "\u2077", bottom = "\u2087", gesture = FOUR_WAY_KEY),
        KeyPosition(1, 4) to key("8", top = "\u2078", topLeft = "$", bottom = "\u2088"),
        KeyPosition(2, 0) to
            key(
                "9",
                top = "\u2079",
                topRight = "(",
                right = "[",
                bottom = "\u2089",
                bottomRight = "{",
            ),
        KeyPosition(2, 1) to
            key(
                ",",
                top = ";",
                topLeft = "<",
                bottom = "#",
                bottomLeft = "@",
                bottomRight = "$",
                gesture = FOUR_WAY_KEY,
            ),
        KeyPosition(2, 2) to
            key(
                "+",
                top = "-",
                topLeft = ";",
                topRight = ":",
                right = "_",
                bottom = "^",
                bottomLeft = "%",
                bottomRight = "&",
            ),
        KeyPosition(2, 3) to
            key(
                ".",
                top = ":",
                topRight = ">",
                bottom = "=",
                bottomLeft = "*",
                bottomRight = "/",
            ),
        KeyPosition(2, 4) to
            key(
                "0",
                top = "\u2070",
                topLeft = ")",
                left = "]",
                bottom = "\u2080",
                bottomLeft = "}",
                gesture = FOUR_WAY_KEY,
            ),
        KeyPosition(3, 0) to SUAVE_CTRL,
        KeyPosition(3, 1) to SUAVE_EMOJI_KEY,
        KeyPosition(3, 2) to SUAVE_ABC_KEY,
        KeyPosition(3, 3) to SUAVE_ENTER,
    )

/**
 * Functional row shown under the system emoji picker. Ctrl is replaced by Backspace (the
 * letter grid is gone, so delete still has to live here) and the 123 cluster is replaced by
 * the spacebar with arrow swipes so cursor movement and spaces work while picking emoji.
 * Positions are a single row 0 so [layoutRows] yields one row.
 */
val SUAVE_EMOJI_BOTTOM_ROW: Layout =
    mapOf(
        KeyPosition(0, 0) to SUAVE_BACKSPACE,
        KeyPosition(0, 1) to SUAVE_EMOJI_KEY,
        KeyPosition(0, 2) to SUAVE_SPACE,
        KeyPosition(0, 3) to SUAVE_ENTER,
    )
