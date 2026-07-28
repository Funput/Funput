package app.funput.funput.ui.theme.gallery

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.ime.settings.KeyboardThemeSlot
import app.funput.funput.theme.InstalledThemeRepository
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SlateThemeGalleryTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun slateThemeIsDisplayedAndSelectable() {
        var selectedThemeId = KeyboardThemeId.Dark

        compose.setContent {
            FunputTheme {
                ThemeGalleryScreen(
                    themes = InstalledThemeRepository.builtIn().themes,
                    selectedThemeId = selectedThemeId,
                    followsAppearance = false,
                    activeSlot = KeyboardThemeSlot.SINGLE,
                    onThemeSelected = { selectedThemeId = it },
                    onFollowsAppearanceChange = {},
                    onSlotSelected = {},
                    onCreateTheme = {},
                    onEditTheme = {},
                    onDeleteTheme = {},
                    onBack = {},
                )
            }
        }

        compose.onNodeWithTag(SystemThemesTag)
            .performScrollToNode(hasTestTag(KeyboardThemeId.Slate.value))
        compose.onNodeWithTag(KeyboardThemeId.Slate.value).performClick()

        compose.runOnIdle {
            assertEquals(KeyboardThemeId.Slate, selectedThemeId)
        }
    }
}
