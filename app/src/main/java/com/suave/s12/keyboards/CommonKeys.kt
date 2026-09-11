@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.suave.s12.keyboards

import android.view.KeyEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import com.suave.s12.utils.*
import com.suave.s12.utils.ColorVariant.*
import com.suave.s12.utils.FontSizeVariant.*
import com.suave.s12.utils.KeyAction.*
import com.suave.s12.utils.SwipeNWay.*

val COPY_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.ContentCopy),
        action = Copy,
        color = MUTED,
    )
val SELECT_ALL_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.SelectAll),
        action = SelectAll,
        swipeReturnAction = SelectLineWithCursor,
        color = MUTED,
    )
val CUT_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.ContentCut),
        action = Cut,
        color = MUTED,
    )
val UNDO_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.AutoMirrored.Outlined.Undo),
        action = Undo,
        color = MUTED,
    )
val REDO_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.AutoMirrored.Outlined.Redo),
        action = Redo,
        color = MUTED,
    )
val PASTE_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.ContentPaste),
        action = Paste,
        swipeReturnAction = ToggleClipboardMode(true),
        color = MUTED,
    )
val GOTO_SETTINGS_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.Settings),
        action = GotoSettings,
        color = MUTED,
    )
val SWITCH_IME_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.Keyboard),
        action = SwitchIME,
        color = MUTED,
    )
val SWITCH_IME_VOICE_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.Mic),
        action = SwitchIMEVoice,
        color = MUTED,
    )
val SWITCH_LANGUAGE_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.Language),
        action = SwitchLanguage,
        color = MUTED,
    )
val HIDE_KEYBOARD_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.KeyboardDoubleArrowDown),
        action = HideKeyboard,
        color = MUTED,
    )

fun textEditKeyItem(center: KeyC): KeyItemC =
    KeyItemC(
        backgroundColor = SURFACE_VARIANT,
        swipeType = EIGHT_WAY,
        center = center,
        top = COPY_KEYC,
        topLeft = SELECT_ALL_KEYC,
        topRight = CUT_KEYC,
        bottomLeft = UNDO_KEYC,
        bottomRight = REDO_KEYC,
        bottom = PASTE_KEYC,
    )

val TOGGLE_NUMERIC_MODE_FALSE_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.Abc),
        action = ToggleNumericMode(false),
        size = LARGE,
    )
val TOGGLE_NUMERIC_MODE_TRUE_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.Numbers),
        action = ToggleNumericMode(true),
        size = LARGE,
        color = SECONDARY,
    )
val NUMERIC_KEY_ITEM =
    textEditKeyItem(
        center = TOGGLE_NUMERIC_MODE_TRUE_KEYC,
    )

val TOGGLE_EMOJI_MODE_TRUE_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.Mood),
        action = ToggleEmojiMode(true),
        size = LARGE,
        color = SECONDARY,
    )

val TOGGLE_CLIPBOARD_MODE_TRUE_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.Inventory),
        action = ToggleClipboardMode(true),
        size = LARGE,
        color = SECONDARY,
    )

val TOGGLE_EMOJI_MODE_FALSE_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.Abc),
        action = ToggleEmojiMode(false),
        size = LARGE,
    )
val EMOJI_BACK_KEY_ITEM =
    KeyItemC(
        center = TOGGLE_EMOJI_MODE_FALSE_KEYC,
        backgroundColor = SURFACE_VARIANT,
    )

val TOGGLE_CAPS_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.KeyboardCapslock),
        capsModeDisplay = KeyDisplay.IconDisplay(Icons.Outlined.Copyright),
        action = ToggleCapsLock,
        swipeReturnAction = ToggleCurrentWordCapitalization(true),
        color = MUTED,
    )

val TOGGLE_SHIFT_TRUE_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.ArrowDropUp),
        action = ToggleShiftMode(true),
        swipeReturnAction = ToggleCurrentWordCapitalization(true),
        color = MUTED,
    )

val TOGGLE_SHIFT_FALSE_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.ArrowDropDown),
        action = ToggleShiftMode(false),
        swipeReturnAction = ToggleCurrentWordCapitalization(false),
        color = MUTED,
    )

val TOGGLE_CTRL_TRUE_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.KeyboardControlKey),
        action = ToggleCtrlMode(true),
        color = MUTED,
    )

