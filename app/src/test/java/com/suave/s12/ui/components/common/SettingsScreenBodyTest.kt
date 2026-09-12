package com.suave.s12.ui.components.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsScreenBodyTest {
    @Test
    fun `closed keyboard keeps the scaffold nav inset`() {
        assertEquals(48.dp, settingsBodyBottomPadding(scaffoldBottom = 48.dp, imeBottom = 0.dp))
    }

    @Test
    fun `open keyboard uses the larger inset instead of stacking ime on nav`() {
        assertEquals(300.dp, settingsBodyBottomPadding(scaffoldBottom = 48.dp, imeBottom = 300.dp))
        assertEquals(300.dp, settingsBodyBottomPadding(scaffoldBottom = 300.dp, imeBottom = 300.dp))
        assertEquals(300.dp, settingsBodyBottomPadding(scaffoldBottom = 300.dp, imeBottom = 0.dp))
    }

    @Test
    fun `body padding keeps scaffold sides and unions the bottom inset`() {
        val padding =
            settingsScreenBodyPadding(
                padding = PaddingValues(start = 8.dp, top = 64.dp, end = 12.dp, bottom = 48.dp),
                layoutDirection = LayoutDirection.Ltr,
                imeBottom = 300.dp,
            )
        assertEquals(8.dp, padding.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(64.dp, padding.calculateTopPadding())
        assertEquals(12.dp, padding.calculateRightPadding(LayoutDirection.Ltr))
        assertEquals(300.dp, padding.calculateBottomPadding())
    }
}
