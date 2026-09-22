package app.funput.funput.ui.theme.custom

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import app.funput.funput.ui.theme.custom.color.ColorPickerHexTag
import app.funput.funput.ui.theme.custom.color.ColorPickerState
import app.funput.funput.theme.BuiltInKeyboardThemeSource
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
import app.funput.funput.theme.store.custom.CustomThemeDraft
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

        // A new theme arrives named, so saving is never blocked on the least interesting decision.
        compose.onNodeWithText("Lưu chủ đề").assertIsEnabled()
        compose.onNodeWithTag("custom-theme-name").performTextClearance()
        compose.onNodeWithTag("custom-theme-name").performTextInput("Ocean")
        // Base and accent are the first two things on the page now, not a dropdown behind restore.
        compose.onNodeWithText(lightTheme.name).performClick()
        compose.onNodeWithTag(AccentColorSelectorTag).performClick()
        compose.onNodeWithTag(ColorPickerHexTag).performTextReplacement("2F9BFF")
        compose.onNodeWithText("Chọn").performClick()
        compose.onNodeWithText("Lưu chủ đề").performClick()

        compose.runOnIdle {
            assertEquals("Ocean", savedDraft?.name)
            assertEquals(KeyboardThemeId.Light, savedDraft?.baseThemeId)
            // The re-dye is proved against every base and hue in ThemeRecolorTest; what this test
            // is for is that the color typed into the picker is the accent that got saved.
            val ocean = ColorPickerState.fromHexOrNull("2F9BFF", alpha = 1f)!!.argb
            assertEquals(ocean, savedDraft?.theme?.accentColor)
            assertNotEquals(lightTheme.theme, savedDraft?.theme)
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
            theme = lightTheme.theme.withAccent(0xFF9F5CFF.toInt()),
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

        compose.onNodeWithText("Lưu chủ đề").performClick()

        compose.runOnIdle {
            assertEquals("Ocean", savedDraft?.name)
            assertEquals(KeyboardThemeId.Light, savedDraft?.baseThemeId)
            // Opening and saving without touching a control must not drift any token.
            assertEquals(editingTheme.theme, savedDraft?.theme)
        }
    }
}
