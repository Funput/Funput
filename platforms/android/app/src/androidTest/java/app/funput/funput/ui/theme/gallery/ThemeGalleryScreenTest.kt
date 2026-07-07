package app.funput.funput.ui.theme.gallery

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.funput.funput.theme.InstalledThemeRepository
import app.funput.funput.theme.KeyboardThemeId
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
        val repository = InstalledThemeRepository.builtIn()

        compose.setContent {
            FunputTheme {
                ThemeGalleryScreen(
                    themes = repository.themes,
                    selectedThemeId = selectedThemeId,
                    onThemeSelected = { selectedThemeId = it },
                    onCreateTheme = { createRequested = true },
                    onBack = { backRequested = true },
                )
            }
        }

        compose.onNodeWithContentDescription("Tạo theme riêng").performClick()
        compose.onNodeWithTag("dark").assertIsSelected()
        compose.onNodeWithTag("light").assertIsNotSelected().performClick()
        compose.onNodeWithContentDescription("Quay lại").performClick()

        compose.runOnIdle {
            assertEquals(KeyboardThemeId.Light, selectedThemeId)
            assertTrue(createRequested)
            assertTrue(backRequested)
        }
    }
}
