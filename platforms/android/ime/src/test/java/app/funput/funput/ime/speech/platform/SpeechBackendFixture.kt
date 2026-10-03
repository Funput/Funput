package app.funput.funput.ime.speech.platform

import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.ime.speech.model.SpeechLocale

internal class SpeechBackendFixture(queued: Boolean = false) {
    val queue = FakeSpeechQueue(queued)
    val factory = FakeSpeechClientFactory()
    val events = mutableListOf<SpeechEvent>()
    val backend = PlatformSpeechBackend(factory, queue)
    val recording = backend.open(SpeechLocale.VI, events::add)
    val client get() = factory.clients.single()
}

internal class FakeSpeechQueue(private val queued: Boolean) : SpeechMainQueue {
    private val tasks = ArrayDeque<() -> Unit>()

    override fun dispatch(task: () -> Unit) {
        if (queued) tasks.addLast(task) else task()
    }

    fun drain() {
        while (tasks.isNotEmpty()) tasks.removeFirst()()
    }
}

internal class FakeSpeechClientFactory : SpeechClientFactory {
    override var apiLevel = 31
    var available = true
    var availabilityChecks = 0
    var creationError: Exception? = null
    var startEvent: SpeechEvent? = null
    var startError: Exception? = null
    val clients = mutableListOf<FakeSpeechClient>()

    override fun isOnDeviceAvailable(): Boolean {
        availabilityChecks += 1
        return available
    }

    override fun create(listener: (SpeechEvent) -> Unit): SpeechClient {
        creationError?.let { throw it }
        return FakeSpeechClient(listener, startEvent, startError).also(clients::add)
    }
}

internal class FakeSpeechClient(
    private val listener: (SpeechEvent) -> Unit,
    private val startEvent: SpeechEvent?,
    private val startError: Exception?,
) : SpeechClient {
    val requests = mutableListOf<SpeechRequest>()
    var stops = 0
    var cancels = 0
    var destroys = 0
    var stopError: Exception? = null
    var cancelError: Exception? = null
    var destroyError: Exception? = null

    override fun start(request: SpeechRequest) {
        requests += request
        startError?.let { throw it }
        startEvent?.let(listener)
    }

    fun emit(event: SpeechEvent) = listener(event)

    override fun stop() {
        stops += 1
        stopError?.let { throw it }
    }

    override fun cancel() {
        cancels += 1
        cancelError?.let { throw it }
    }

    override fun destroy() {
        destroys += 1
        destroyError?.let { throw it }
    }
}
