package app.funput.funput.ui.settings.speech.model

import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.SpeechAvailability
import app.funput.funput.ime.speech.preparation.SpeechCapability
import app.funput.funput.ime.speech.preparation.SpeechDownloadEvent
import app.funput.funput.ime.speech.services.GoogleSpeechServices

internal data class SpeechLocaleState(
    val capability: SpeechCapability = SpeechCapability.UNKNOWN,
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val download: SpeechDownloadEvent? = null,
    /** Downloaded or confirmed, but Speech Services has not reported the pack installed yet. */
    val installing: Boolean = false,
)

internal data class SpeechSetupState(
    val featureAvailable: Boolean = true,
    val availability: SpeechAvailability = SpeechAvailability.UNKNOWN,
    val permissionGranted: Boolean = false,
    val enabled: Boolean = true,
    val saving: Boolean = false,
    val saveError: Boolean = false,
    /** Whether the Google app that downloads and runs the speech model is usable. */
    val speechServices: GoogleSpeechServices.Status = GoogleSpeechServices.Status.INSTALLED,
    /** A locale whose download Speech Services only scheduled: open its consent dialog. */
    val pendingConsent: SpeechLocale? = null,
    val guideVisible: Boolean = false,
    val locales: Map<SpeechLocale, SpeechLocaleState> = SpeechLocale.entries.associateWith { SpeechLocaleState() },
)