val TOGGLE_CTRL_FALSE_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.KeyboardDoubleArrowDown),
        action = ToggleCtrlMode(false),
        color = MUTED,
    )

val TOGGLE_ALT_TRUE_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.KeyboardOptionKey),
        action = ToggleAltMode(true),
        color = MUTED,
    )

val TOGGLE_ALT_FALSE_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.Outlined.KeyboardDoubleArrowDown),
        action = ToggleAltMode(false),
        color = MUTED,
    )

val BACKSPACE_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.AutoMirrored.Outlined.KeyboardBackspace),
        action = DeleteKeyAction,
        size = LARGE,
        color = SECONDARY,
    )
val BACKSPACE_TEXT_MANIPULATION_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.AutoMirrored.Outlined.KeyboardBackspace),
        action = KeyAction.DeleteViaTextManipulation,
        size = LARGE,
        color = SECONDARY,
    )
val DELETE_WORD_BEFORE_CURSOR_KEYC =
    KeyC(
        DeleteWordBeforeCursor,
        display = null,
    )
val DELETE_WORD_AFTER_CURSOR_KEYC =
    KeyC(
        DeleteWordAfterCursor,
        display = null,
    )

val PREVIOUS_WORD_BEFORE_CURSOR_KEYC =
    KeyC(
        PreviousWordBeforeCursor,
        display = null,
    )
val NEXT_WORD_AFTER_CURSOR_KEYC =
    KeyC(
        NextWordAfterCursor,
        display = null,
    )

val BACKSPACE_KEY_ITEM =
    KeyItemC(
        center = BACKSPACE_KEYC,
        swipeType = TWO_WAY_HORIZONTAL,
        slideType = SlideType.DELETE,
        left = DELETE_WORD_BEFORE_CURSOR_KEYC,
        right = DELETE_WORD_AFTER_CURSOR_KEYC,
        backgroundColor = SURFACE_VARIANT,
        longPress = DeleteWordBeforeCursor,
    )

val SPACEBAR_LEFT_KEYC =
    KeyC(
        action =
            SendEvent(
                KeyEvent(
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_DPAD_LEFT,
                ),
            ),
        display = null,
    )
val SPACEBAR_RIGHT_KEYC =
    KeyC(
        action =
            SendEvent(
                KeyEvent(
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_DPAD_RIGHT,
                ),
            ),
        display = null,
    )
val SPACEBAR_PROGRAMMING_TOP_KEYC =
    KeyC(
        action =
            SendEvent(
                KeyEvent(
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_DPAD_UP,
                ),
            ),
        display = null,
    )
val SPACEBAR_PROGRAMMING_BOTTOM_KEYC =
    KeyC(
        action =
            SendEvent(
                KeyEvent(
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_DPAD_DOWN,
                ),
            ),
        display = null,
    )
val RETURN_KEYC =
    KeyC(
        display = KeyDisplay.IconDisplay(Icons.AutoMirrored.Outlined.KeyboardReturn),
        action = IMECompleteAction,
        size = LARGE,
        color = SECONDARY,
    )
val RETURN_KEY_ITEM =
    KeyItemC(
        center = RETURN_KEYC,
        backgroundColor = SURFACE_VARIANT,
        longPress = CommitText("\n"),
    )

val NOOP_KEYC =
    KeyC(
        action = Noop,
        display = null,
    )

// Builds a KeyC that sends a raw KeyEvent with a meta-state flag applied (e.g. Ctrl+<key>,
// Alt+<key>) - used by Suave's CTRLED/ALTED modes for single-character shortcut keys.
fun keyCModifier(
    flagCode: Int,
    keyCode: Int,
    displayText: String,
    swipeReturnAction: KeyAction? = null,
    display: KeyDisplay = KeyDisplay.TextDisplay(displayText),
    capsModeDisplay: KeyDisplay? = null,
    size: FontSizeVariant = FontSizeVariant.SMALL,
    color: ColorVariant =
        when (size) {
            LARGE -> ColorVariant.PRIMARY
            else -> ColorVariant.SECONDARY
        },
): KeyC =
    KeyC(
        SendEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0, flagCode)),
        swipeReturnAction,
        display,
        capsModeDisplay,
        size,
        color,
    )

