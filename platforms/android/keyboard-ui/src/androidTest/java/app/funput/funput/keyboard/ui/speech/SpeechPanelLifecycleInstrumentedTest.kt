package app.funput.funput.keyboard.ui.speech

import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.placement.KeyboardPlacementMode
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import app.funput.funput.keyboard.ui.KeyboardPanel
import app.funput.funput.keyboard.ui.support.childOfType
import app.funput.funput.theme.KeyboardThemes
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SpeechPanelLifecycleInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun englishResourcesAndLanguageLabelRenderIndependentlyOfTheDeviceLocale() {
        SpeechPanelFixture(locale = java.util.Locale.ENGLISH).use { fixture ->
            fixture.update { keyboard ->
                keyboard.speechPanelState = SpeechPanelState(SpeechPanelStage.LISTENING, KeyboardLanguage.ENGLISH)
                keyboard.showSpeechPanel()
            }
            compose.onNodeWithText("Listening…").assertIsDisplayed()
            compose.onNodeWithText("English").assertIsDisplayed()
            compose.onNodeWithText("Stop").assertIsDisplayed()
            compose.onNodeWithText("Cancel").assertIsDisplayed()
        }
    }

    @Test fun cancellationCallbackCanReturnToLettersWithoutResurrectingAnotherPanel() {
        SpeechPanelFixture().use { fixture ->
            fixture.update { keyboard ->
                val changes = mutableListOf<KeyboardPanel>()
                var cancellations = 0
                keyboard.callbacks.onPanelChanged = changes::add
                keyboard.callbacks.onSpeechAction = {
                    if (it == SpeechPanelAction.CANCEL) {
                        cancellations++
                        keyboard.showLettersPanel()
                    }
                }
                keyboard.showSpeechPanel()
                keyboard.showEmojiPanel()
                assertEquals(1, cancellations)
                assertEquals(KeyboardPanel.LETTERS, keyboard.activePanel)
                assertEquals(listOf(KeyboardPanel.SPEECH, KeyboardPanel.LETTERS), changes)
                assertEquals(View.VISIBLE, keyboard.childOfType<KeyboardSurfaceView>().visibility)
                assertEquals(View.GONE, keyboard.childOfType<SpeechPanelView>().visibility)
            }
        }
    }

    @Test fun reusedPanelKeepsThemeFeedbackAndPlacementWithoutGrowingTheHost() {
        SpeechPanelFixture().use { fixture ->
            fixture.update { keyboard ->
                keyboard.keyboardTheme = KeyboardThemes.GlassDark
                keyboard.hapticsEnabled = false
                keyboard.soundsEnabled = false
                keyboard.placementPreferences = KeyboardPlacementPreferences.Default.copy(
                    activeMode = KeyboardPlacementMode.ONE_HANDED, oneHandedWidthFraction = 0.68f)
                keyboard.speechPanelState = SpeechPanelState(SpeechPanelStage.LISTENING, preview = "mẫu")
                keyboard.showSpeechPanel()
                val panel = keyboard.childOfType<SpeechPanelView>()
                assertFalse(panel.isHapticFeedbackEnabled)
                assertFalse(panel.isSoundEffectsEnabled)
                assertArrayEquals(intArrayOf(KeyboardThemes.GlassDark.backgroundStartColor,
                    KeyboardThemes.GlassDark.backgroundEndColor), (panel.background as GradientDrawable).colors)
                keyboard.showLettersPanel()
                keyboard.keyboardTheme = KeyboardThemes.GlassLight
                keyboard.hapticsEnabled = true
                keyboard.soundsEnabled = true
                val height = keyboard.measuredHeight
                keyboard.showSpeechPanel()
                assertSame(panel, keyboard.childOfType<SpeechPanelView>())
                assertEquals(height, keyboard.measuredHeight)
                assertEquals(KeyboardPlacementMode.ONE_HANDED, keyboard.placementPreferences.activeMode)
                assertEquals("", panel.state.preview)
                assertTrue(panel.isHapticFeedbackEnabled)
                assertTrue(panel.isSoundEffectsEnabled)
                assertArrayEquals(intArrayOf(KeyboardThemes.GlassLight.backgroundStartColor,
                    KeyboardThemes.GlassLight.backgroundEndColor), (panel.background as GradientDrawable).colors)
            }
        }
    }
}
