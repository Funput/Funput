package app.funput.funput.ui.settings.speech

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.SpeechAvailability
import app.funput.funput.ime.speech.preparation.SpeechCapability
import app.funput.funput.ime.speech.services.GoogleSpeechServices
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
        compose.onNodeWithTag("speech-check-vi-VN").assertDoesNotExist()
    }
    @Test fun missingGoogleAppShowsWarningRowThatOpensTheGuide() {
        var shown = 0
        val state = SpeechSetupState(permissionGranted = true, availability = SpeechAvailability.AVAILABLE,
            speechServices = GoogleSpeechServices.Status.MISSING)
        compose.setContent { FunputUiTheme(false) {
            SpeechSetupScreen(state, {}, {}, {}, {}, {}, guide = SpeechGuideActions(show = { shown++ }))
        } }
        compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(hasTestTag("speech-services-missing"))
        compose.onNodeWithTag("speech-services-missing").performClick()
        assertEquals(1, shown)
    }
    @Test fun guideSheetListsThePopupStepOnlyForBlockingRoms() {
        val state = SpeechSetupState(availability = SpeechAvailability.AVAILABLE,
            speechServices = GoogleSpeechServices.Status.INSTALLED, guideVisible = true)
        var guide by mutableStateOf(SpeechGuideActions(SpeechGuideContext(
            GoogleSpeechServices.Status.INSTALLED, hasPlayStore = true, popupPermissionRom = true)))
        compose.setContent { FunputUiTheme(false) { SpeechSetupScreen(state, {}, {}, {}, {}, {}, guide = guide) } }
        compose.onNodeWithTag("speech-guide-popup").assertExists()
        compose.onNodeWithTag("speech-guide-play").assertExists()
        compose.onNodeWithTag("speech-guide-settings").assertExists()
        guide = SpeechGuideActions(SpeechGuideContext(
            GoogleSpeechServices.Status.MISSING, hasPlayStore = true, popupPermissionRom = false))
        compose.onNodeWithTag("speech-guide-popup").assertDoesNotExist()
        compose.onNodeWithTag("speech-guide-settings").assertDoesNotExist()
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
