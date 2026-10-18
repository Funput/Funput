package app.funput.funput.ui.settings.speech

import android.speech.SpeechRecognizer
import app.funput.funput.R
import app.funput.funput.ime.speech.preparation.SpeechCapability
import app.funput.funput.ime.speech.preparation.SpeechDownloadEvent
import app.funput.funput.ime.speech.services.GoogleSpeechServices
import app.funput.funput.ui.settings.speech.model.SpeechLocaleState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechLocaleStatusTest {
    private fun action(state: SpeechLocaleState) = speechLocaleStatus(state).action

    @Test fun eachStateOffersOneClearAction() {
        assertEquals(SpeechLocaleAction.BUSY, action(SpeechLocaleState(checking = true)))
        assertEquals(R.string.speech_installing,
            speechLocaleStatus(SpeechLocaleState(SpeechCapability.PENDING, installing = true)).message)
        assertEquals(SpeechLocaleAction.READY, action(SpeechLocaleState(SpeechCapability.READY)))
        assertEquals(SpeechLocaleAction.DOWNLOAD, action(SpeechLocaleState(SpeechCapability.DOWNLOADABLE)))
        assertEquals(SpeechLocaleAction.BUSY, action(SpeechLocaleState(SpeechCapability.PENDING)))
        assertEquals(SpeechLocaleAction.RETRY_CHECK, action(SpeechLocaleState(SpeechCapability.UNSUPPORTED)))
        assertEquals(SpeechLocaleAction.RETRY_CHECK, action(SpeechLocaleState(SpeechCapability.UNKNOWN)))
        assertEquals(SpeechLocaleAction.BUSY, action(SpeechLocaleState(downloading = true)))
        assertEquals(SpeechLocaleAction.BUSY,
            action(SpeechLocaleState(SpeechCapability.PENDING, download = SpeechDownloadEvent.Scheduled)))
    }

    @Test fun failuresSayWhyAndOnlyNonNetworkOnesPointToTheGuide() {
        val network = speechLocaleStatus(SpeechLocaleState(download = SpeechDownloadEvent.Failure(SpeechRecognizer.ERROR_NETWORK)))
        assertEquals(R.string.speech_network_error, network.message)
        assertEquals(SpeechLocaleAction.RETRY_DOWNLOAD, network.action)
        assertFalse(network.needsGuide)
        val other = speechLocaleStatus(SpeechLocaleState(download = SpeechDownloadEvent.Failure(SpeechRecognizer.ERROR_CLIENT)))
        assertEquals(R.string.speech_download_error, other.message)
        assertTrue(other.needsGuide)
        val missing = speechLocaleStatus(
            SpeechLocaleState(download = SpeechDownloadEvent.Failure(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)))
        assertEquals(SpeechLocaleAction.RETRY_CHECK, missing.action)
        assertTrue(missing.needsGuide)
    }

    @Test fun progressCarriesItsPercent() {
        assertEquals(40, speechLocaleStatus(SpeechLocaleState(downloading = true,
            download = SpeechDownloadEvent.Progress(40))).progress)
    }

    @Test fun guideStepsFollowTheDevice() {
        fun steps(status: GoogleSpeechServices.Status, rom: Boolean) =
            guideSteps(SpeechGuideContext(status, hasPlayStore = true, popupPermissionRom = rom))
        assertEquals(listOf(SpeechGuideStep.INSTALL, SpeechGuideStep.POPUP, SpeechGuideStep.RETURN),
            steps(GoogleSpeechServices.Status.MISSING, rom = true))
        assertEquals(listOf(SpeechGuideStep.ENABLE, SpeechGuideStep.RETURN),
            steps(GoogleSpeechServices.Status.DISABLED, rom = false))
        assertTrue(isPopupPermissionRom("Xiaomi"))
        assertTrue(isPopupPermissionRom("OnePlus"))
        assertFalse(isPopupPermissionRom("samsung"))
    }

    @Test fun voiceStringsAvoidSeparatorsThatReadAsMachineWritten() {
        val text = File("src/main/res/values/speech.xml").readText()
        listOf("·", "—", "–").forEach { assertFalse("speech.xml contains $it", it in text) }
    }
}
