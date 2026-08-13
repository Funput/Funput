package app.funput.funput.ime.clipboard.controller

import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.clipboard.persistence.ClipboardHistoryStore
import app.funput.funput.ime.clipboard.platform.ClipboardContentKind
import app.funput.funput.ime.clipboard.platform.ClipboardGateway
import app.funput.funput.ime.clipboard.platform.ClipboardObservation
import app.funput.funput.ime.clipboard.platform.ClipboardReadResult
import app.funput.funput.ime.clipboard.platform.ClipboardSnapshot
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

class ImeClipboardControllerTest {
    @Test
    fun `listener observes metadata only while active and enabled`() {
        val fixture = Fixture()
        fixture.controller.start(KeyboardEditorMode.TEXT)
        assertTrue(fixture.gateway.observing)
        assertEquals(1, fixture.gateway.snapshotCount)
        assertEquals(0, fixture.gateway.readCount)

        fixture.gateway.notifyChanged()
        assertEquals(2, fixture.gateway.snapshotCount)
        assertEquals(0, fixture.gateway.readCount)
        fixture.preferences.value = ClipboardPreferences(false, ClipboardExpiry.HOUR)
        assertFalse(fixture.gateway.observing)
        assertNull(fixture.controller.offer.value)
    }

    @Test
    fun `explicit paste commits exact text once and records it`() {
        val fixture = Fixture(text = " Việt,\\\nline ")
        fixture.controller.start(KeyboardEditorMode.TEXT)
        val results = mutableListOf<ClipboardPasteResult>()
        fixture.controller.pasteCurrent(results::add)
        fixture.controller.pasteCurrent(results::add)

        assertEquals(listOf(" Việt,\\\nline "), fixture.committed)
        assertEquals(1, fixture.afterCommitCount)
        assertEquals(" Việt,\\\nline ", fixture.store.load().single().text)
        assertEquals(listOf(ClipboardPasteResult.PASTED, ClipboardPasteResult.BLOCKED), results)
    }

    @Test
    fun `sensitive paste commits without persistence`() {
        val fixture = Fixture(sensitive = true)
        fixture.controller.start(KeyboardEditorMode.TEXT)
        fixture.controller.pasteCurrent()
        assertEquals(listOf("clipboard"), fixture.committed)
        assertTrue(fixture.store.load().isEmpty())
        assertNull(fixture.store.lastCapturedSourceToken())
        assertNull(fixture.controller.offer.value)
    }

    @Test
    fun `changed and oversized clips never commit`() {
        val fixture = Fixture()
        fixture.controller.start(KeyboardEditorMode.TEXT)
        fixture.gateway.readResult = fixture.gateway.success(sourceToken = "new-token")
        var result: ClipboardPasteResult? = null
        fixture.controller.pasteCurrent { result = it }
        assertEquals(ClipboardPasteResult.CHANGED, result)
        assertTrue(fixture.committed.isEmpty())

        fixture.gateway.snapshotValue = ClipboardSnapshot(ClipboardContentKind.TEXT, false, "large")
        fixture.gateway.readResult = ClipboardReadResult.TooLarge
        fixture.gateway.notifyChanged()
        fixture.controller.pasteCurrent { result = it }
        assertEquals(ClipboardPasteResult.TOO_LARGE, result)
        assertTrue(fixture.committed.isEmpty())
        assertNull(fixture.controller.offer.value)
    }

    @Test
    fun `read failures do not commit or persist`() {
        val failures = listOf(
            ClipboardReadResult.Empty to ClipboardPasteResult.EMPTY,
            ClipboardReadResult.Unsupported to ClipboardPasteResult.UNSUPPORTED,
            ClipboardReadResult.Unavailable to ClipboardPasteResult.UNAVAILABLE,
        )
        failures.forEach { (read, expected) ->
            val fixture = Fixture()
            fixture.gateway.readResult = read
            fixture.controller.start(KeyboardEditorMode.TEXT)
            var result: ClipboardPasteResult? = null
            fixture.controller.pasteCurrent { result = it }
            assertEquals(expected, result)
            assertTrue(fixture.committed.isEmpty())
            assertTrue(fixture.store.load().isEmpty())
        }
    }
}

internal class Fixture(text: String = "clipboard", sensitive: Boolean = false) {
    val preferences = MutableStateFlow(ClipboardPreferences.Default)
    val gateway = FakeClipboardGateway(text, sensitive)
    private val directory = Files.createTempDirectory("clipboard-controller").toFile()
    val store = ClipboardHistoryStore(directory)
    val committed = mutableListOf<String>()
    var afterCommitCount = 0
    val controller = ImeClipboardController(
        parentScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        preferences = preferences,
        gateway = gateway,
        storeFactory = { ClipboardHistoryStore(directory, it) },
        commitText = committed::add,
        afterCommit = { afterCommitCount += 1 },
        ioDispatcher = Dispatchers.Unconfined,
    )
}

internal class FakeClipboardGateway(text: String, sensitive: Boolean) : ClipboardGateway {
    var snapshotValue = ClipboardSnapshot(ClipboardContentKind.TEXT, sensitive, Token)
    var readResult: ClipboardReadResult = success(text, sensitive)
    var snapshotCount = 0
    var readCount = 0
    var observing = false
    private var changed: (() -> Unit)? = null

    override fun snapshot() = snapshotValue.also { snapshotCount += 1 }
    override fun readText(maxLength: Int) = readResult.also { readCount += 1 }
    override fun observe(onChanged: () -> Unit) = ClipboardObservation {
        observing = false
        changed = null
    }.also {
        observing = true
        changed = onChanged
    }

    fun notifyChanged() = changed?.invoke()
    fun success(
        text: String = "clipboard",
        sensitive: Boolean = false,
        sourceToken: String = Token,
    ) = ClipboardReadResult.Success(text, ClipboardContentKind.TEXT, sensitive, sourceToken)

    private companion object { const val Token = "android:v1:timestamp:1" }
}
