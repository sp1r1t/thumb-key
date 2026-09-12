package com.suave.s12.ui.engine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.outlined.Abc
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardCapslock
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.ui.unit.dp
import com.suave.s12.engine.gesture.GestureConfig
import com.suave.s12.engine.gesture.Zone
import com.suave.s12.engine.intent.CommandId
import com.suave.s12.engine.intent.KeyIntent
import com.suave.s12.engine.intent.KeyMapping
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.engine.modifier.ActivationMode
import com.suave.s12.engine.modifier.ModifierState
import com.suave.s12.layout.SUAVE_SHIFT_MAPPINGS
import com.suave.s12.utils.ColorVariant
import com.suave.s12.utils.FontSizeVariant
import com.suave.s12.utils.fontSizeVariantToFontSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyLegendTest {
    private val idle = ModifierState()
    private val shown = LegendVisibility()
    private val config = GestureConfig(minSwipeDistancePx = 64f)

    private fun mapping(center: KeyIntent) = KeyMapping(config, mapOf(Zone.Center to center))

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
    fun `hiding layer switches blanks emoji numeric abc and clipboard icons`() {
        val hidden = LegendVisibility(hideLayerSwitches = true)
        assertNull(legend(KeyIntent.Command(CommandId.TOGGLE_EMOJI_MODE), hidden))
        assertNull(legend(KeyIntent.Command(CommandId.TOGGLE_NUMERIC_MODE), hidden))
        assertNull(legend(KeyIntent.Command(CommandId.TOGGLE_ABC_MODE), hidden))
        assertNull(legend(KeyIntent.Command(CommandId.TOGGLE_CLIPBOARD_HISTORY), hidden))
        assertEquals(KeyLegend.Icon(Icons.Outlined.ContentCopy), legend(KeyIntent.Command(CommandId.COPY), hidden))
    }

    @Test
    fun `hiding specials blanks copy settings and hide-letters but not arrows`() {
        val hidden = LegendVisibility(hideSpecials = true)
        assertNull(legend(KeyIntent.Command(CommandId.COPY), hidden))
        assertNull(legend(KeyIntent.Command(CommandId.GOTO_SETTINGS), hidden))
        assertNull(legend(KeyIntent.Command(CommandId.TOGGLE_HIDE_LETTERS), hidden))
        assertEquals(
            KeyLegend.Icon(Icons.Outlined.History),
            legend(KeyIntent.Command(CommandId.TOGGLE_CLIPBOARD_HISTORY), hidden),
        )
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
        assertEquals(KeyLegend.Icon(Icons.Outlined.History), legend(KeyIntent.Command(CommandId.TOGGLE_CLIPBOARD_HISTORY)))
        assertNull(legend(KeyIntent.Text(" ")))
    }

    @Test
    fun `layout switch legend is hidden when there is no other layout`() {
        val noSwitch = LegendVisibility(canSwitchLayout = false)
        assertNull(legend(KeyIntent.Command(CommandId.SWITCH_LANGUAGE), noSwitch))
        assertEquals(
            KeyLegend.Icon(Icons.Outlined.SwapHoriz),
            legend(KeyIntent.Command(CommandId.SWITCH_LANGUAGE), shown),
        )
        assertEquals(
            KeyLegend.Icon(Icons.Outlined.ViewColumn),
            legend(KeyIntent.Command(CommandId.MOVE_KEYBOARD), noSwitch),
        )
    }

    @Test
    fun `move keyboard legend is hidden when fewer than two positions are reachable`() {
        val noMove = LegendVisibility(canMoveKeyboard = false)
        assertNull(legend(KeyIntent.Command(CommandId.MOVE_KEYBOARD), noMove))
        assertEquals(
            KeyLegend.Icon(Icons.Outlined.SwapHoriz),
            legend(KeyIntent.Command(CommandId.SWITCH_LANGUAGE), noMove),
        )
        assertEquals(
            KeyLegend.Icon(Icons.Outlined.ViewColumn),
            legend(KeyIntent.Command(CommandId.MOVE_KEYBOARD), shown),
        )
    }

    @Test
    fun `center legends use Thumb-Key primary large, swipes use secondary small`() {
        assertEquals(ColorVariant.PRIMARY, legendColorVariant(isCenter = true))
        assertEquals(ColorVariant.SECONDARY, legendColorVariant(isCenter = false))
        assertEquals(FontSizeVariant.LARGE, legendFontSizeVariant(isCenter = true))
        assertEquals(FontSizeVariant.SMALL, legendFontSizeVariant(isCenter = false))
    }

    @Test
    fun `letter number and symbol centers use letter fill, controls use variant`() {
        assertFalse(mapping(KeyIntent.Text("s")).usesControlKeyFill())
        assertFalse(mapping(KeyIntent.Text("1")).usesControlKeyFill())
        assertFalse(mapping(KeyIntent.Text("+")).usesControlKeyFill())
        assertFalse(mapping(KeyIntent.Text("sch")).usesControlKeyFill())
        assertTrue(mapping(KeyIntent.Text(" ")).usesControlKeyFill())
        assertTrue(mapping(KeyIntent.Text("")).usesControlKeyFill())
        assertTrue(mapping(KeyIntent.Command(CommandId.BACKSPACE)).usesControlKeyFill())
        assertTrue(mapping(KeyIntent.Command(CommandId.TOGGLE_NUMERIC_MODE)).usesControlKeyFill())
        assertTrue(mapping(KeyIntent.Command(CommandId.ENTER)).usesControlKeyFill())
        assertTrue(mapping(KeyIntent.Command(CommandId.SPACE)).usesControlKeyFill())
        assertTrue(mapping(KeyIntent.ModifierPress(ModifierId.SHIFT)).usesControlKeyFill())
        assertTrue(mapping(KeyIntent.Noop).usesControlKeyFill())
        assertTrue(KeyMapping(config, emptyMap()).usesControlKeyFill())
    }

    @Test
    fun `distinct letter control fills pick SURFACE for letters and SURFACE_VARIANT for controls`() {
        val letter = mapping(KeyIntent.Text("s"))
        val control = mapping(KeyIntent.Command(CommandId.BACKSPACE))
        assertEquals(ColorVariant.SURFACE, letter.restingFillVariant(distinctLetterControlColors = true))
        assertEquals(ColorVariant.SURFACE_VARIANT, control.restingFillVariant(distinctLetterControlColors = true))
        assertEquals(ColorVariant.SURFACE_VARIANT, letter.restingFillVariant(distinctLetterControlColors = false))
        assertEquals(ColorVariant.SURFACE_VARIANT, control.restingFillVariant(distinctLetterControlColors = false))
    }

    @Test
    fun `engine legends are smaller than Thumb-Key LARGE and SMALL`() {
        val key = 64.dp
        val center = legendFontSize(isCenter = true, key, isUpperCase = false)
        val swipe = legendFontSize(isCenter = false, key, isUpperCase = false)
        assertTrue(center < fontSizeVariantToFontSize(FontSizeVariant.LARGE, key, false))
        assertTrue(swipe < fontSizeVariantToFontSize(FontSizeVariant.SMALL, key, false))
        assertTrue(center > swipe)
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
