package app.funput.funput.keyboard.ui.emoji

import app.funput.funput.keyboard.model.KeyRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EmojiSearchKeyboardLayoutTest {
    @Test fun `local keyboard exposes expected reusable actions`() {
        val keys = EmojiSearchKeyboardLayout.layout.rows.flatMap { it.keys }
        assertEquals(4, EmojiSearchKeyboardLayout.layout.rows.size)
        assertNotNull(keys.singleOrNull { it.role == KeyRole.SHIFT })
        assertNotNull(keys.singleOrNull { it.role == KeyRole.BACKSPACE })
        assertEquals("Tìm emoji", keys.single { it.role == KeyRole.SPACE }.spaceLabelOverride)
        assertEquals("Xong", keys.single { it.role == KeyRole.ENTER }.label)
    }
}
