package com.suave.keyboard.ui.engine

import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.KeyboardBackspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.automirrored.outlined.KeyboardTab
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Abc
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardCapslock
import androidx.compose.material.icons.outlined.KeyboardControlKey
import androidx.compose.material.icons.outlined.KeyboardOptionKey
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SpaceBar
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.suave.keyboard.R
import com.suave.keyboard.engine.gesture.Zone
import com.suave.keyboard.engine.intent.CommandId
import com.suave.keyboard.engine.intent.KeyIntent
import com.suave.keyboard.engine.intent.KeyFillRole
import com.suave.keyboard.engine.intent.KeyMapping
import com.suave.keyboard.engine.intent.ModifierId
import com.suave.keyboard.engine.modifier.ActivationMode
import com.suave.keyboard.engine.modifier.ModifierEngine
import com.suave.keyboard.engine.modifier.ModifierState
import com.suave.keyboard.layout.ActiveLayer
import com.suave.keyboard.utils.ColorVariant
import com.suave.keyboard.utils.FontSizeVariant

/**
 * What a key zone shows. Commands and modifiers use the same Material icons Thumb-Key put on
 * these actions; letters stay text and follow Shift / caps lock via [ModifierEngine.applyCase].
 */
sealed class KeyLegend {
    data class Text(
        val text: String,
    ) : KeyLegend()

    data class Icon(
        val icon: ImageVector,
    ) : KeyLegend()
}

/** Live IME action from the focused editor; defaults to a Return-style action in previews. */
val LocalImeAction = compositionLocalOf { EditorInfo.IME_ACTION_UNSPECIFIED }

/**
 * How a key zone's label is grouped for the Appearance hide toggles. Space is editing but has
 * no legend, so hiding editing does not change how the space key looks. There is no ninth
 * category: every [CommandId] and every [KeyIntent.Text] value lands in one of these.
 */
enum class LegendCategory {
    LETTER,
    NUMBER,
    SYMBOL,
    MODIFIER,
    LAYER_SWITCH,
    SPECIAL,
    NAVIGATION,
    EDITING,
}

data class LegendVisibility(
    val hideLetters: Boolean = false,
    val hideSymbols: Boolean = false,
    val hideNumbers: Boolean = false,
    val hideModifiers: Boolean = false,
    val hideLayerSwitches: Boolean = false,
    val hideSpecials: Boolean = false,
    val hideNavigation: Boolean = false,
    val hideEditing: Boolean = false,
    /** Availability: the layout-switch legend is dead if there is nothing to cycle to. */
    val canSwitchLayout: Boolean = true,
    /** Availability: Move keyboard is dead if fewer than two positions are reachable. */
    val canMoveKeyboard: Boolean = true,
) {
    fun hides(category: LegendCategory): Boolean =
        when (category) {
            LegendCategory.LETTER -> hideLetters
            LegendCategory.NUMBER -> hideNumbers
            LegendCategory.SYMBOL -> hideSymbols
            LegendCategory.MODIFIER -> hideModifiers
            LegendCategory.LAYER_SWITCH -> hideLayerSwitches
            LegendCategory.SPECIAL -> hideSpecials
            LegendCategory.NAVIGATION -> hideNavigation
            LegendCategory.EDITING -> hideEditing
        }
}

fun keyLegend(
    intent: KeyIntent?,
    visibility: LegendVisibility,
    modifierState: ModifierState,
    shiftMappings: Map<String, String>,
    capsLockMappings: Map<String, String> = emptyMap(),
    displayLabel: String? = null,
    switchLayerIcons: Map<String, ImageVector> = emptyMap(),
    imeAction: Int = EditorInfo.IME_ACTION_UNSPECIFIED,
): KeyLegend? =
    when (intent) {
        null, KeyIntent.Noop -> null

        is KeyIntent.Text -> {
            val source = displayLabel ?: intent.text
            val shown =
                ModifierEngine.applyCase(
                    source,
                    modifierState,
                    shiftMappings,
                    capsLockMappings,
                    intent.case,
                )
            when {
                shown.isBlank() -> null
                visibility.hides(classifyText(shown)) -> null
                else -> KeyLegend.Text(shown)
            }
        }

        is KeyIntent.Command -> {
            if (intent.id == CommandId.SWITCH_LANGUAGE && !visibility.canSwitchLayout) return null
            if (intent.id == CommandId.MOVE_KEYBOARD && !visibility.canMoveKeyboard) return null
            if (visibility.hides(intent.id.legendCategory())) return null
            if (intent.id == CommandId.IME_ACTION) return imeActionLegend(imeAction)
            if (!displayLabel.isNullOrBlank()) return KeyLegend.Text(displayLabel)
            commandLegend(intent.id)
        }

        is KeyIntent.SwitchLayer -> {
            if (visibility.hides(LegendCategory.LAYER_SWITCH)) {
                null
            } else {
                KeyLegend.Icon(switchLayerLegendIcon(intent.layerId, switchLayerIcons))
            }
        }

        is KeyIntent.ModifierPress -> {
            if (visibility.hides(LegendCategory.MODIFIER)) {
                null
            } else {
                modifierLegend(intent.modifier, modifierState)
            }
        }
    }

