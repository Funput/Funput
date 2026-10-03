package app.funput.funput.ime.speech.preparation.operations

import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.platform.SpeechMainQueue
import app.funput.funput.ime.speech.platform.SpeechRequestFactory
import app.funput.funput.ime.speech.preparation.SpeechAvailability
import app.funput.funput.ime.speech.preparation.SpeechCapability
import app.funput.funput.ime.speech.preparation.SpeechCapabilityCache
import app.funput.funput.ime.speech.preparation.SpeechCapabilityClassifier
import app.funput.funput.ime.speech.preparation.platform.PreparationClientFactory
import app.funput.funput.ime.speech.preparation.platform.PreparationTime

internal class CheckSpeechOperation(
    factory: PreparationClientFactory,
    main: SpeechMainQueue,
    time: PreparationTime,
    private val locale: SpeechLocale,
    private val cache: SpeechCapabilityCache,
    private val availability: () -> SpeechAvailability,
    listener: (SpeechCapability) -> Unit,
) : PreparationLease<SpeechCapability>(factory, main, time, listener) {
    override fun startOnMain() {
        when (availability()) {
            SpeechAvailability.UNAVAILABLE, SpeechAvailability.UNSUPPORTED_OS -> return deliver(SpeechCapability.UNSUPPORTED)
            SpeechAvailability.UNKNOWN -> return deliver(SpeechCapability.UNKNOWN)
            SpeechAvailability.AVAILABLE -> Unit
        }
        if (factory.apiLevel < 33) return deliver(SpeechCapability.UNKNOWN)
        cache.get(locale)?.let { return deliver(it) }
        val revision = cache.revision(locale)
        acquire(3_000, { SpeechCapability.UNKNOWN }) { client ->
            client.check(SpeechRequestFactory.create(locale)) { response -> receive {
                val value = SpeechCapabilityClassifier.classify(locale, response)
                cache.put(locale, value, revision)
                deliver(value)
            } }
        }
    }
}
