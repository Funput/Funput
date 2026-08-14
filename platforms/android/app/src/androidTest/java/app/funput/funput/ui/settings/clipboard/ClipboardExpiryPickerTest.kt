package app.funput.funput.ui.settings.clipboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.settings.SettingsPicker
import app.funput.funput.ui.settings.SettingsPickerSheet
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ClipboardExpiryPickerTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun pickerMatchesIosOrderAndSelectsExpiry() {
        var selected = ClipboardExpiry.HOUR
        compose.setContent {
            FunputTheme {
                SettingsPickerSheet(
                    picker = SettingsPicker.CLIPBOARD_EXPIRY,
                    inputMethod = KeyboardInputMethod.TELEX,
                    toneStyle = ToneStyle.TRADITIONAL,
                    clipboardExpiry = selected,
                    onInputMethodSelected = {},
                    onToneStyleSelected = {},
                    onClipboardExpirySelected = { selected = it },
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("1 giờ").assertIsDisplayed()
        compose.onNodeWithText("1 ngày").assertIsDisplayed()
        compose.onNodeWithText("1 tuần").assertIsDisplayed()
        compose.onNodeWithText("Giữ lâu nhất. Cân nhắc nếu bạn hay sao chép thông tin nhạy cảm.")
            .assertIsDisplayed()
        compose.onNodeWithText("1 tuần").performClick()
        compose.runOnIdle { assertEquals(ClipboardExpiry.WEEK, selected) }
    }
}
