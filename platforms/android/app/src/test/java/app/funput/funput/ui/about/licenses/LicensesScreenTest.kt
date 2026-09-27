package app.funput.funput.ui.about.licenses

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.ui.about.AboutRoute
import app.funput.funput.ui.kit.glass.GlassTier
import app.funput.funput.ui.kit.glass.LocalGlassTier
import app.funput.funput.ui.kit.layout.FunputScreenListTag
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.navigation.AppDestination
import app.funput.funput.ui.navigation.rememberAppNavigator
import app.funput.funput.uitesting.SCREENSHOT_SDK
import app.funput.funput.uitesting.ScreenshotDevices
import app.funput.funput.uitesting.waitForText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** About → licences and back, with every shipped notice readable offline. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class LicensesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun scrollTo(text: String) =
        compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(hasText(text))

    @Test
    fun `about opens every notice, and back returns to about`() {
        compose.setContent {
            val navigator = rememberAppNavigator()
            FunputUiTheme(isDark = false) {
                CompositionLocalProvider(LocalGlassTier provides GlassTier.SOLID) {
                    if (navigator.currentDestination == AppDestination.THIRD_PARTY_LICENSES) {
                        LicensesRoute { navigator.navigateBack() }
                    } else {
                        AboutRoute { navigator.navigate(AppDestination.THIRD_PARTY_LICENSES) }
                    }
                }
            }
        }
        scrollTo("Giấy phép bên thứ ba")
        compose.onNodeWithText("Giấy phép bên thứ ba").performClick()

        compose.waitForText("SCOWL", substring = true)
        for (source in listOf("Google", "LDNOOBW", "SIL Open Font License", "Phosphor")) {
            scrollTo(source, substring = true)
        }
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithText("Tìm hiểu", ignoreCase = true).assertExists()
    }

    private fun scrollTo(text: String, substring: Boolean) =
        compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(hasText(text, substring = substring))
}
