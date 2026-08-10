package app.funput.funput.ime.clipboard.persistence

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.funput.funput.ime.clipboard.model.ClipboardEntry
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClipboardHistoryLocationInstrumentedTest {
    @Test
    fun storeUsesNoBackupDirectoryAndPersistsInsideSandbox() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expected = context.noBackupFilesDir.resolve("Clipboard").canonicalFile
        assertEquals(expected, ClipboardHistoryStore.directory(context).canonicalFile)
        val store = ClipboardHistoryStore.from(context)
        store.clear()
        val now = Instant.now()
        store.record(ClipboardEntry(text = "xin chào", capturedAt = now, sourceToken = "test"), now)

        assertEquals("xin chào", ClipboardHistoryStore.from(context).load(now).single().text)
        store.clear()
        assertTrue(store.load(now).isEmpty())
    }
}
