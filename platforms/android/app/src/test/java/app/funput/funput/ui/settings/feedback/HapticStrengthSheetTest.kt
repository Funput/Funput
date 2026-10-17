package app.funput.funput.ui.settings.feedback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.funput.funput.keyboard.KeyboardHapticStrength
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The strength sheet marks the current strength, and stays open while the user tries each one. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class HapticStrengthSheetTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `choosing strengths in turn keeps the sheet open, and off is a choice too`() {
        val chosen = mutableListOf<KeyboardHapticStrength?>()
        var dismissed = 0
        compose.setContent {
            var selected by remember { mutableStateOf<KeyboardHapticStrength?>(KeyboardHapticStrength.MEDIUM) }
            FunputUiTheme(isDark = false) {
                HapticStrengthSheet(
                    selected = selected,
                    onSelect = { chosen += it; selected = it },
                    onDismiss = { dismissed += 1 },
                )
            }
        }

        compose.onNodeWithTag(hapticStrengthTag(HapticStrengthOption.MEDIUM)).assertIsSelected()
        compose.onNodeWithTag(hapticStrengthTag(HapticStrengthOption.STRONG)).performClick()
        compose.onNodeWithTag(hapticStrengthTag(HapticStrengthOption.LIGHT)).performClick()
        compose.onNodeWithTag(hapticStrengthTag(HapticStrengthOption.OFF)).performClick()

        compose.onNodeWithTag(hapticStrengthTag(HapticStrengthOption.OFF)).assertIsSelected()
        assertEquals(listOf(KeyboardHapticStrength.STRONG, KeyboardHapticStrength.LIGHT, null), chosen)
        assertEquals(0, dismissed)
    }

    @Test
    fun `every option maps back to itself`() {
        HapticStrengthOption.entries.forEach { option ->
            assertEquals(option, HapticStrengthOption.of(option.strength))
        }
    }
}
