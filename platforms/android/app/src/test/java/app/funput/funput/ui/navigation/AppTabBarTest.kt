package app.funput.funput.ui.navigation

import android.content.Context
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import app.funput.funput.R
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The app's tab bar, around a screen not yet rebuilt on FunputUI, drives the navigator. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK])
class AppTabBarTest {
    @get:Rule
    val compose = createComposeRule()

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    private fun label(id: Int) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    @Test
    fun `the legacy host shows the three tabs and switching one moves the navigator`() {
        lateinit var navigator: AppNavigator
        compose.setContent {
            navigator = rememberAppNavigator()
            FunputUiTheme(isDark = false) { LegacyTabHost(navigator) { BasicText("Màn cũ") } }
        }

        compose.onNodeWithText("Màn cũ").assertExists()
        compose.onNode(hasText(label(R.string.nav_settings)) and isTab).assertIsSelected()
        compose.onNode(hasText(label(R.string.nav_about)) and isTab).performClick()

        assertEquals(TopLevelDestination.ABOUT, navigator.currentTab)
    }
}
