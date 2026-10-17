package app.funput.funput.ui.settings.hub

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.ui.kit.layout.FunputScreenListTag
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.navigation.AppDestination
import app.funput.funput.ui.navigation.AppNavDisplay
import app.funput.funput.ui.navigation.AppNavigator
import app.funput.funput.ui.navigation.TopLevelDestination
import app.funput.funput.ui.settings.pages.SettingsPage
import app.funput.funput.ui.settings.pages.SettingsPageScreen
import app.funput.funput.ui.settings.testSettingsState
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Every hub row opens its page inside the keyboard tab, and back from the page lands on the hub. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class SettingsNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `each hub page opens and back returns to the hub`() {
        val navigator = AppNavigator()
        val state = testSettingsState()
        compose.setContent {
            FunputUiTheme(isDark = false) {
                AppNavDisplay(navigator) { destination ->
                    val page = SettingsPage.of(destination)
                    if (page == null) {
                        KeyboardHubScreen(state, onOpenPage = { opened ->
                            navigator.navigate(opened.destinationIn(TopLevelDestination.KEYBOARD))
                        })
                    } else {
                        SettingsPageScreen(page, state, onBack = { navigator.navigateBack() })
                    }
                }
            }
        }

        SettingsPage.hubRows(speechAvailable = false).forEach { page ->
            val row = settingsPageRowTag(page)
            compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(hasTestTag(row))
            compose.onNodeWithTag(row).performClick()
            compose.waitForIdle()
            assertEquals(page.destinations.first(), navigator.currentDestination)

            compose.onNodeWithContentDescription("Quay lại").performClick()
            compose.waitForIdle()
            assertEquals(AppDestination.KEYBOARD, navigator.currentDestination)
            compose.onNodeWithTag(SettingsAllTag).assertExists()
        }
    }
}
