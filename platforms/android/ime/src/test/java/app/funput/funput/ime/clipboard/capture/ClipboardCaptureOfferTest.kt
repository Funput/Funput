package app.funput.funput.ime.clipboard.capture

import app.funput.funput.ime.clipboard.controller.ImeClipboardController
import app.funput.funput.ime.clipboard.controller.ImeClipboardHistoryController
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** System capture and the paste offer working on one history, as they do in the IME. */
class ClipboardCaptureOfferTest {
    @Test
    fun `a copy recorded while the keyboard was hidden is offered with its text`() {
        val fixture = CaptureOfferFixture()
        fixture.copy("xin chào", "android:v1:timestamp:7")
        val readsByCapture = fixture.gateway.readCount

        fixture.controller.start(KeyboardEditorMode.TEXT)

        assertEquals("xin chào", fixture.controller.offer.value?.preview)
        // The preview comes from history: building the offer reads no clipboard content.
        assertEquals(readsByCapture, fixture.gateway.readCount)
    }

    @Test
    fun `a copy made with the keyboard open gains its preview once recorded`() {
        val fixture = CaptureOfferFixture()
        fixture.controller.start(KeyboardEditorMode.TEXT)

        fixture.copy("vừa sao chép", "android:v1:timestamp:8")

        assertEquals("vừa sao chép", fixture.controller.offer.value?.preview)
    }

    @Test
    fun `pasting consumes the offer for later keyboard sessions`() {
        val fixture = CaptureOfferFixture()
        fixture.copy("xin chào", "android:v1:timestamp:7")
        fixture.controller.start(KeyboardEditorMode.TEXT)
        fixture.controller.pasteCurrent()
        fixture.controller.stop()

        fixture.controller.start(KeyboardEditorMode.TEXT)

        assertEquals(listOf("xin chào"), fixture.committed)
        assertNull(fixture.controller.offer.value)
    }

    @Test
    fun `with recording off the offer is generic and nothing is read`() {
        val fixture = CaptureOfferFixture(capture = false)
        fixture.copy("xin chào", "android:v1:timestamp:7")

        fixture.controller.start(KeyboardEditorMode.TEXT)

        assertNotNull(fixture.controller.offer.value)
        assertNull(fixture.controller.offer.value?.preview)
        assertEquals(0, fixture.gateway.readCount)
    }

    @Test
    fun `deleting the entry from history withdraws its preview`() {
        val fixture = CaptureOfferFixture()
        fixture.copy("xin chào", "android:v1:timestamp:7")
        fixture.controller.start(KeyboardEditorMode.TEXT)
        fixture.history.start(KeyboardEditorMode.TEXT)
        fixture.history.open()

        fixture.history.remove(fixture.history.state.value.entries.single().id)

        assertNotNull(fixture.controller.offer.value)
        assertNull(fixture.controller.offer.value?.preview)
    }
}

private class CaptureOfferFixture(capture: Boolean = true) {
    val gateway = SharedGateway()
    val committed = mutableListOf<String>()
    private val directory = Files.createTempDirectory("clipboard-capture-offer").toFile()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val preferences = MutableStateFlow(ClipboardPreferences.Default.copy(captureSystemCopies = capture))
    private val storeFactory = { expiry: ClipboardExpiry -> ClipboardHistoryStore(directory, expiry) }
    val controller = ImeClipboardController(
        scope, preferences, gateway, storeFactory, committed::add, {}, Dispatchers.Unconfined,
    )
    val history = ImeClipboardHistoryController(
        scope, preferences, storeFactory, {}, {}, {}, controller::historyChanged, Dispatchers.Unconfined,
    )
    private val systemCapture = ClipboardSystemCapture(
        scope, preferences, gateway, storeFactory,
        onRecorded = { controller.historyChanged() }, ioDispatcher = Dispatchers.Unconfined,
    )

    fun copy(text: String, token: String) {
        gateway.put(text, token)
        gateway.notifyChanged()
    }
}

/** A clipboard both the capture and the offer listen to, like the real one. */
private class SharedGateway : ClipboardGateway {
    private val listeners = mutableListOf<() -> Unit>()
    private var current: ClipboardSnapshot? = null
    private var content: ClipboardReadResult = ClipboardReadResult.Empty
    var readCount = 0

    fun put(text: String, token: String) {
        current = ClipboardSnapshot(ClipboardContentKind.TEXT, false, token)
        content = ClipboardReadResult.Success(text, ClipboardContentKind.TEXT, false, token)
    }

    fun notifyChanged() = listeners.toList().forEach { it() }

    override fun snapshot() = current
    override fun readText(maxLength: Int) = content.also { readCount += 1 }
    override fun observe(onChanged: () -> Unit): ClipboardObservation {
        listeners += onChanged
        return ClipboardObservation { listeners -= onChanged }
    }
}