private fun switchLayerLegendIcon(
    layerId: String,
    switchLayerIcons: Map<String, ImageVector>,
): ImageVector {
    switchLayerIcons[layerId]?.let { return it }
    return when (layerId) {
        "main", "MAIN" -> Icons.Outlined.Abc
        "numeric", "NUMERIC" -> Icons.Outlined.Numbers
        "emoji", "EMOJI" -> Icons.Outlined.EmojiEmotions
        "clipboard", "CLIPBOARD" -> Icons.Outlined.History
        else -> Icons.Outlined.Functions
    }
}

/** Same color roles Thumb-Key used: center is PRIMARY, swipes are SECONDARY. */
fun legendFontSizeVariant(isCenter: Boolean): FontSizeVariant =
    if (isCenter) FontSizeVariant.LARGE else FontSizeVariant.SMALL

fun legendColorVariant(isCenter: Boolean): ColorVariant =
    if (isCenter) ColorVariant.PRIMARY else ColorVariant.SECONDARY

/** Center ~31% of the keycap, swipes ~15%. Thumb-Key LARGE/SMALL (40%/20%) crowded this grid. */
internal const val LEGEND_CENTER_DIV = 3.25f
internal const val LEGEND_SWIPE_DIV = 6.5f

fun legendFontSize(
    isCenter: Boolean,
    keySize: Dp,
    isUpperCase: Boolean,
): Dp {
    val upperCaseFactor = if (isUpperCase) 0.8f else 1f
    val divFactor = if (isCenter) LEGEND_CENTER_DIV else LEGEND_SWIPE_DIV
    return keySize.times(upperCaseFactor).div(divFactor)
}

internal fun classifyText(shown: String): LegendCategory =
    when {
        shown.any { it.isLetter() } -> LegendCategory.LETTER
        shown.any { it.isDigit() } -> LegendCategory.NUMBER
        else -> LegendCategory.SYMBOL
    }

internal fun CommandId.legendCategory(): LegendCategory =
    when (this) {
        CommandId.ENTER,
        CommandId.TAB,
        CommandId.BACKSPACE,
        CommandId.DELETE_FORWARD,
        CommandId.SPACE,
        -> LegendCategory.EDITING

        CommandId.ARROW_LEFT,
        CommandId.ARROW_RIGHT,
        CommandId.ARROW_UP,
        CommandId.ARROW_DOWN,
        -> LegendCategory.NAVIGATION

        CommandId.ESCAPE,
        CommandId.CTRL,
        CommandId.ALT,
        CommandId.SHIFT,
        -> LegendCategory.MODIFIER

        CommandId.TOGGLE_EMOJI_MODE,
        CommandId.TOGGLE_NUMERIC_MODE,
        CommandId.TOGGLE_ABC_MODE,
        CommandId.TOGGLE_CLIPBOARD_HISTORY,
        -> LegendCategory.LAYER_SWITCH

        CommandId.COPY,
        CommandId.CUT,
        CommandId.PASTE,
        CommandId.SELECT_ALL,
        CommandId.UNDO,
        CommandId.REDO,
        CommandId.GOTO_SETTINGS,
        CommandId.TOGGLE_HIDE_LETTERS,
        CommandId.SWITCH_IME,
        CommandId.SWITCH_IME_VOICE,
        CommandId.SWITCH_LANGUAGE,
        CommandId.MOVE_KEYBOARD,
        CommandId.IME_ACTION,
        CommandId.HIDE_KEYBOARD,
        CommandId.META,
        CommandId.TOGGLE_LANDSCAPE_FLOATING,
        -> LegendCategory.SPECIAL
    }

