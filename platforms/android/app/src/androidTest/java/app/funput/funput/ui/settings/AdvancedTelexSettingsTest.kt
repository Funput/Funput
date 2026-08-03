package app.funput.funput.ui.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdvancedTelexSettingsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun inputMethodPickerShowsIosParityContentAndSelectsAdvancedTelex() {
        var selected = KeyboardInputMethod.TELEX
        compose.setContent {
            FunputTheme {
                SettingsPickerSheet(
                    picker = SettingsPicker.INPUT_METHOD,
                    inputMethod = selected,
                    toneStyle = ToneStyle.TRADITIONAL,
                    keySizeProfile = KeyboardSizingProfile.Default,
                    appearanceMode = AppearanceMode.SYSTEM,
                    onInputMethodSelected = { selected = it },
                    onToneStyleSelected = {},
                    onKeySizeSelected = {},
                    onAppearanceSelected = {},
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("Telex").assertExists()
        compose.onNodeWithText("Dùng tổ hợp chữ để nhập dấu.").assertExists()
        compose.onNodeWithText("Telex nâng cao").assertExists()
        compose.onNodeWithText("Full Telex — [→ư, ]→ơ, w đầu từ→ư.").assertExists()
        compose.onNodeWithText("VNI").assertExists()
        compose.onNodeWithText("Dùng các phím số để nhập dấu.").assertExists()
        compose.onNodeWithText("Telex nâng cao").performClick()
        compose.runOnIdle { assertEquals(KeyboardInputMethod.TELEX_ADVANCED, selected) }
    }
}
