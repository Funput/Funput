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
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
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
        val baseThemes = BuiltInKeyboardThemeSource.loadThemes()
        val lightTheme = baseThemes.single { theme -> theme.id == KeyboardThemeId.Light }

        compose.setContent {
            FunputTheme {
                CreateCustomThemeScreen(
                    baseThemes = baseThemes,
                    onSave = { draft -> savedDraft = draft },
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("Lưu theme").assertIsNotEnabled()
        compose.onNodeWithText("Thông tin").performClick()
        compose.onNodeWithTag("custom-theme-name").performTextInput("Ocean")
        compose.onNodeWithText("Phong cách").performClick()
        compose.onNodeWithText("Sáng").performClick().assertIsSelected()
        compose.onNodeWithContentDescription("Xanh biển").performClick().assertIsSelected()
        compose.onNodeWithText("Lưu theme").performClick()

        compose.runOnIdle {
            assertEquals("Ocean", savedDraft?.name)
            assertEquals(KeyboardThemeId.Light, savedDraft?.baseThemeId)
            // The draft carries resolved tokens, so assert the theme the editor produced.
            assertEquals(lightTheme.theme.withAccent(AccentPresets[3].argb), savedDraft?.theme)
        }
    }

    @Test
    fun editsDraftFromExistingCustomTheme() {
        var savedDraft: CustomThemeDraft? = null
        val baseThemes = BuiltInKeyboardThemeSource.loadThemes()
        val lightTheme = baseThemes.single { theme -> theme.id == KeyboardThemeId.Light }
        val editingTheme = KeyboardThemeDescriptor(
            id = KeyboardThemeId.of("custom.ocean"),
            version = 1,
            name = "Ocean",
            author = "Me",
            origin = KeyboardThemeOrigin.CUSTOM,
            baseThemeId = KeyboardThemeId.Light,
            theme = lightTheme.theme.withAccent(AccentPresets[2].argb),
        )

        compose.setContent {
            FunputTheme {
                CreateCustomThemeScreen(
                    baseThemes = baseThemes,
                    editingTheme = editingTheme,
                    onSave = { draft -> savedDraft = draft },
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("Lưu theme").performClick()

        compose.runOnIdle {
            assertEquals("Ocean", savedDraft?.name)
            assertEquals(KeyboardThemeId.Light, savedDraft?.baseThemeId)
            assertEquals(AccentPresets[2].argb, savedDraft?.theme?.accentColor)
            // Opening and saving without touching a control must not drift the key opacity.
            assertEquals(editingTheme.theme.keyColor, savedDraft?.theme?.keyColor)
        }
    }
}
