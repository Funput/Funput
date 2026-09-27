package app.funput.funput.ui.settings

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Picker sheets list every option with its hint, mark the current one, apply a choice and close. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class SettingsPickerSheetTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the input method sheet explains each method and selects advanced telex`() {
        var selected: KeyboardInputMethod? = null
        var dismissed = false
        compose.setContent {
            FunputUiTheme(isDark = false) {
                SettingsPickerSheet(
                    picker = SettingsPicker.INPUT_METHOD,
                    state = testSettingsState(onInputMethodSelected = { selected = it }),
                    onDismiss = { dismissed = true },
                )
            }
        }

        compose.onNodeWithText("Telex").assertIsSelected()
        compose.onNodeWithText("Dùng tổ hợp chữ để nhập dấu.").assertExists()
        compose.onNodeWithText("Gõ ] thành ư, [ thành ơ; w đầu từ thành ư.").assertExists()
        compose.onNodeWithText("Dùng các phím số để nhập dấu.").assertExists()
        compose.onNodeWithText("Telex nâng cao").performClick()

        assertEquals(KeyboardInputMethod.TELEX_ADVANCED, selected)
        assertTrue(dismissed)
    }

    @Test
    fun `the clipboard sheet keeps the iOS order and selects a week`() {
        var selected: ClipboardExpiry? = null
        compose.setContent {
            FunputUiTheme(isDark = false) {
                SettingsPickerSheet(
                    picker = SettingsPicker.CLIPBOARD_EXPIRY,
                    state = testSettingsState(
                        clipboardExpiry = ClipboardExpiry.HOUR,
                        onClipboardExpirySelected = { selected = it },
                    ),
                    onDismiss = {},
                )
            }
        }

        listOf("1 giờ", "1 ngày", "1 tuần").forEach { compose.onNodeWithText(it).assertExists() }
        val weekHint = "Giữ lâu nhất. Cân nhắc nếu bạn hay sao chép thông tin nhạy cảm."
        compose.onNodeWithText(weekHint).assertExists()
        compose.onNodeWithText("1 tuần").performClick()

        assertEquals(ClipboardExpiry.WEEK, selected)
    }
}
