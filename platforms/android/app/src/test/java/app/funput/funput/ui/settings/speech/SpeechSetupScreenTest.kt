package app.funput.funput.ui.settings.speech

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.SpeechAvailability
import app.funput.funput.ime.speech.preparation.SpeechCapability
import app.funput.funput.ui.kit.layout.FunputScreenListTag
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.speech.model.SpeechLocaleState
import app.funput.funput.ui.settings.speech.model.SpeechSetupState
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class SpeechSetupScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun permissionTapHasNoDownloadSideEffect() {
        var requests = 0
        var downloads = 0
        compose.setContent { FunputUiTheme(false) {
            SpeechSetupScreen(SpeechSetupState(), {}, { requests++ }, {}, { downloads++ }, {})
        } }
        compose.onNodeWithTag("speech-permission").performClick()
        assertEquals(1, requests)
        assertEquals(0, downloads)
    }
    @Test fun downloadableLocaleOnlyDownloadsAfterTap() {
        val downloads = mutableListOf<SpeechLocale>()
        val state = SpeechSetupState(permissionGranted = true, availability = SpeechAvailability.AVAILABLE,
            locales = mapOf(SpeechLocale.VI to SpeechLocaleState(SpeechCapability.DOWNLOADABLE),
                SpeechLocale.EN to SpeechLocaleState(SpeechCapability.UNSUPPORTED)))
        compose.setContent { FunputUiTheme(false) {
            SpeechSetupScreen(state, {}, {}, {}, downloads::add, {})
        } }
        assertEquals(emptyList<SpeechLocale>(), downloads)
        compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(hasTestTag("speech-download-vi-VN"))
        compose.onNodeWithTag("speech-download-vi-VN").performClick()
        assertEquals(listOf(SpeechLocale.VI), downloads)
        compose.onNodeWithTag("speech-download-en-US").assertDoesNotExist()
    }
    @Test fun productionGateRendersAnInertSwitchAndNoPermissionAction() {
        compose.setContent { FunputUiTheme(false) {
            SpeechSetupScreen(SpeechSetupState(featureAvailable = false),
                { error("disabled") }, { error("disabled") }, {}, {}, {})
        } }
        compose.onNodeWithTag("speech-enabled").assertIsNotEnabled()
        compose.onNodeWithTag("speech-permission").assertDoesNotExist()
    }
}
