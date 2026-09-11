package com.suave.s12.ui.engine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.outlined.Abc
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardCapslock
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.ViewColumn
import com.suave.s12.engine.intent.CommandId
import com.suave.s12.engine.intent.KeyIntent
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.engine.modifier.ActivationMode
import com.suave.s12.engine.modifier.ModifierState
import com.suave.s12.layout.SUAVE_SHIFT_MAPPINGS
import com.suave.s12.utils.ColorVariant
import com.suave.s12.utils.FontSizeVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyLegendTest {
    private val idle = ModifierState()
    private val shown = LegendVisibility()

    @Test
    fun `letter keys preview the shift mapping while Shift is active`() {
        val shiftOn = ModifierState().activate(ModifierId.SHIFT, ActivationMode.ONE_SHOT)

        assertEquals(KeyLegend.Text("a"), legend(KeyIntent.Text("a")))
        assertEquals(KeyLegend.Text("A"), legend(KeyIntent.Text("a"), modifierState = shiftOn))
        assertEquals(
            KeyLegend.Text("Sch"),
            legend(KeyIntent.Text("sch"), modifierState = shiftOn, shiftMappings = SUAVE_SHIFT_MAPPINGS),
        )
        assertEquals(
            KeyLegend.Text("SS"),
            legend(KeyIntent.Text("ß"), modifierState = shiftOn, shiftMappings = SUAVE_SHIFT_MAPPINGS),
        )
    }

    @Test
    fun `classifyText splits letters, numbers, and symbols`() {
        assertEquals(LegendCategory.LETTER, classifyText("s"))
        assertEquals(LegendCategory.LETTER, classifyText("ß"))
        assertEquals(LegendCategory.LETTER, classifyText("1a"))
        assertEquals(LegendCategory.NUMBER, classifyText("1"))
        assertEquals(LegendCategory.NUMBER, classifyText("42"))
        assertEquals(LegendCategory.SYMBOL, classifyText("+"))
        assertEquals(LegendCategory.SYMBOL, classifyText("."))
    }

    @Test
    fun `hiding letters leaves other categories visible`() {
        val hidden = LegendVisibility(hideLetters = true)
        assertNull(legend(KeyIntent.Text("s"), hidden))
        assertEquals(KeyLegend.Text("1"), legend(KeyIntent.Text("1"), hidden))
        assertEquals(KeyLegend.Text("+"), legend(KeyIntent.Text("+"), hidden))
        assertEquals(KeyLegend.Icon(Icons.Outlined.ContentCopy), legend(KeyIntent.Command(CommandId.COPY), hidden))
    }

    @Test
    fun `hiding symbols leaves letters and numbers visible`() {
        val hidden = LegendVisibility(hideSymbols = true)
        assertNull(legend(KeyIntent.Text("+"), hidden))
        assertEquals(KeyLegend.Text("s"), legend(KeyIntent.Text("s"), hidden))
        assertEquals(KeyLegend.Text("1"), legend(KeyIntent.Text("1"), hidden))
    }

    @Test
    fun `hiding numbers leaves letters and symbols visible`() {
        val hidden = LegendVisibility(hideNumbers = true)
        assertNull(legend(KeyIntent.Text("1"), hidden))
        assertEquals(KeyLegend.Text("s"), legend(KeyIntent.Text("s"), hidden))
        assertEquals(KeyLegend.Text("+"), legend(KeyIntent.Text("+"), hidden))
    }

    @Test
    fun `hiding modifiers blanks Shift Ctrl Alt Esc but not letters`() {
        val hidden = LegendVisibility(hideModifiers = true)
        assertNull(legend(KeyIntent.ModifierPress(ModifierId.SHIFT), hidden))
        assertNull(legend(KeyIntent.ModifierPress(ModifierId.CTRL), hidden))
        assertNull(legend(KeyIntent.Command(CommandId.ESCAPE), hidden))
        assertEquals(KeyLegend.Text("s"), legend(KeyIntent.Text("s"), hidden))
    }

    @Test
    fun `hiding layer switches blanks emoji numeric abc icons`() {
        val hidden = LegendVisibility(hideLayerSwitches = true)
        assertNull(legend(KeyIntent.Command(CommandId.TOGGLE_EMOJI_MODE), hidden))
        assertNull(legend(KeyIntent.Command(CommandId.TOGGLE_NUMERIC_MODE), hidden))
        assertNull(legend(KeyIntent.Command(CommandId.TOGGLE_ABC_MODE), hidden))
        assertEquals(KeyLegend.Icon(Icons.Outlined.ContentCopy), legend(KeyIntent.Command(CommandId.COPY), hidden))
    }

    @Test
    fun `hiding specials blanks copy settings and hide-letters but not arrows`() {
        val hidden = LegendVisibility(hideSpecials = true)
        assertNull(legend(KeyIntent.Command(CommandId.COPY), hidden))
        assertNull(legend(KeyIntent.Command(CommandId.GOTO_SETTINGS), hidden))
        assertNull(legend(KeyIntent.Command(CommandId.TOGGLE_HIDE_LETTERS), hidden))
        assertEquals(
            KeyLegend.Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft),
            legend(KeyIntent.Command(CommandId.ARROW_LEFT), hidden),
        )
    }

    @Test
    fun `hiding navigation blanks arrows but not editing`() {
        val hidden = LegendVisibility(hideNavigation = true)
        assertNull(legend(KeyIntent.Command(CommandId.ARROW_LEFT), hidden))
        assertNull(legend(KeyIntent.Command(CommandId.ARROW_UP), hidden))
        assertEquals(
            KeyLegend.Icon(Icons.AutoMirrored.Outlined.KeyboardReturn),
            legend(KeyIntent.Command(CommandId.ENTER), hidden),
        )
    }

    @Test
    fun `hiding editing blanks enter tab backspace but not space which has no legend`() {
        val hidden = LegendVisibility(hideEditing = true)
        assertNull(legend(KeyIntent.Command(CommandId.ENTER), hidden))
        assertNull(legend(KeyIntent.Command(CommandId.TAB), hidden))
        assertNull(legend(KeyIntent.Command(CommandId.BACKSPACE), hidden))
        assertNull(legend(KeyIntent.Command(CommandId.SPACE), shown))
        assertNull(legend(KeyIntent.Command(CommandId.SPACE), hidden))
    }

    @Test
    fun `function commands use the Thumb-Key icons`() {
        assertEquals(KeyLegend.Icon(Icons.Outlined.EmojiEmotions), legend(KeyIntent.Command(CommandId.TOGGLE_EMOJI_MODE)))
        assertEquals(KeyLegend.Icon(Icons.Outlined.Numbers), legend(KeyIntent.Command(CommandId.TOGGLE_NUMERIC_MODE)))
        assertEquals(KeyLegend.Icon(Icons.Outlined.Abc), legend(KeyIntent.Command(CommandId.TOGGLE_ABC_MODE)))
        assertEquals(KeyLegend.Icon(Icons.Outlined.SwapHoriz), legend(KeyIntent.Command(CommandId.SWITCH_LANGUAGE)))
        assertEquals(KeyLegend.Icon(Icons.Outlined.ViewColumn), legend(KeyIntent.Command(CommandId.MOVE_KEYBOARD)))
        assertNull(legend(KeyIntent.Text(" ")))
    }

    @Test
    fun `center legends use Thumb-Key primary large, swipes use secondary small`() {
        assertEquals(ColorVariant.PRIMARY, legendColorVariant(isCenter = true))
        assertEquals(ColorVariant.SECONDARY, legendColorVariant(isCenter = false))
        assertEquals(FontSizeVariant.LARGE, legendFontSizeVariant(isCenter = true))
        assertEquals(FontSizeVariant.SMALL, legendFontSizeVariant(isCenter = false))
    }

    @Test
    fun `shift key icon follows inactive, sticky, and caps-lock state`() {
        assertEquals(
            KeyLegend.Icon(Icons.Outlined.ArrowDropUp),
            legend(KeyIntent.ModifierPress(ModifierId.SHIFT)),
        )
        assertEquals(
            KeyLegend.Icon(Icons.Outlined.KeyboardArrowUp),
            legend(
                KeyIntent.ModifierPress(ModifierId.SHIFT),
                modifierState = ModifierState().activate(ModifierId.SHIFT, ActivationMode.ONE_SHOT),
            ),
        )
        assertEquals(
            KeyLegend.Icon(Icons.Outlined.KeyboardCapslock),
            legend(
                KeyIntent.ModifierPress(ModifierId.SHIFT),
                modifierState = ModifierState().activate(ModifierId.SHIFT, ActivationMode.LOCKED),
            ),
        )
    }

    private fun legend(
        intent: KeyIntent?,
        visibility: LegendVisibility = shown,
        modifierState: ModifierState = idle,
        shiftMappings: Map<String, String> = emptyMap(),
    ): KeyLegend? = keyLegend(intent, visibility, modifierState, shiftMappings)
}
