package app.funput.funput.keyboard.ui.speech

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.theme.LocalKeyboardThemeCatalog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SpeechPanelDesignInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun listeningControlsHaveSeparateRoundedActionAreasWithMinimumTargets() {
        SpeechPanelFixture().use { fixture ->
            fixture.update {
                it.speechPanelState = SpeechPanelState(SpeechPanelStage.LISTENING)
                it.showSpeechPanel()
            }
            val cancel = compose.onNodeWithText("Huỷ").getUnclippedBoundsInRoot()
            val stop = compose.onNodeWithText("Dừng").getUnclippedBoundsInRoot()
            assertTrue(stop.left > cancel.right)
            assertTrue(stop.bottom - stop.top >= 48.dp)
            assertTrue(cancel.bottom - cancel.top >= 48.dp)
            compose.onNodeWithText("Hãy nói, Funput đang lắng nghe.").assertIsDisplayed()
            compose.onNodeWithText("Dừng").assertIsDisplayed()
        }
    }

    @Test fun largeEnglishErrorKeepsActionsVisibleAndMessageReachableByScrolling() {
        SpeechPanelFixture(fontScale = 2f, widthDp = 240, locale = java.util.Locale.ENGLISH).use { fixture ->
            val message = "Please check the microphone permission and return to the keyboard."
            fixture.update {
                it.speechPanelState = SpeechPanelState(SpeechPanelStage.ERROR,
                    language = KeyboardLanguage.ENGLISH, message = message, canOpenSetup = true)
                it.showSpeechPanel()
            }
            compose.onNodeWithText("Set up voice input").assertIsDisplayed()
            compose.onNodeWithText("Back").assertIsDisplayed()
            compose.onNodeWithText(message).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Set up voice input").assertIsDisplayed()
            compose.onNodeWithText("Back").assertIsDisplayed()
        }
    }

    @Test fun switchingEveryPresetKeepsTheSameTranscriptAndActions() {
        SpeechPanelFixture().use { fixture ->
            fixture.update {
                it.speechPanelState = SpeechPanelState(SpeechPanelStage.LISTENING, preview = "Lời nói của bạn")
                it.showSpeechPanel()
            }
            LocalKeyboardThemeCatalog.themes.forEach { theme ->
                fixture.update { it.keyboardTheme = theme.theme }
                compose.onNodeWithText("Lời nói của bạn").assertIsDisplayed()
                compose.onNodeWithText("Dừng").assertIsDisplayed()
                compose.onNodeWithText("Huỷ").assertIsDisplayed()
            }
        }
    }
}
