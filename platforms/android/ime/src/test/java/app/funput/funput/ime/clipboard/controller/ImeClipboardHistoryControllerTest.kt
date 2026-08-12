package app.funput.funput.ime.clipboard.controller

import app.funput.funput.ime.clipboard.model.ClipboardEntry
import app.funput.funput.ime.clipboard.persistence.ClipboardHistoryStore
import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.keyboard.model.KeyboardEditorMode
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeClipboardHistoryControllerTest {
    @Test
    fun `open paste pin remove and clear use persisted history`() {
        val fixture = fixture()
        val first = entry(" Việt,\\\n ", "one")
        val second = entry("second", "two")
        fixture.store.record(first)
        fixture.store.record(second)

        fixture.controller.start(KeyboardEditorMode.TEXT)
        fixture.controller.open()
        assertEquals(listOf(second.id, first.id), fixture.controller.state.value.entries.map { it.id })
        assertEquals(1, fixture.prepared)

        fixture.controller.paste(first.id)
        assertEquals(listOf(first.text), fixture.committed)
        fixture.controller.togglePin(first.id, true)
        assertTrue(fixture.controller.state.value.entries.first { it.id == first.id }.isPinned)
        fixture.controller.remove(second.id)
        assertEquals(listOf(first.id), fixture.controller.state.value.entries.map { it.id })
        fixture.controller.clear()
        assertTrue(fixture.controller.state.value.entries.isEmpty())
        assertNull(fixture.store.lastCapturedSourceToken())
        assertEquals(1, fixture.cleared)
        fixture.close()
    }

    @Test
    fun `blocked modes and disabled preference never expose history`() {
        val prefs = MutableStateFlow(ClipboardPreferences.Default)
        val fixture = fixture(prefs)
        fixture.controller.start(KeyboardEditorMode.PASSWORD)
        fixture.controller.open()
        assertFalse(fixture.controller.state.value.available)
        prefs.value = ClipboardPreferences.Default.copy(enabled = false)
        fixture.controller.start(KeyboardEditorMode.TEXT)
        assertFalse(fixture.controller.state.value.available)
        fixture.close()
    }

    private fun fixture(
        prefs: MutableStateFlow<ClipboardPreferences> = MutableStateFlow(ClipboardPreferences.Default),
    ): Fixture {
        val directory = Files.createTempDirectory("history-controller").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val store = ClipboardHistoryStore(directory)
        lateinit var fixture: Fixture
        val controller = ImeClipboardHistoryController(
            scope, prefs, { ClipboardHistoryStore(directory, it) },
            { fixture.committed += it }, {}, { fixture.prepared += 1 },
            { fixture.cleared += 1 }, Dispatchers.Unconfined,
        )
        fixture = Fixture(controller, store, scope)
        return fixture
    }

    private fun entry(text: String, token: String) = ClipboardEntry(text = text, sourceToken = token)

    private data class Fixture(
        val controller: ImeClipboardHistoryController,
        val store: ClipboardHistoryStore,
        val scope: CoroutineScope,
        val committed: MutableList<String> = mutableListOf(),
        var prepared: Int = 0,
        var cleared: Int = 0,
    ) {
        fun close() { controller.close() }
    }
}
