package com.suave.keyboard.layout

import com.suave.keyboard.engine.gesture.Zone
import com.suave.keyboard.engine.intent.KeyIntent
import com.suave.keyboard.engine.intent.KeyPosition
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LayoutPreviewSessionTest {
    @After
    fun tearDown() {
        LayoutPreviewSession.stop()
    }

    @Test
    fun `Edited on always serves the draft`() {
        val baseline = layout("a", "baseline")
        val draft = layout("a", "draft")
        val other = layout("b", "other")
        LayoutPreviewSession.bind(draft, baseline, useEdited = true)
        assertSame(draft, LayoutPreviewSession.resolve(other))
        assertSame(draft, LayoutPreviewSession.resolve(draft))
    }

    @Test
    fun `Edited off honors live selection of another layout`() {
        val baseline = layout("a", "baseline")
        val draft = layout("a", "draft")
        val other = layout("b", "other")
        LayoutPreviewSession.bind(draft, baseline, useEdited = false)
        assertSame(other, LayoutPreviewSession.resolve(other))
    }

    @Test
    fun `Edited off uses baseline when settings still select the layout under edit`() {
        val baseline = layout("a", "baseline")
        val draft = layout("a", "draft")
        LayoutPreviewSession.bind(draft, baseline, useEdited = false)
        // Registry-selected object is the draft; resolve must not return it.
        assertSame(baseline, LayoutPreviewSession.resolve(draft))
    }

    @Test
    fun `toggling Edited does not clear the session`() {
        val baseline = layout("a", "baseline")
        val draft = layout("a", "draft")
        LayoutPreviewSession.bind(draft, baseline, useEdited = true)
        LayoutPreviewSession.setUseEdited(false)
        assertEquals(true, LayoutPreviewSession.isActive)
        assertEquals(false, LayoutPreviewSession.useEdited.value)
        assertSame(baseline, LayoutPreviewSession.resolve(draft))
        LayoutPreviewSession.setUseEdited(true)
        assertSame(draft, LayoutPreviewSession.resolve(draft))
    }

    private fun layout(
        id: String,
        center: String,
    ): NamedLayout {
        val key =
            blankKeyMapping().copy(
                intents = mapOf(Zone.Center to KeyIntent.Text(center)),
            )
        return NamedLayout(
            id = id,
            title = id,
            layers =
                listOf(
                    LayerDefinition(
                        id = ActiveLayer.MAIN,
                        title = "ABC",
                        icon = LayerIcon.Abc,
                        keyGrid = mapOf(KeyPosition(0, 0) to key),
                    ),
                ),
            homeLayerId = ActiveLayer.MAIN,
        )
    }
}
