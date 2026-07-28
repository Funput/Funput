package app.funput.funput.keyboard.popover.interaction

import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.popover.model.VietnameseKeyAlternates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternateSelectionControllerTest {
    private val scheduler = Scheduler()
    private val selected = mutableListOf<String>()
    private val key = KeySpec(
        id = "character-a",
        label = "a",
        role = KeyRole.CHARACTER,
        alternates = VietnameseKeyAlternates.valuesFor('a'),
    )
    private val controller = AlternateSelectionController(
        keyBounds = { KeyBounds(102f, 198f, 138f, 242f) },
        surfaceBounds = { KeyBounds(0f, 0f, 390f, 304f) },
        schedule = scheduler::schedule,
        cancel = scheduler::cancel,
        touchSlop = 8f,
        density = 1f,
        onCaptured = {},
        onFeedback = {},
        onChanged = {},
        onSelected = { _, alternate -> selected += alternate.text },
    )

    @Test
    fun `hold release on source selects base character`() {
        startAndActivate()
        assertTrue(controller.finish(1, 120f, 220f))
        assertEquals(listOf("a"), selected)
    }

    @Test
    fun `drag selects alternate and outside release cancels`() {
        startAndActivate()
        val second = requireNotNull(controller.preview).layout.itemBounds[1]
        controller.move(1, null, second.centerX, second.centerY)
        controller.finish(1, second.centerX, second.centerY)
        assertEquals(listOf("á"), selected)

        startAndActivate()
        assertTrue(controller.finish(1, 389f, 303f))
        assertEquals(listOf("á"), selected)
    }

    @Test
    fun `early drag cancels hold and preserves regular release`() {
        controller.start(1, key, 120f, 220f)
        controller.move(1, key.id, 140f, 220f)
        scheduler.run()
        assertNull(controller.preview)
        assertFalse(controller.finish(1, 140f, 220f))
    }

    @Test
    fun `captured pointer does not capture another pointer`() {
        startAndActivate()
        controller.start(2, key, 120f, 220f)

        assertTrue(controller.isCaptured(1))
        assertFalse(controller.isCaptured(2))
        assertFalse(controller.finish(2, 120f, 220f))
        assertTrue(controller.isCaptured(1))
    }

    @Test
    fun `cancel clears active selection without committing`() {
        startAndActivate()
        controller.cancelAll()

        assertNull(controller.preview)
        assertFalse(controller.finish(1, 120f, 220f))
        assertTrue(selected.isEmpty())
    }

    private fun startAndActivate() {
        controller.start(1, key, 120f, 220f)
        scheduler.run()
    }

    private class Scheduler {
        private var task: Runnable? = null
        fun schedule(value: Runnable, delay: Long) {
            assertEquals(350L, delay)
            task = value
        }
        fun cancel(value: Runnable) {
            if (task == value) task = null
        }
        fun run() = task?.also { task = null }?.run()
    }
}
