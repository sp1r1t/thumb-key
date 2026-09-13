package com.suave.keyboard.layout

import com.suave.keyboard.engine.intent.KeyIntent
import com.suave.keyboard.engine.intent.KeyPosition
import com.suave.keyboard.engine.intent.SlideBehavior
import com.suave.keyboard.engine.gesture.SlideAxis
import com.suave.keyboard.engine.gesture.Zone
import com.suave.keyboard.layout.json.encodeNamedLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserLayoutStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `file round-trip preserves layout`() {
        val dir = tempFolder.newFolder("layouts")
        val original =
            BuiltinLayouts.S12.copy(
                id = "user_test1",
                title = "Test copy",
                tags = listOf("en", "qa"),
            )
        UserLayoutFiles.save(dir, original)
        val restored = UserLayoutFiles.load(dir, "user_test1")
        assertNotNull(restored)
        assertEquals(original.id, restored!!.id)
        assertEquals(original.title, restored.title)
        assertEquals(listOf("en", "qa"), restored.tags)
        assertEquals(original.homeLayer().keyGrid.keys, restored.homeLayer().keyGrid.keys)
        assertEquals(original.shiftMappings, restored.shiftMappings)
        assertEquals(
            encodeNamedLayout(original),
            encodeNamedLayout(restored),
        )
    }

    @Test
    fun `import export helpers round-trip`() {
        val s12 = com.suave.keyboard.layout.json.loadS12Asset()
        val json = UserLayoutFiles.exportToJson(s12)
        val decoded = UserLayoutFiles.importFromJson(json)
        assertEquals(s12.id, decoded.id)
        assertEquals(s12.homeLayer().keyGrid.keys, decoded.homeLayer().keyGrid.keys)
    }

    @Test
    fun `delete removes file`() {
        val dir = tempFolder.newFolder("layouts")
        val layout = blankNamedLayout("user_x", "X", listOf(3, 3))
        UserLayoutFiles.save(dir, layout)
        assertTrue(UserLayoutFiles.delete(dir, "user_x"))
        assertNull(UserLayoutFiles.load(dir, "user_x"))
        assertFalse(UserLayoutFiles.delete(dir, "user_x"))
    }

    @Test
    fun `blank layout has center noop on every key`() {
        val layout = blankLayout(listOf(2, 3))
        assertEquals(5, layout.size)
        assertEquals(KeyIntent.Noop, layout.getValue(KeyPosition(0, 0)).intents[Zone.Center])
        assertEquals(KeyIntent.Noop, layout.getValue(KeyPosition(1, 2)).intents[Zone.Center])
    }

    @Test
    fun `resizeRows pads and trims`() {
        val base = blankLayout(listOf(2, 2))
        val withSlide =
            base + (
                KeyPosition(0, 0) to
                    blankKeyMapping(
                        slideAxis = SlideAxis.HORIZONTAL,
                        slideBehavior = SlideBehavior.MOVE_CURSOR,
                    )
            )
        val resized = withSlide.resizeRows(listOf(3, 1))
        assertEquals(4, resized.size)
        assertEquals(SlideAxis.HORIZONTAL, resized.getValue(KeyPosition(0, 0)).gestureConfig.slideAxis)
        assertNotNull(resized[KeyPosition(0, 2)])
        assertNull(resized[KeyPosition(1, 1)])
    }

    @Test
    fun `resizeRows restores trimmed keys from memory`() {
        val memory = LayoutResizeMemory()
        val marked =
            blankLayout(listOf(2, 2)) + (
                KeyPosition(1, 1) to
                    blankKeyMapping().copy(
                        intents = mapOf(Zone.Center to KeyIntent.Text("keep")),
                    )
            )
        val shrunk = marked.resizeRows(listOf(2, 1), memory)
        assertNull(shrunk[KeyPosition(1, 1)])
        val restored = shrunk.resizeRows(listOf(2, 2), memory)
        assertEquals(
            KeyIntent.Text("keep"),
            restored.getValue(KeyPosition(1, 1)).intents[Zone.Center],
        )
    }

    @Test
    fun `resizeRows restores trimmed whole rows from memory`() {
        val memory = LayoutResizeMemory()
        val marked =
            blankLayout(listOf(2, 2)) + (
                KeyPosition(1, 0) to
                    blankKeyMapping().copy(
                        intents = mapOf(Zone.Center to KeyIntent.Text("row")),
                    )
            )
        val withoutRow = marked.resizeRows(listOf(2), memory)
        assertEquals(2, withoutRow.size)
        val withRow = withoutRow.resizeRows(listOf(2, 2), memory)
        assertEquals(
            KeyIntent.Text("row"),
            withRow.getValue(KeyPosition(1, 0)).intents[Zone.Center],
        )
    }

    @Test
    fun `registry register and unregister`() {
        LayoutRegistry.register(
            blankNamedLayout("user_reg", "Reg", listOf(1)),
        )
        assertEquals("Reg", LayoutRegistry.byId("user_reg").title)
        LayoutRegistry.unregister("user_reg")
        // Unknown id falls back to default, not the removed user layout.
        assertEquals(LayoutRegistry.DEFAULT_ID, LayoutRegistry.byId("user_reg").id)
    }
}
