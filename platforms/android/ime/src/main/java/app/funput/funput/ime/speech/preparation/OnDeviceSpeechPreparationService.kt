package app.funput.funput.ime.speech.preparation

import android.content.Context
import androidx.annotation.MainThread
import app.funput.funput.ime.speech.mlkit.MlKitPreparationClientFactory
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.platform.AndroidSpeechMainQueue
import app.funput.funput.ime.speech.platform.SpeechMainQueue
import app.funput.funput.ime.speech.preparation.operations.CheckSpeechOperation
import app.funput.funput.ime.speech.preparation.operations.DownloadSpeechOperation
import app.funput.funput.ime.speech.preparation.platform.AndroidPreparationTime
import app.funput.funput.ime.speech.preparation.platform.PreparationClientFactory
import app.funput.funput.ime.speech.preparation.platform.PreparationTime

/** Preparation facade with no path to recording or an online recognizer. */
class OnDeviceSpeechPreparationService internal constructor(
    private val factory: PreparationClientFactory,
    private val main: SpeechMainQueue,
    private val time: PreparationTime,
) : SpeechPreparationService {
    constructor(context: Context) : this(context, AndroidSpeechMainQueue())
    private constructor(context: Context, main: SpeechMainQueue) :
        this(MlKitPreparationClientFactory(), main, AndroidPreparationTime())
    private val cache = SpeechCapabilityCache(time)

    override fun invalidate(locale: SpeechLocale?) = main.dispatch { cache.invalidate(locale) }

    @MainThread
    override fun availability(): SpeechAvailability = when {
        factory.apiLevel < 31 -> SpeechAvailability.UNSUPPORTED_OS
        else -> runCatching {
            if (factory.isOnDeviceAvailable()) SpeechAvailability.AVAILABLE else SpeechAvailability.UNAVAILABLE
        }.getOrDefault(SpeechAvailability.UNKNOWN)
    }

    override fun check(locale: SpeechLocale, listener: (SpeechCapability) -> Unit): SpeechPreparationOperation =
        CheckSpeechOperation(factory, main, time, locale, cache, ::availability, listener)

    override fun download(locale: SpeechLocale, listener: (SpeechDownloadEvent) -> Unit): SpeechPreparationOperation =
        DownloadSpeechOperation(factory, main, time, locale, cache, ::availability, listener)
}
