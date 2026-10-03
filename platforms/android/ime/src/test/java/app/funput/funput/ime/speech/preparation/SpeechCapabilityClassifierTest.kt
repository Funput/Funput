package app.funput.funput.ime.speech.preparation

import android.speech.SpeechRecognizer
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.platform.SpeechSupportResult
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechCapabilityClassifierTest {
    private fun classify(installed: List<String> = emptyList(), pending: List<String> = emptyList(),
        supported: List<String> = emptyList()) = SpeechCapabilityClassifier.classify(SpeechLocale.VI,
            SpeechSupportResult.Languages(installed, pending, supported))

    @Test fun installedWinsOverPendingAndSupported() {
        assertEquals(SpeechCapability.READY, classify(listOf("VI-vn"), listOf("vi-VN"), listOf("vi-VN")))
    }
    @Test fun pendingWinsOverDownloadable() {
        assertEquals(SpeechCapability.PENDING, classify(pending = listOf("vi-VN"), supported = listOf("vi-VN")))
    }
    @Test fun downloadableRequiresExactLocaleTag() {
        assertEquals(SpeechCapability.DOWNLOADABLE, classify(supported = listOf("vi-VN")))
        assertEquals(SpeechCapability.UNSUPPORTED, classify(supported = listOf("vi")))
        assertEquals(SpeechCapability.UNSUPPORTED, classify(supported = listOf("en-US")))
    }
    @Test fun englishVariantsAreNotSubstituted() {
        assertEquals(SpeechCapability.UNSUPPORTED, SpeechCapabilityClassifier.classify(SpeechLocale.EN,
            SpeechSupportResult.Languages(listOf("en", "en-GB"), emptyList(), emptyList())))
    }
    @Test fun emptyOfflineListsCannotBecomeReady() {
        assertEquals(SpeechCapability.UNSUPPORTED, classify())
    }
    @Test fun unsupportedErrorDiffersFromCannotCheckAndTemporaryLanguageFailure() {
        listOf(SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, SpeechRecognizer.ERROR_SERVER).forEach {
            assertEquals(SpeechCapability.UNKNOWN,
                SpeechCapabilityClassifier.classify(SpeechLocale.VI, SpeechSupportResult.Failure(it)))
        }
        assertEquals(SpeechCapability.UNSUPPORTED, SpeechCapabilityClassifier.classify(SpeechLocale.VI,
            SpeechSupportResult.Failure(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED)))
    }
}
