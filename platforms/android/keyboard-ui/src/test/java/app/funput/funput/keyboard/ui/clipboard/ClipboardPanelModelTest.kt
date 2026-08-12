package app.funput.funput.keyboard.ui.clipboard

import app.funput.funput.keyboard.ui.clipboard.row.shouldRevealClipboardAction
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardPanelModelTest {
    @Test
    fun `groups pinned first and sorts newest first`() {
        val old = entry("old", 1, false)
        val pinned = entry("pin", 2, true)
        val newest = entry("new", 3, false)
        val groups = clipboardGroups(listOf(old, newest, pinned))

        assertEquals(listOf(true, false), groups.map(ClipboardHistoryGroup::pinned))
        assertEquals(listOf(pinned), groups[0].entries)
        assertEquals(listOf(newest, old), groups[1].entries)
    }

    @Test
    fun `preview preserves unicode and collapses whitespace without splitting code points`() {
        assertEquals("Việt 😀 text", ClipboardRowText.preview("  Việt\n\t😀  text "))
        val long = "😀".repeat(121)
        assertEquals(121, ClipboardRowText.preview(long).codePointCount(0, ClipboardRowText.preview(long).length))
        assertEquals("…", ClipboardRowText.preview(long).takeLast(1))
    }

    @Test
    fun `relative time follows minute hour and day boundaries`() {
        val now = Instant.ofEpochSecond(200_000)
        val strings = ClipboardTimeStrings("now", { "$it m" }, { "$it h" }, { "$it d" })
        assertEquals("now", ClipboardRowText.relativeTime(now.minusSeconds(59), now, strings))
        assertEquals("1 m", ClipboardRowText.relativeTime(now.minusSeconds(60), now, strings))
        assertEquals("1 h", ClipboardRowText.relativeTime(now.minusSeconds(3_600), now, strings))
        assertEquals("1 d", ClipboardRowText.relativeTime(now.minusSeconds(86_400), now, strings))
    }

    @Test
    fun `swipe settles open without treating a partial swipe as deletion`() {
        assertEquals(false, shouldRevealClipboardAction(-43f, 88f))
        assertEquals(true, shouldRevealClipboardAction(-44f, 88f))
        assertEquals(true, shouldRevealClipboardAction(-88f, 88f))
    }

    private fun entry(text: String, second: Long, pinned: Boolean) = KeyboardClipboardEntry(
        UUID.nameUUIDFromBytes(text.toByteArray()), text, Instant.ofEpochSecond(second), pinned,
    )
}
