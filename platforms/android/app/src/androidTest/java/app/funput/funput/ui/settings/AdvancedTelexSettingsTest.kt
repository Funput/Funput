package app.funput.funput.ui.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.settings.ToneStyle
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
                    clipboardExpiry = ClipboardExpiry.HOUR,
                    onInputMethodSelected = { selected = it },
                    onToneStyleSelected = {},
                    onClipboardExpirySelected = {},
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("Telex").assertExists()
        compose.onNodeWithText("Dùng tổ hợp chữ để nhập dấu.").assertExists()
        compose.onNodeWithText("Telex nâng cao").assertExists()
        compose.onNodeWithText("Gõ [ thành ư, ] thành ơ; w đầu từ thành ư.").assertExists()
        compose.onNodeWithText("VNI").assertExists()
        compose.onNodeWithText("Dùng các phím số để nhập dấu.").assertExists()
        compose.onNodeWithText("Telex nâng cao").performClick()
        compose.runOnIdle { assertEquals(KeyboardInputMethod.TELEX_ADVANCED, selected) }
    }
}
