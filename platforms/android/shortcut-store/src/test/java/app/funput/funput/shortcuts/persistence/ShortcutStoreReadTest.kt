package app.funput.funput.shortcuts.persistence

import app.funput.funput.shortcuts.model.ShortcutLibrary
import app.funput.funput.shortcuts.model.TextShortcut
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutStoreReadTest {
    @Test fun `corrupt incomplete and unsupported documents are not overwritten`() =
        withDirectory { directory ->
            listOf(
                "not-json" to ShortcutsStorageError.InvalidData,
                "{\"schemaVersion\":99}" to ShortcutsStorageError.UnsupportedVersion,
                "{\"schemaVersion\":1}" to ShortcutsStorageError.InvalidData,
            ).forEach { (content, expected) ->
                val file = directory.resolve("shortcuts.json").apply { writeText(content) }
                val store = FileShortcutsStore(directory)
                assertFailure(expected) { store.load() }
                assertFailure(expected) { store.save(ShortcutLibrary()) }
                assertEquals(content, file.readText())
            }
        }

    @Test fun `duplicate triggers on disk are invalid data`() = withDirectory { directory ->
        val one = TextShortcut(trigger = "vn", expansion = "one")
        val two = TextShortcut(trigger = "vn", expansion = "two")
        directory.resolve("shortcuts.json").writeText(ShortcutLibraryJson.encode(
            ShortcutLibrary(entries = listOf(one, two)),
        ))
        assertFailure(ShortcutsStorageError.InvalidData) { FileShortcutsStore(directory).load() }
    }

    @Test fun `unavailable path is a read failure`() = withDirectory { directory ->
        directory.resolve("shortcuts.json").mkdirs()
        assertFailure(ShortcutsStorageError.ReadFailed) { FileShortcutsStore(directory).load() }
    }

    private fun withDirectory(test: (File) -> Unit) {
        val directory = createTempDirectory("shortcuts-read-").toFile()
        try { test(directory) } finally { directory.deleteRecursively() }
    }

    private fun assertFailure(expected: ShortcutsStorageError, block: () -> Unit) {
        try { block(); throw AssertionError("Expected failure") }
        catch (error: Throwable) {
            if (error::class != expected::class) throw error
        }
    }
}