/** Icon or short text used on the live keycap and in the layout editor. */
fun commandLegend(id: CommandId): KeyLegend? =
    when (id) {
        CommandId.ENTER -> KeyLegend.Icon(Icons.AutoMirrored.Outlined.KeyboardReturn)
        CommandId.TAB -> KeyLegend.Icon(Icons.AutoMirrored.Outlined.KeyboardTab)
        CommandId.BACKSPACE -> KeyLegend.Icon(Icons.AutoMirrored.Outlined.KeyboardBackspace)
        CommandId.DELETE_FORWARD -> KeyLegend.Text("del")
        CommandId.SPACE -> null
        CommandId.ARROW_LEFT -> KeyLegend.Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft)
        CommandId.ARROW_RIGHT -> KeyLegend.Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight)
        CommandId.ARROW_UP -> KeyLegend.Icon(Icons.Outlined.KeyboardArrowUp)
        CommandId.ARROW_DOWN -> KeyLegend.Icon(Icons.Outlined.KeyboardArrowDown)
        CommandId.ESCAPE -> KeyLegend.Text("esc")
        CommandId.CTRL -> KeyLegend.Icon(Icons.Outlined.KeyboardControlKey)
        CommandId.ALT -> KeyLegend.Icon(Icons.Outlined.KeyboardOptionKey)
        CommandId.SHIFT -> KeyLegend.Icon(Icons.Outlined.ArrowDropUp)
        CommandId.META -> KeyLegend.Text("Meta")
        CommandId.COPY -> KeyLegend.Icon(Icons.Outlined.ContentCopy)
        CommandId.CUT -> KeyLegend.Icon(Icons.Outlined.ContentCut)
        CommandId.PASTE -> KeyLegend.Icon(Icons.Outlined.ContentPaste)
        CommandId.SELECT_ALL -> KeyLegend.Icon(Icons.Outlined.SelectAll)
        CommandId.UNDO -> KeyLegend.Icon(Icons.AutoMirrored.Outlined.Undo)
        CommandId.REDO -> KeyLegend.Icon(Icons.AutoMirrored.Outlined.Redo)
        CommandId.GOTO_SETTINGS -> KeyLegend.Icon(Icons.Outlined.Settings)
        CommandId.TOGGLE_HIDE_LETTERS -> KeyLegend.Icon(Icons.Outlined.HideImage)
        CommandId.TOGGLE_CLIPBOARD_HISTORY -> KeyLegend.Icon(Icons.Outlined.History)
        CommandId.SWITCH_IME -> KeyLegend.Icon(Icons.Outlined.Keyboard)
        CommandId.SWITCH_IME_VOICE -> KeyLegend.Icon(Icons.Outlined.Mic)
        CommandId.SWITCH_LANGUAGE -> KeyLegend.Icon(Icons.Outlined.SwapHoriz)
        CommandId.MOVE_KEYBOARD -> KeyLegend.Icon(Icons.Outlined.ViewColumn)
        CommandId.TOGGLE_EMOJI_MODE -> KeyLegend.Icon(Icons.Outlined.EmojiEmotions)
        CommandId.TOGGLE_NUMERIC_MODE -> KeyLegend.Icon(Icons.Outlined.Numbers)
        CommandId.TOGGLE_ABC_MODE -> KeyLegend.Icon(Icons.Outlined.Abc)
        CommandId.IME_ACTION -> KeyLegend.Icon(Icons.AutoMirrored.Outlined.KeyboardReturn)
        CommandId.HIDE_KEYBOARD -> KeyLegend.Icon(Icons.Outlined.Keyboard)
        CommandId.TOGGLE_LANDSCAPE_FLOATING -> KeyLegend.Icon(Icons.Outlined.Fullscreen)
    }

/** Icon for the live IME action key, following the focused field's EditorInfo action. */
fun imeActionLegend(imeAction: Int): KeyLegend =
    when (imeAction) {
        EditorInfo.IME_ACTION_GO -> KeyLegend.Icon(Icons.AutoMirrored.Outlined.ArrowForward)
        EditorInfo.IME_ACTION_SEARCH -> KeyLegend.Icon(Icons.Outlined.Search)
        EditorInfo.IME_ACTION_SEND -> KeyLegend.Icon(Icons.AutoMirrored.Outlined.Send)
        EditorInfo.IME_ACTION_NEXT -> KeyLegend.Icon(Icons.AutoMirrored.Outlined.KeyboardTab)
        EditorInfo.IME_ACTION_DONE -> KeyLegend.Icon(Icons.Outlined.Done)
        EditorInfo.IME_ACTION_PREVIOUS -> KeyLegend.Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft)
        else -> KeyLegend.Icon(Icons.AutoMirrored.Outlined.KeyboardReturn)
    }

