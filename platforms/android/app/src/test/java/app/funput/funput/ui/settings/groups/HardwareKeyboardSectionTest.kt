package app.funput.funput.ui.settings.groups

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ime.settings.hardware.HardwareKeyboardPreferences
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.hardware.HardwareKeyboardSection
import app.funput.funput.ui.settings.hardware.HardwareKeyboardSectionState
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The physical keyboard group: both switches and the link to Android's own settings. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class HardwareKeyboardSectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the section dispatches both switches and the system settings link`() {
        val showChanges = mutableListOf<Boolean>()
        val hotkeyChanges = mutableListOf<Boolean>()
        var openedSystemSettings = false
        compose.setContent {
            FunputUiTheme(isDark = false) {
                HardwareKeyboardSection(
                    HardwareKeyboardSectionState(
                        preferences = HardwareKeyboardPreferences.Default,
                        onShowsSoftKeyboardChanged = showChanges::add,
                        onToggleHotkeyChanged = hotkeyChanges::add,
                        onOpenSystemSettings = { openedSystemSettings = true },
                    ),
                )
            }
        }

        compose.onNodeWithText(ShowTitle).performClick()
        compose.onNodeWithText(HotkeyTitle).performClick()
        compose.onNodeWithText("Cài đặt bàn phím vật lý").performClick()

        assertEquals(listOf(true), showChanges)
        assertEquals(listOf(false), hotkeyChanges)
        assertTrue(openedSystemSettings)
    }

    @Test
    fun `the switches reflect the defaults`() {
        compose.setContent {
            FunputUiTheme(isDark = false) { HardwareKeyboardSection(HardwareKeyboardSectionState.Inert) }
        }

        compose.onNodeWithText(ShowTitle).assertIsOff()
        compose.onNodeWithText(HotkeyTitle).assertIsOn()
    }

    private companion object {
        const val ShowTitle = "Hiện Funput khi có bàn phím rời"
        const val HotkeyTitle = "Alt + Space để hiện hoặc ẩn"
    }
}
