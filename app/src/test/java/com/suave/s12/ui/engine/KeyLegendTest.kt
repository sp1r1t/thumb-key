package com.suave.s12.ui.engine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Abc
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardCapslock
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Numbers
import com.suave.s12.engine.intent.CommandId
import com.suave.s12.engine.intent.KeyIntent
import com.suave.s12.engine.intent.ModifierId
import com.suave.s12.engine.modifier.ActivationMode
import com.suave.s12.engine.modifier.ModifierState
import com.suave.s12.layout.SUAVE_SHIFT_MAPPINGS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyLegendTest {
    @Test
    fun `letter keys preview the shift mapping while Shift is active`() {
        val shiftOn = ModifierState().activate(ModifierId.SHIFT, ActivationMode.ONE_SHOT)

        assertEquals(KeyLegend.Text("a"), keyLegend(KeyIntent.Text("a"), false, ModifierState(), emptyMap()))
        assertEquals(KeyLegend.Text("A"), keyLegend(KeyIntent.Text("a"), false, shiftOn, emptyMap()))
        assertEquals(KeyLegend.Text("Sch"), keyLegend(KeyIntent.Text("sch"), false, shiftOn, SUAVE_SHIFT_MAPPINGS))
        assertEquals(KeyLegend.Text("SS"), keyLegend(KeyIntent.Text("ß"), false, shiftOn, SUAVE_SHIFT_MAPPINGS))
    }

    @Test
    fun `hideLetters hides letter text but not symbols or command icons`() {
        val hidden = true
        assertNull(keyLegend(KeyIntent.Text("s"), hidden, ModifierState(), emptyMap()))
        assertEquals(KeyLegend.Text("1"), keyLegend(KeyIntent.Text("1"), hidden, ModifierState(), emptyMap()))
        assertEquals(KeyLegend.Text("+"), keyLegend(KeyIntent.Text("+"), hidden, ModifierState(), emptyMap()))
        assertEquals(
            KeyLegend.Icon(Icons.Outlined.ContentCopy),
            keyLegend(KeyIntent.Command(CommandId.COPY), hidden, ModifierState(), emptyMap()),
        )
    }

    @Test
    fun `function commands use the Thumb-Key icons`() {
        val idle = ModifierState()
        assertEquals(KeyLegend.Icon(Icons.Outlined.Mood), keyLegend(KeyIntent.Command(CommandId.TOGGLE_EMOJI_MODE), false, idle, emptyMap()))
        assertEquals(
            KeyLegend.Icon(Icons.Outlined.Numbers),
            keyLegend(KeyIntent.Command(CommandId.TOGGLE_NUMERIC_MODE), false, idle, emptyMap()),
        )
        assertEquals(KeyLegend.Icon(Icons.Outlined.Abc), keyLegend(KeyIntent.Command(CommandId.TOGGLE_ABC_MODE), false, idle, emptyMap()))
        assertNull(keyLegend(KeyIntent.Text(" "), false, idle, emptyMap()))
    }

    @Test
    fun `shift key icon follows inactive, sticky, and caps-lock state`() {
        assertEquals(
            KeyLegend.Icon(Icons.Outlined.ArrowDropUp),
            keyLegend(KeyIntent.ModifierPress(ModifierId.SHIFT), false, ModifierState(), emptyMap()),
        )
        assertEquals(
            KeyLegend.Icon(Icons.Outlined.KeyboardArrowUp),
            keyLegend(
                KeyIntent.ModifierPress(ModifierId.SHIFT),
                false,
                ModifierState().activate(ModifierId.SHIFT, ActivationMode.ONE_SHOT),
                emptyMap(),
            ),
        )
        assertEquals(
            KeyLegend.Icon(Icons.Outlined.KeyboardCapslock),
            keyLegend(
                KeyIntent.ModifierPress(ModifierId.SHIFT),
                false,
                ModifierState().activate(ModifierId.SHIFT, ActivationMode.LOCKED),
                emptyMap(),
            ),
        )
    }
}