fun CommandId.titleRes(): Int =
    when (this) {
        CommandId.ENTER -> R.string.command_enter
        CommandId.TAB -> R.string.command_tab
        CommandId.BACKSPACE -> R.string.command_backspace
        CommandId.DELETE_FORWARD -> R.string.command_delete_forward
        CommandId.SPACE -> R.string.command_space
        CommandId.ARROW_LEFT -> R.string.command_arrow_left
        CommandId.ARROW_RIGHT -> R.string.command_arrow_right
        CommandId.ARROW_UP -> R.string.command_arrow_up
        CommandId.ARROW_DOWN -> R.string.command_arrow_down
        CommandId.ESCAPE -> R.string.command_escape
        CommandId.CTRL -> R.string.command_ctrl
        CommandId.ALT -> R.string.command_alt
        CommandId.SHIFT -> R.string.command_shift
        CommandId.COPY -> R.string.command_copy
        CommandId.CUT -> R.string.command_cut
        CommandId.PASTE -> R.string.command_paste
        CommandId.SELECT_ALL -> R.string.command_select_all
        CommandId.UNDO -> R.string.command_undo
        CommandId.REDO -> R.string.command_redo
        CommandId.GOTO_SETTINGS -> R.string.command_settings
        CommandId.TOGGLE_HIDE_LETTERS -> R.string.command_hide_letters
        CommandId.SWITCH_IME -> R.string.command_switch_ime
        CommandId.SWITCH_IME_VOICE -> R.string.command_switch_ime_voice
        CommandId.SWITCH_LANGUAGE -> R.string.command_switch_language
        CommandId.MOVE_KEYBOARD -> R.string.command_move_keyboard
        CommandId.TOGGLE_EMOJI_MODE -> R.string.command_emoji
        CommandId.TOGGLE_NUMERIC_MODE -> R.string.command_numeric
        CommandId.TOGGLE_ABC_MODE -> R.string.command_abc
        CommandId.TOGGLE_CLIPBOARD_HISTORY -> R.string.command_clipboard
        CommandId.IME_ACTION -> R.string.command_ime_action
        CommandId.HIDE_KEYBOARD -> R.string.command_hide_keyboard
        CommandId.META -> R.string.command_meta
        CommandId.TOGGLE_LANDSCAPE_FLOATING -> R.string.command_toggle_landscape_floating
    }

/** Legend for the layout editor: always has a mark (Space gets an icon here). */
fun commandEditorLegend(id: CommandId): KeyLegend =
    commandLegend(id) ?: when (id) {
        CommandId.SPACE -> KeyLegend.Icon(Icons.Outlined.SpaceBar)
        else -> KeyLegend.Text("?")
    }

@Composable
fun commandDisplayTitle(id: CommandId): String = stringResource(id.titleRes())

private fun modifierLegend(
    id: ModifierId,
    modifierState: ModifierState,
): KeyLegend =
    when (id) {
        ModifierId.SHIFT -> KeyLegend.Icon(shiftIcon(modifierState))
        ModifierId.CTRL -> KeyLegend.Icon(Icons.Outlined.KeyboardControlKey)
        ModifierId.ALT -> KeyLegend.Icon(Icons.Outlined.KeyboardOptionKey)
        ModifierId.ESC -> KeyLegend.Text("esc")
        ModifierId.META -> KeyLegend.Text("Meta")
    }

/**
 * Control keys (space, modifiers, 123, Enter, and similar) use the theme's surfaceVariant
 * fill when letter/control colors are split. Letter, number, and symbol keys use surface.
 * Classification follows the center intent only: a letter with a command on a swipe is still
 * a letter key.
 */
fun KeyMapping.usesControlKeyFill(): Boolean {
    when (fillRole) {
        KeyFillRole.LETTER -> return false
        KeyFillRole.CONTROL -> return true
        KeyFillRole.SPACER -> return true
        KeyFillRole.AUTO -> Unit
    }
    val center = intents[Zone.Center]
    return when (center) {
        is KeyIntent.Text -> center.text.isBlank()
        else -> true
    }
}

fun KeyMapping.restingFillVariant(distinctLetterControlColors: Boolean): ColorVariant =
    if (distinctLetterControlColors && !usesControlKeyFill()) {
        ColorVariant.SURFACE
    } else {
        ColorVariant.SURFACE_VARIANT
    }

private fun shiftIcon(modifierState: ModifierState): ImageVector =
    when (modifierState.active[ModifierId.SHIFT]?.mode) {
        ActivationMode.LOCKED -> Icons.Outlined.KeyboardCapslock
        ActivationMode.HELD, ActivationMode.ONE_SHOT -> Icons.Outlined.KeyboardArrowUp
        null -> Icons.Outlined.ArrowDropUp
    }

/** Shared text/icon mark for a resolved [KeyLegend] (live keyboard and layout preview). */
@Composable
fun KeyLegendMark(
    legend: KeyLegend,
    fontSize: TextUnit,
    iconSize: Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    when (legend) {
        is KeyLegend.Text -> {
            Text(
                legend.text,
                modifier = modifier,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                lineHeight = fontSize,
                color = color,
            )
        }
        is KeyLegend.Icon -> {
            Icon(
                imageVector = legend.icon,
                contentDescription = legend.icon.name,
                tint = color,
                modifier = modifier.size(iconSize),
            )
        }
    }
}
