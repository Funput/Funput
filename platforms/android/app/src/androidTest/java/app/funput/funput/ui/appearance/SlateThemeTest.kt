package app.funput.funput.ui.appearance

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SlateThemeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun slateThemeIsDisplayedAndSelectable() {
        var selectedThemeId = KeyboardThemeId.Dark

        compose.setContent {
            FunputTheme {
                AppearanceScreen(
                    testAppearanceState(onThemeSelected = { selectedThemeId = it }),
                )
            }
        }

        compose.onNodeWithTag(AppearanceListTag)
            .performScrollToNode(hasTestTag(KeyboardThemeId.Slate.value))
        compose.onNodeWithTag(KeyboardThemeId.Slate.value).performClick()

        assertEquals(KeyboardThemeId.Slate, selectedThemeId)
    }
}
