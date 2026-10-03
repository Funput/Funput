package app.funput.funput.keyboard.ui.speech

import android.view.View
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.ui.KeyboardPanel
import app.funput.funput.keyboard.ui.support.childOfType
import app.funput.funput.keyboard.ui.support.descendants
import app.funput.funput.keyboard.utility.KeyboardMicrophoneState
import app.funput.funput.theme.KeyboardThemes
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SpeechPanelInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun panelIsLazyInertAndStopWaitsForFinalWhileCancelClearsPreview() {
        SpeechPanelFixture().use { fixture ->
            val actions = mutableListOf<SpeechPanelAction>()
            fixture.update { keyboard ->
                keyboard.callbacks.onSpeechAction = actions::add
                assertTrue(keyboard.descendants().filterIsInstance<SpeechPanelView>().none())
                keyboard.speechPanelState = SpeechPanelState(SpeechPanelStage.LISTENING, preview = "xin chào")
                keyboard.showSpeechPanel()
                keyboard.showSpeechPanel()
            }
            compose.onNodeWithText("Đang nghe…").assert(SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            compose.onNodeWithText("xin chào").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
            assertTrue(actions.isEmpty())
            compose.onNodeWithText("Dừng").performClick()
            assertEquals(listOf(SpeechPanelAction.STOP), actions)
            fixture.update { keyboard ->
                assertEquals(KeyboardPanel.SPEECH, keyboard.activePanel)
                keyboard.speechPanelState = keyboard.speechPanelState.copy(stage = SpeechPanelStage.FINALIZING)
            }
            compose.onNodeWithText("Đang xử lý…").assertIsDisplayed()
            compose.onNodeWithText("Dừng").assertDoesNotExist()
            compose.onNodeWithText("Huỷ").performClick()
            fixture.update { keyboard ->
                assertEquals(KeyboardPanel.LETTERS, keyboard.activePanel)
                assertEquals("", keyboard.speechPanelState.preview)
                assertEquals(View.GONE, keyboard.childOfType<SpeechPanelView>().visibility)
            }
            assertEquals(listOf(SpeechPanelAction.STOP, SpeechPanelAction.CANCEL), actions)
        }
    }

    @Test fun errorActionsAreExplicitAndOpeningOrRecomposingDoesNotStartRecording() {
        SpeechPanelFixture().use { fixture ->
            val actions = mutableListOf<SpeechPanelAction>()
            fixture.update { keyboard ->
                keyboard.callbacks.onSpeechAction = actions::add
                keyboard.speechPanelState = SpeechPanelState(SpeechPanelStage.ERROR,
                    message = "Thiếu model", canOpenSetup = true, canRetry = true)
                keyboard.showSpeechPanel()
            }
            compose.onNodeWithText("Thiết lập giọng nói").performClick()
            compose.onNodeWithText("Thử lại").assertDoesNotExist()
            fixture.update { keyboard ->
                keyboard.speechPanelState = keyboard.speechPanelState.copy(canOpenSetup = false)
                keyboard.keyboardTheme = KeyboardThemes.GlassLight
            }
            compose.onNodeWithText("Thử lại").performClick()
            assertEquals(listOf(SpeechPanelAction.OPEN_SETUP, SpeechPanelAction.RETRY), actions)
            compose.onNodeWithText("Quay lại").performClick()
            assertEquals(SpeechPanelAction.CANCEL, actions.last())
        }
    }

    @Test fun backAndPanelNavigationCancelOnceAndReuseTheLazyView() {
        SpeechPanelFixture().use { fixture ->
            val actions = mutableListOf<SpeechPanelAction>()
            fixture.update { keyboard ->
                keyboard.callbacks.onSpeechAction = actions::add
                keyboard.showSpeechPanel()
                val first = keyboard.childOfType<SpeechPanelView>()
                assertTrue(keyboard.cancelSpeechPanel())
                assertFalse(keyboard.cancelSpeechPanel())
                keyboard.showSpeechPanel()
                assertSame(first, keyboard.childOfType<SpeechPanelView>())
                keyboard.showSymbolsPanel()
                keyboard.showLettersPanel()
                assertEquals(listOf(SpeechPanelAction.CANCEL, SpeechPanelAction.CANCEL), actions)
                assertEquals(View.VISIBLE, keyboard.childOfType<KeyboardSurfaceView>().visibility)
            }
        }
    }

    @Test fun largeTextAndNarrowPanelKeepControlsReadableAndPreviewScrollable() {
        SpeechPanelFixture(fontScale = 2f, widthDp = 240).use { fixture ->
            fixture.update { keyboard ->
                keyboard.keyboardTheme = KeyboardThemes.GlassDark
                keyboard.speechPanelState = SpeechPanelState(SpeechPanelStage.LISTENING,
                    preview = "Một đoạn nhận dạng tiếng Việt dài. ".repeat(80))
                keyboard.showSpeechPanel()
            }
            compose.onNodeWithText("Dừng").assertIsDisplayed()
            compose.onNodeWithText("Huỷ").assertIsDisplayed().performClick()
        }
    }

    @Test fun micRequestsAreHostActionsAndSecureEditorsRemainBlocked() {
        SpeechPanelFixture().use { fixture ->
            fixture.update { keyboard ->
                var requests = 0
                keyboard.callbacks.onSpeechRequested = { requests++ }
                val surface = keyboard.childOfType<KeyboardSurfaceView>()
                surface.callbacks.onMicrophoneRequested?.invoke()
                assertEquals(0, requests)
                keyboard.microphone = KeyboardMicrophoneState(visible = true)
                keyboard.suggestionBarEnabled = false
                keyboard.suggestions = listOf("xin", "chào")
                surface.callbacks.onMicrophoneRequested?.invoke()
                assertEquals(1, requests)
                assertEquals(KeyboardPanel.LETTERS, keyboard.activePanel)
                listOf(KeyboardEditorMode.PASSWORD, KeyboardEditorMode.PIN, KeyboardEditorMode.EMAIL,
                    KeyboardEditorMode.NUMBER, KeyboardEditorMode.PHONE).forEach { mode ->
                    keyboard.editorMode = mode
                    surface.callbacks.onMicrophoneRequested?.invoke()
                }
                assertEquals(1, requests)
            }
        }
    }
}
