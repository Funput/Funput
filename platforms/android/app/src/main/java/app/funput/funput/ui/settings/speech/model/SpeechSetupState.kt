package app.funput.funput.ui.settings.speech.model

import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.SpeechAvailability
import app.funput.funput.ime.speech.preparation.SpeechCapability
import app.funput.funput.ime.speech.preparation.SpeechDownloadEvent

internal data class SpeechLocaleState(
    val capability: SpeechCapability = SpeechCapability.UNKNOWN,
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val download: SpeechDownloadEvent? = null,
)

internal data class SpeechSetupState(
    val featureAvailable: Boolean = true,
    val availability: SpeechAvailability = SpeechAvailability.UNKNOWN,
    val permissionGranted: Boolean = false,
    val enabled: Boolean = true,
    val saving: Boolean = false,
    val saveError: Boolean = false,
    val locales: Map<SpeechLocale, SpeechLocaleState> = SpeechLocale.entries.associateWith { SpeechLocaleState() },
)
