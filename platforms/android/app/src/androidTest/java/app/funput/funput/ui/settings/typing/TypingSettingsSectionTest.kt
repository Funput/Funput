package app.funput.funput.ui.settings.typing

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.settings.SettingsPicker
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TypingSettingsSectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sectionDispatchesInputMethodAndToneStyleActions() {
        var picker: SettingsPicker? = null
        var toneStyle: ToneStyle? = null
        compose.setContent {
            FunputTheme {
                TypingSettingsSection(
                    inputMethod = KeyboardInputMethod.TELEX,
                    toneStyle = ToneStyle.TRADITIONAL,
                    onOpenPicker = { picker = it },
                    onToneStyleSelected = { toneStyle = it },
                )
            }
        }

        compose.onNodeWithText("Kiểu gõ").performClick()
        compose.onNodeWithText("Hiện đại").performClick()

        compose.runOnIdle {
            assertEquals(SettingsPicker.INPUT_METHOD, picker)
            assertEquals(ToneStyle.MODERN, toneStyle)
        }
    }
}
