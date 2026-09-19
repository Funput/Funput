package app.funput.funput.shortcuts.model

import app.funput.funput.shortcuts.persistence.ShortcutsStorageError
import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutLibraryTest {
    @Test fun `validation preserves exact case-sensitive triggers`() {
        ShortcutLibrary(entries = listOf(
            TextShortcut(trigger = "vn", expansion = "việt nam"),
            TextShortcut(trigger = "VN", expansion = "VIỆT NAM"),
        )).validate()
    }

    @Test fun `empty fields duplicate IDs and duplicate triggers are rejected`() {
        expect<ShortcutsStorageError.InvalidData> {
            ShortcutLibrary(entries = listOf(TextShortcut(trigger = " \n", expansion = "ok"))).validate()
        }
        val id = UUID.randomUUID()
        expect<ShortcutsStorageError.InvalidData> {
            ShortcutLibrary(entries = listOf(
                TextShortcut(id, "a", "one"), TextShortcut(id, "b", "two"),
            )).validate()
        }
        expect<ShortcutsStorageError.DuplicateTrigger> {
            ShortcutLibrary(entries = listOf(
                TextShortcut(trigger = "a", expansion = "one"),
                TextShortcut(trigger = "a", expansion = "two"),
            )).validate()
        }
    }

    @Test fun `duplicate lookup ignores the edited identity`() {
        val original = TextShortcut(trigger = "vn", expansion = "việt nam")
        val library = ShortcutLibrary(entries = listOf(original))
        assertFalse(library.isDuplicate(original.copy(expansion = "Việt Nam")))
        assertTrue(library.isDuplicate(TextShortcut(trigger = "vn", expansion = "khác")))
    }

    private inline fun <reified T : Throwable> expect(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected ${T::class.java.simpleName}")
        } catch (error: Throwable) {
            if (error !is T) throw error
        }
    }
}
