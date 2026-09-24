package app.funput.funput.ui.settings.hardware

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ime.settings.hardware.HardwareKeyboardPreferences
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HardwareKeyboardSettingsSectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sectionDispatchesBothSwitchesAndTheSystemSettingsLink() {
        val showChanges = mutableListOf<Boolean>()
        val hotkeyChanges = mutableListOf<Boolean>()
        var openedSystemSettings = false
        compose.setContent {
            FunputTheme {
                HardwareKeyboardSettingsSection(
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

        compose.runOnIdle {
            assertEquals(listOf(true), showChanges)
            assertEquals(listOf(false), hotkeyChanges)
            assertTrue(openedSystemSettings)
        }
    }

    @Test
    fun switchesReflectTheDefaults() {
        compose.setContent {
            FunputTheme { HardwareKeyboardSettingsSection(HardwareKeyboardSectionState.Inert) }
        }

        compose.onNode(isToggleable() and hasText(ShowTitle)).assertIsOff()
        compose.onNode(isToggleable() and hasText(HotkeyTitle)).assertIsOn()
    }

    private companion object {
        const val ShowTitle = "Hiện Funput khi có bàn phím rời"
        const val HotkeyTitle = "Alt + Space để hiện hoặc ẩn"
    }
}
