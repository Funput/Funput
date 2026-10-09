package app.funput.funput.keyboard.popover.interaction

import app.funput.funput.keyboard.model.KeyAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommaPopoverInteractionTest {
    @Test
    fun `tap types comma and cancels the hold timer`() {
        val subject = CommaPopoverSubject()
        subject.down()
        subject.release()

        assertEquals(listOf(KeyAction.Input("comma", ",")), subject.input)
        assertTrue(subject.pending.isEmpty())
        assertTrue(subject.events.isEmpty())
    }

    @Test
    fun `hold and release at source closes without input or action`() {
        val subject = CommaPopoverSubject()
        subject.down()
        subject.hold()
        assertNull(subject.controller.alternatePreview?.selectedIndex)
        subject.release()

        assertNull(subject.controller.alternatePreview)
        assertTrue(subject.input.isEmpty())
        assertTrue(subject.events.isEmpty())
    }

    @Test
    fun `drag and release invokes each function exactly once without text`() {
        listOf("placement", "settings").forEachIndexed { index, name ->
            val subject = CommaPopoverSubject()
            subject.down()
            subject.hold()
            val cell = requireNotNull(subject.controller.alternatePreview).layout.itemBounds[index]
            subject.controller.onPointerMoved(1, null, cell.centerX, cell.centerY)
            assertEquals(index, subject.controller.alternatePreview?.selectedIndex)
            subject.release(x = cell.centerX, y = cell.centerY, keyId = null)
            subject.release(keyId = null)

            assertEquals(listOf("cancelPointers", name), subject.events)
            assertTrue(subject.input.isEmpty())
        }
    }

    @Test
    fun `outside release and layout reset do not perform functions`() {
        val subject = CommaPopoverSubject()
        subject.down()
        subject.hold()
        subject.release(x = 310f, y = 280f, keyId = null)
        assertNull(subject.controller.alternatePreview)
        subject.down()
        subject.hold()
        subject.controller.reset()
        subject.release(keyId = null)

        assertTrue(subject.input.isEmpty())
        assertTrue(subject.events.isEmpty())
        assertTrue(subject.pending.isEmpty())
    }

    @Test
    fun `function closes other palettes and cancels pending pointers first`() {
        val subject = CommaPopoverSubject()
        subject.down()
        subject.hold()
        val cell = requireNotNull(subject.controller.alternatePreview).layout.itemBounds[0]
        subject.down(2, subject.period)
        subject.hold()
        subject.down(3)
        subject.release(x = cell.centerX, y = cell.centerY, keyId = null)
        subject.release(pointer = 2, keyId = null)
        subject.release(pointer = 3, keyId = null)

        assertEquals(listOf("cancelPointers", "placement"), subject.events)
        assertTrue(subject.input.isEmpty())
        assertTrue(subject.pending.isEmpty())
    }

    @Test
    fun `TalkBack routes functions through the same callbacks`() {
        val subject = CommaPopoverSubject()
        subject.controller.emitAlternate(subject.comma.id, 1)

        assertEquals(listOf("cancelPointers", "settings"), subject.events)
        assertTrue(subject.input.isEmpty())
    }
}
