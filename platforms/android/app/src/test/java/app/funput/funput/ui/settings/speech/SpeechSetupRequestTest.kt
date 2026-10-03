package app.funput.funput.ui.settings.speech

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.navigation.SpeechSetupReason
import app.funput.funput.ime.speech.preparation.navigation.SpeechSetupRequest
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK])
class SpeechSetupRequestTest {
    @Test fun requestTargetsOnlyThePrivatePreparationHost() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val request = SpeechSetupRequest(SpeechSetupReason.MODEL, SpeechLocale.EN)
        val intent = request.intent(context)
        assertEquals(context.packageName + ".speech.SpeechSetupActivity", intent.component?.className)
        assertEquals(request, SpeechSetupRequest.read(intent))
        assertNull(intent.action)
    }
    @Test fun invalidExtrasCannotCreateRecordingOrSubstituteLanguage() {
        val request = SpeechSetupRequest.read(Intent().putExtra("speech_setup_reason", "RECORD")
            .putExtra("speech_setup_locale", "auto"))
        assertEquals(SpeechSetupReason.PERMISSION, request.reason)
        assertNull(request.locale)
    }
}
