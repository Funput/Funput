package app.funput.funput.ui.theme.gallery

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.theme.InstalledThemeRepository
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
import app.funput.funput.theme.KeyboardThemes
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ThemeGalleryScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun displaysSelectionAndDispatchesThemeAndBackActions() {
        var selectedThemeId = KeyboardThemeId.Dark
        var backRequested = false
        var createRequested = false
        var editedThemeId: KeyboardThemeId? = null
        var deletedThemeId: KeyboardThemeId? = null
        val repository = InstalledThemeRepository.builtIn()
        val customTheme = KeyboardThemeDescriptor(
            id = KeyboardThemeId.of("custom.ocean"),
            version = 1,
            name = "Ocean",
            author = "Me",
            origin = KeyboardThemeOrigin.CUSTOM,
            baseThemeId = KeyboardThemeId.Dark,
            theme = KeyboardThemes.Ink,
        )

        compose.setContent {
            FunputTheme {
                ThemeGalleryScreen(
                    themes = repository.themes + customTheme,
                    selectedThemeId = selectedThemeId,
                    onThemeSelected = { selectedThemeId = it },
                    onCreateTheme = { createRequested = true },
                    onEditTheme = { themeId -> editedThemeId = themeId },
                    onDeleteTheme = { themeId -> deletedThemeId = themeId },
                    onBack = { backRequested = true },
                )
            }
        }

        compose.onNodeWithContentDescription("Tạo theme riêng").performClick()
        compose.onNodeWithContentDescription("Sửa theme Ocean").performClick()
        compose.onNodeWithContentDescription("Xóa theme Ocean").performClick()
        compose.onNodeWithText("Xóa theme").performClick()
        compose.onNodeWithTag("dark").assertIsSelected()
        compose.onNodeWithTag("light").assertIsNotSelected().performClick()
        compose.onNodeWithContentDescription("Quay lại").performClick()

        compose.runOnIdle {
            assertEquals(KeyboardThemeId.Light, selectedThemeId)
            assertEquals(customTheme.id, editedThemeId)
            assertEquals(customTheme.id, deletedThemeId)
            assertTrue(createRequested)
            assertTrue(backRequested)
        }
    }
}
