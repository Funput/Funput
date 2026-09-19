package app.funput.funput.shortcuts.persistence

import app.funput.funput.shortcuts.model.ShortcutLibrary
import app.funput.funput.shortcuts.model.TextShortcut
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FileShortcutsStoreTest {
    @Test fun `missing file returns defaults without creating it`() = withDirectory { directory ->
        assertEquals(ShortcutLibrary(), FileShortcutsStore(directory).load())
        assertFalse(directory.resolve("shortcuts.json").exists())
    }

    @Test fun `round trip retains order unicode whitespace and options`() = withDirectory { directory ->
        val expected = ShortcutLibrary(
            entries = listOf(
                TextShortcut(trigger = "vn", expansion = " việt nam\n👨‍👩‍👧‍👦 "),
                TextShortcut(trigger = "VN", expansion = "Đặng 👋🏽".repeat(1_000)),
            ),
            isEnabled = false,
            smartCase = false,
            inEnglish = false,
        )
        FileShortcutsStore(directory).save(expected)
        assertEquals(expected, FileShortcutsStore(directory).load())
        assertEquals(listOf("shortcuts.json"), directory.list()?.toList())
    }

    @Test fun `failed atomic replacement leaves the old document intact`() = withDirectory { directory ->
        val original = ShortcutLibrary(entries = listOf(TextShortcut(trigger = "vn", expansion = "cũ")))
        FileShortcutsStore(directory).save(original)
        val bytes = directory.resolve("shortcuts.json").readBytes()
        val failing = FileShortcutsStore(directory) { _, _ -> error("full") }

        expect<ShortcutsStorageError.WriteFailed> { failing.save(ShortcutLibrary()) }
        assertEquals(bytes.toList(), directory.resolve("shortcuts.json").readBytes().toList())
        assertEquals(original, FileShortcutsStore(directory).load())
    }

    @Test fun `store instances serialize access to the same path`() = withDirectory { directory ->
        val values = listOf("one", "two").map { value ->
            ShortcutLibrary(entries = listOf(TextShortcut(trigger = value, expansion = value)))
        }
        val ready = CountDownLatch(values.size)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(values.size)
        values.forEach { value -> pool.submit {
            ready.countDown(); start.await(); FileShortcutsStore(directory).save(value)
        } }
        ready.await(2, TimeUnit.SECONDS)
        start.countDown()
        pool.shutdown()
        assert(pool.awaitTermination(5, TimeUnit.SECONDS))

        assert(FileShortcutsStore(directory).load() in values)
    }

    private fun withDirectory(test: (File) -> Unit) {
        val directory = createTempDirectory("shortcuts-").toFile()
        try { test(directory) } finally { directory.deleteRecursively() }
    }

    private inline fun <reified T : Throwable> expect(block: () -> Unit) {
        try { block(); throw AssertionError("Expected ${T::class.java.simpleName}") }
        catch (error: Throwable) { if (error !is T) throw error }
    }
}
