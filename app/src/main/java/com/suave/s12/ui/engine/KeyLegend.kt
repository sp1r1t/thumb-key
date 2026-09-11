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
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardCapslock
import androidx.compose.material.icons.outlined.KeyboardControlKey
import androidx.compose.material.icons.outlined.KeyboardOptionKey
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.suave.s12.engine.intent.CommandId
import com.suave.s12.engine.intent.KeyIntent
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.engine.modifier.ActivationMode
import com.suave.s12.engine.modifier.ModifierEngine
import com.suave.s12.engine.modifier.ModifierState

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

fun keyLegend(
    intent: KeyIntent?,
    hideLetters: Boolean,
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
                hideLetters && shown.any { it.isLetter() } -> null
                else -> KeyLegend.Text(shown)
            }
        }

        is KeyIntent.Command -> commandLegend(intent.id)

        is KeyIntent.ModifierPress -> modifierLegend(intent.modifier, modifierState)
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
        CommandId.SWITCH_IME -> KeyLegend.Icon(Icons.Outlined.Keyboard)
        CommandId.SWITCH_IME_VOICE -> KeyLegend.Icon(Icons.Outlined.Mic)
        CommandId.SWITCH_LANGUAGE -> KeyLegend.Icon(Icons.Outlined.Language)
        CommandId.MOVE_KEYBOARD -> KeyLegend.Icon(Icons.Outlined.LinearScale)
        CommandId.TOGGLE_EMOJI_MODE -> KeyLegend.Icon(Icons.Outlined.Mood)
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
