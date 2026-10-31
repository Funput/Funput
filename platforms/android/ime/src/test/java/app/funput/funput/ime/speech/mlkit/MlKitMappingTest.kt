package app.funput.funput.ime.speech.mlkit

import android.speech.SpeechRecognizer
import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.SpeechCapability
import app.funput.funput.ime.speech.preparation.SpeechCapabilityClassifier
import app.funput.funput.ime.speech.preparation.SpeechDownloadEvent
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse as R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MlKitMappingTest {
    private fun capability(status: Int) =
        SpeechCapabilityClassifier.classify(SpeechLocale.VI, MlKitSpeech.support(status, SpeechLocale.VI.tag))

    @Test fun featureStatusMapsOntoCapability() {
        assertEquals(SpeechCapability.READY, capability(FeatureStatus.AVAILABLE))
        assertEquals(SpeechCapability.PENDING, capability(FeatureStatus.DOWNLOADING))
        assertEquals(SpeechCapability.DOWNLOADABLE, capability(FeatureStatus.DOWNLOADABLE))
        assertEquals(SpeechCapability.UNSUPPORTED, capability(FeatureStatus.UNAVAILABLE))
        assertEquals(SpeechCapability.UNKNOWN, capability(42))
    }

    @Test fun errorsKeepPlatformCodes() {
        fun code(value: Int) = MlKitSpeech.errorCode(GenAiException(RuntimeException(), value))
        assertEquals(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, code(GenAiException.ErrorCode.NOT_AVAILABLE))
        assertEquals(SpeechRecognizer.ERROR_RECOGNIZER_BUSY, code(GenAiException.ErrorCode.BUSY))
        assertEquals(SpeechRecognizer.ERROR_NO_MATCH, code(GenAiException.ErrorCode.REQUEST_TOO_SMALL))
        assertEquals(SpeechRecognizer.ERROR_CLIENT, code(GenAiException.ErrorCode.UNKNOWN))
        assertEquals(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, MlKitSpeech.errorCode(SecurityException()))
        assertEquals(SpeechRecognizer.ERROR_CLIENT, MlKitSpeech.errorCode(IllegalStateException()))
    }

    // 1.0.0-alpha2 declares a default for wordTimings in its Kotlin metadata but ships without the
    // synthetic default constructor, so omitting the argument compiles and then fails at runtime.
    private fun final(text: String) = R.FinalTextResponse(text, emptyList())

    @Test fun responsesBecomeSessionEvents() {
        val mapper = MlKitResponseMapper()
        assertEquals(SpeechEvent.Partial("chiều nay"), mapper.map(R.PartialTextResponse("chiều nay")))
        assertNull(mapper.map(R.AudioLevelResponse(42)))
        assertEquals(SpeechEvent.Final("chiều nay gặp"), mapper.map(final("chiều nay gặp")))
        assertNull(mapper.map(R.CompletedResponse))
    }

    @Test fun blankFinalOrCompletionWithoutFinalIsNoMatch() {
        val noMatch = SpeechEvent.Failure(SpeechRecognizer.ERROR_NO_MATCH)
        assertEquals(noMatch, MlKitResponseMapper().map(final("  ")))
        assertEquals(noMatch, MlKitResponseMapper().map(R.CompletedResponse))
    }

    @Test fun downloadReportsPercentFromStartedTotal() {
        val mapper = MlKitDownloadMapper()
        assertNull(MlKitDownloadMapper().map(DownloadStatus.DownloadProgress(10)))
        assertEquals(SpeechDownloadEvent.Progress(0), mapper.map(DownloadStatus.DownloadStarted(200)))
        assertEquals(SpeechDownloadEvent.Progress(50), mapper.map(DownloadStatus.DownloadProgress(100)))
        assertEquals(SpeechDownloadEvent.Progress(100), mapper.map(DownloadStatus.DownloadProgress(400)))
        assertEquals(SpeechDownloadEvent.Success, mapper.map(DownloadStatus.DownloadCompleted))
        val failed = DownloadStatus.DownloadFailed(GenAiException(RuntimeException(), GenAiException.ErrorCode.UNKNOWN))
        assertEquals(SpeechDownloadEvent.Failure(SpeechRecognizer.ERROR_CLIENT), mapper.map(failed))
    }

    @Test fun downloadFailureKeepsTheReasonFromTheMessage() {
        fun failed(message: String) = MlKitDownloadMapper().map(DownloadStatus.DownloadFailed(
            GenAiException(message, null, GenAiException.ErrorCode.NOT_AVAILABLE)))
        // The exact texts ML Kit 1.0.0-alpha1 builds from Speech Services' end reasons.
        assertEquals(SpeechDownloadEvent.Scheduled,
            failed("Model download is not completed yet and will be retried automatically"))
        assertEquals(SpeechDownloadEvent.Failure(SpeechRecognizer.ERROR_NETWORK),
            failed("Model download failed with reason: END_REASON_NO_INTERNET_CONNECTION"))
        assertEquals(SpeechDownloadEvent.Failure(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE),
            failed("Model download failed with reason: END_REASON_MODEL_UNAVAILABLE"))
        assertEquals(SpeechDownloadEvent.Failure(SpeechRecognizer.ERROR_CLIENT),
            failed("Model download failed with reason: END_REASON_DOWNLOAD_ERROR"))
    }
}
