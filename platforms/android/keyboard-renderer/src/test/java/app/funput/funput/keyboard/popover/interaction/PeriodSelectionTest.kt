package app.funput.funput.keyboard.popover.interaction

import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.keys.periodKey
import app.funput.funput.keyboard.popover.model.KeyAlternate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodSelectionTest {
    private val key = periodKey("period")
    private val source = KeyBounds(270f, 200f, 300f, 240f)
    private var pending: Runnable? = null
    private val selected = mutableListOf<String>()
    private val controller = AlternateSelectionController(
        keyBounds = { source },
        surfaceBounds = { KeyBounds(0f, -400f, 320f, 304f) },
        schedule = { task, _ -> pending = task },
        cancel = { if (pending == it) pending = null },
        touchSlop = 8f,
        density = 1f,
        onCaptured = {},
        onFeedback = {},
        onChanged = {},
        onSelected = { _, alternate -> selected += (alternate as KeyAlternate.Text).text },
    )

    @Test
    fun `hold and release on period commits first alternate`() {
        activate()

        assertEquals(0, controller.preview?.selectedIndex)
        assertTrue(controller.finish(1, source.centerX, source.centerY))
        assertEquals(listOf("@"), selected)
    }

    @Test
    fun `dragging to another cell commits that symbol`() {
        activate()
        val at = requireNotNull(controller.preview).layout.itemBounds.first()
        controller.move(1, null, at.centerX, at.centerY)

        assertEquals(0, controller.preview?.selectedIndex)
        assertTrue(controller.finish(1, at.centerX, at.centerY))
        assertEquals(listOf("@"), selected)
    }

    private fun activate() {
        controller.start(1, key, source.centerX, source.centerY)
        requireNotNull(pending).run()
    }
}
