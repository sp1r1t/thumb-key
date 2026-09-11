package com.suave.s12.ui.engine

import com.suave.s12.engine.action.CursorDirection
import com.suave.s12.engine.action.SemanticAction
import com.suave.s12.engine.intent.CommandId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyAnimationTest {
    @Test
    fun `release animation only runs for typed text`() {
        assertEquals("s", typedTextForReleaseAnimation(SemanticAction.TypeText("s")))
        assertEquals(" ", typedTextForReleaseAnimation(SemanticAction.TypeText(" ")))
        assertNull(typedTextForReleaseAnimation(SemanticAction.TypeText("")))
        assertNull(typedTextForReleaseAnimation(SemanticAction.TypeCommand(CommandId.BACKSPACE)))
        assertNull(typedTextForReleaseAnimation(SemanticAction.MoveCursor(CursorDirection.LEFT)))
        assertNull(typedTextForReleaseAnimation(SemanticAction.Noop))
    }

    @Test
    fun `release animations share a trigger so either overlay or letter drop can run alone`() {
        assertEquals(false, KeyAnimationSettings(releaseFlash = false, letterDrop = false).playsRelease)
        assertEquals(true, KeyAnimationSettings(releaseFlash = true, letterDrop = false).playsRelease)
        assertEquals(true, KeyAnimationSettings(releaseFlash = false, letterDrop = true).playsRelease)
    }
}
