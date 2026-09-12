package com.suave.s12.ui.engine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.KeyboardBackspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.automirrored.outlined.KeyboardTab
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Abc
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.EmojiEmotions
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
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.ui.graphics.vector.ImageVector
import com.suave.s12.engine.intent.CommandId
import com.suave.s12.engine.intent.KeyIntent
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.engine.modifier.ActivationMode
import com.suave.s12.engine.modifier.ModifierEngine
import com.suave.s12.engine.modifier.ModifierState
import com.suave.s12.utils.ColorVariant
import com.suave.s12.utils.FontSizeVariant

/**
 * What a key zone shows. Commands and modifiers use the same Material icons Thumb-Key put on
 * these actions; letters stay text and follow Shift via [ModifierEngine.applyShift].
 */
sealed class KeyLegend {
    data class Text(
        val text: String,
    ) : KeyLegend()

    data class Icon(
        val icon: ImageVector,
    ) : KeyLegend()
}

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
): KeyLegend? =
    when (intent) {
        null, KeyIntent.Noop -> null

        is KeyIntent.Text -> {
            val shown =
                if (modifierState.isActive(ModifierId.SHIFT)) {
                    ModifierEngine.applyShift(intent.text, shiftMappings)
                } else {
                    intent.text
                }
            when {
                shown.isBlank() -> null
                visibility.hides(classifyText(shown)) -> null
                else -> KeyLegend.Text(shown)
            }
        }

        is KeyIntent.Command -> {
            val legend = commandLegend(intent.id) ?: return null
            if (visibility.hides(intent.id.legendCategory())) null else legend
        }

        is KeyIntent.ModifierPress -> {
            if (visibility.hides(LegendCategory.MODIFIER)) {
                null
            } else {
                modifierLegend(intent.modifier, modifierState)
            }
        }
    }

/** Same size/color roles Thumb-Key used: center is LARGE/PRIMARY, swipes are SMALL/SECONDARY. */
fun legendFontSizeVariant(isCenter: Boolean): FontSizeVariant =
    if (isCenter) FontSizeVariant.LARGE else FontSizeVariant.SMALL

fun legendColorVariant(isCenter: Boolean): ColorVariant =
    if (isCenter) ColorVariant.PRIMARY else ColorVariant.SECONDARY

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
        CommandId.TOGGLE_CLIPBOARD_HISTORY,
        -> LegendCategory.SPECIAL
    }

private fun commandLegend(id: CommandId): KeyLegend? =
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
    }

private fun modifierLegend(
    id: ModifierId,
    modifierState: ModifierState,
): KeyLegend =
    when (id) {
        ModifierId.SHIFT -> KeyLegend.Icon(shiftIcon(modifierState))
        ModifierId.CTRL -> KeyLegend.Icon(Icons.Outlined.KeyboardControlKey)
        ModifierId.ALT -> KeyLegend.Icon(Icons.Outlined.KeyboardOptionKey)
        ModifierId.ESC -> KeyLegend.Text("esc")
    }

private fun shiftIcon(modifierState: ModifierState): ImageVector =
    when (modifierState.active[ModifierId.SHIFT]?.mode) {
        ActivationMode.LOCKED -> Icons.Outlined.KeyboardCapslock
        ActivationMode.HELD, ActivationMode.ONE_SHOT -> Icons.Outlined.KeyboardArrowUp
        null -> Icons.Outlined.ArrowDropUp
    }
