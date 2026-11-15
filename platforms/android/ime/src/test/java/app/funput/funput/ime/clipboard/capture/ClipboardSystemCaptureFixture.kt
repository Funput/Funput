package app.funput.funput.ime.clipboard.capture

import app.funput.funput.ime.clipboard.persistence.ClipboardHistoryStore
import app.funput.funput.ime.clipboard.platform.ClipboardContentKind
import app.funput.funput.ime.clipboard.platform.ClipboardGateway
import app.funput.funput.ime.clipboard.platform.ClipboardObservation
import app.funput.funput.ime.clipboard.platform.ClipboardReadResult
import app.funput.funput.ime.clipboard.platform.ClipboardSnapshot
import app.funput.funput.ime.settings.ClipboardPreferences
import java.nio.file.Files
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

internal class ClipboardSystemCaptureFixture(
    capture: Boolean,
    sensitive: Boolean = false,
    ioDispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
) {
    val preferences = MutableStateFlow(
        ClipboardPreferences.Default.copy(captureSystemCopies = capture),
    )
    val gateway = CaptureGateway(sensitive)
    private val directory = Files.createTempDirectory("clipboard-capture").toFile()
    val store = ClipboardHistoryStore(directory)
    var recordedCount = 0
    val capture = ClipboardSystemCapture(
        parentScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        preferences = preferences,
        gateway = gateway,
        storeFactory = { ClipboardHistoryStore(directory, it) },
        onRecorded = { recordedCount += 1 },
        ioDispatcher = ioDispatcher,
    )
}

internal class CaptureGateway(sensitive: Boolean) : ClipboardGateway {
    var readResult: ClipboardReadResult = ClipboardReadResult.Success(
        text = "hello",
        kind = ClipboardContentKind.TEXT,
        isSensitive = sensitive,
        sourceToken = "token:1",
    )
    var observing = false
    var blockRead: (() -> Unit)? = null
    private var changed: (() -> Unit)? = null

    override fun snapshot() = ClipboardSnapshot(ClipboardContentKind.TEXT, false, "token:1")
    override fun readText(maxLength: Int): ClipboardReadResult {
        blockRead?.invoke()
        return readResult
    }

    override fun observe(onChanged: () -> Unit) = ClipboardObservation {
        observing = false
        changed = null
    }.also {
        observing = true
        changed = onChanged
    }

    fun notifyChanged() = changed?.invoke()
}
