package app.funput.funput.ime.speech.preparation

import app.funput.funput.ime.speech.platform.SpeechMainQueue
import app.funput.funput.ime.speech.platform.SpeechRequest
import app.funput.funput.ime.speech.preparation.platform.*

internal class PreparationFixture(api: Int = 34) {
    val factory = FakePreparationFactory(api)
    val time = FakePreparationTime()
    val tasks = mutableListOf<() -> Unit>()
    var queued = false
    val main = SpeechMainQueue { if (queued) tasks += it else it() }
    val service = OnDeviceSpeechPreparationService(factory, main, time)
    val client get() = factory.clients.last()
    fun drain() {
        while (tasks.isNotEmpty()) tasks.removeAt(0).invoke()
    }
}

internal class FakePreparationFactory(override val apiLevel: Int) : PreparationClientFactory {
    var available = true
    var probeThrows = false
    var createThrows = false
    var probes = 0
    val clients = mutableListOf<FakePreparationClient>()
    var configure: (FakePreparationClient) -> Unit = {}
    override fun isOnDeviceAvailable(): Boolean {
        probes++
        if (probeThrows) error("probe")
        return available
    }
    override fun create(): PreparationClient {
        if (createThrows) error("create")
        return FakePreparationClient().also { configure(it); clients += it }
    }
}

internal class FakePreparationClient : PreparationClient {
    val requests = mutableListOf<SpeechRequest>()
    var checkListener: (SpeechSupportResult) -> Unit = {}
    var downloadListener: (SpeechDownloadEvent) -> Unit = {}
    var checks = 0
    var trackedDownloads = 0
    var closes = 0
    var onCheck: () -> Unit = {}
    var onDownload: () -> Unit = {}
    var onClose: () -> Unit = {}
    override fun check(request: SpeechRequest, listener: (SpeechSupportResult) -> Unit) {
        checks++
        requests += request
        checkListener = listener
        onCheck()
    }
    override fun trackDownload(request: SpeechRequest, listener: (SpeechDownloadEvent) -> Unit) {
        trackedDownloads++
        requests += request
        downloadListener = listener
        onDownload()
    }
    override fun close() {
        closes++
        onClose()
    }
    fun languages(installed: List<String> = emptyList(), pending: List<String> = emptyList(),
        supported: List<String> = emptyList()) =
        checkListener(SpeechSupportResult.Languages(installed, pending, supported))
}

internal class FakePreparationTime : PreparationTime {
    class Task(val at: Long, val action: () -> Unit) : PreparationCancellation {
        var cancelled = false
        override fun cancel() { cancelled = true }
    }
    var now = 0L
    var inline = false
    val tasks = mutableListOf<Task>()
    override fun nowMillis() = now
    override fun schedule(delayMillis: Long, task: () -> Unit): PreparationCancellation {
        if (inline) task()
        return Task(now + delayMillis, task).also { tasks += it }
    }
    fun advance(millis: Long) {
        now += millis
        tasks.filter { !it.cancelled && it.at <= now }.forEach { it.action() }
    }
}
