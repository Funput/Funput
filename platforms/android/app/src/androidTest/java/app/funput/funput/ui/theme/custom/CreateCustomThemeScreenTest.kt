package app.funput.funput.ui.theme.custom

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.funput.funput.theme.BuiltInKeyboardThemeSource
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.store.custom.CustomThemeDraft
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CreateCustomThemeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun createsDraftFromFormState() {
        var savedDraft: CustomThemeDraft? = null

        compose.setContent {
            FunputTheme {
                CreateCustomThemeScreen(
                    baseThemes = BuiltInKeyboardThemeSource.loadThemes(),
                    onSave = { draft -> savedDraft = draft },
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("Lưu theme").assertIsNotEnabled()
        compose.onNodeWithTag("custom-theme-name").performTextInput("Ocean")
        compose.onNodeWithText("Sáng").performClick().assertIsSelected()
        compose.onNodeWithContentDescription("Xanh biển").performClick().assertIsSelected()
        compose.onNodeWithText("Lưu theme").performClick()

        compose.runOnIdle {
            assertEquals("Ocean", savedDraft?.name)
            assertEquals(KeyboardThemeId.Light, savedDraft?.baseThemeId)
            assertEquals(AccentPresets[3].argb, savedDraft?.overrides?.accentColor)
        }
    }
}
