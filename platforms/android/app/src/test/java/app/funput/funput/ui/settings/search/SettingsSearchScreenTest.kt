package app.funput.funput.ui.settings.search

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.hub.KeyboardHubScreen
import app.funput.funput.ui.settings.hub.SettingsAllTag
import app.funput.funput.ui.settings.hub.SettingsHeroTag
import app.funput.funput.ui.settings.pages.SettingsPage
import app.funput.funput.ui.settings.testSettingsState
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Typing in the hub's search swaps the hub for results, and back or clearing brings it back. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class SettingsSearchScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<SettingsPage>()

    @Before
    fun show() = compose.setContent {
        FunputUiTheme(isDark = false) { KeyboardHubScreen(testSettingsState(), onOpenPage = { opened += it }) }
    }

    private fun search(query: String) = compose.onNodeWithTag(SettingsSearchFieldTag).performTextInput(query)

    @Test
    fun `a query replaces the hub with results that open their page`() {
        search("rung")

        compose.onNodeWithTag(SettingsHeroTag).assertDoesNotExist()
        compose.onNodeWithTag(SettingsAllTag).assertDoesNotExist()
        compose.onNodeWithText("Phản hồi khi chạm").assertExists()
        compose.onNodeWithText("Rung khi nhấn phím").performClick()

        compose.runOnIdle { assertEquals(listOf(SettingsPage.FEEDBACK), opened) }
    }

    @Test
    fun `nothing found says so and suggests words to try`() {
        search("zzz")

        compose.onNodeWithText("Không tìm thấy cài đặt").assertExists()
    }

    @Test
    fun `clearing the query brings the hub back`() {
        search("rung")
        compose.onNodeWithContentDescription("Xoá tìm kiếm").performClick()

        compose.onNodeWithTag(SettingsAllTag).assertExists()
    }

    @Test
    fun `back clears the query before anything else`() {
        search("rung")
        // Let the hub register its back handler for the query before pressing back.
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }

        compose.onNodeWithTag(SettingsSearchResultsTag).assertDoesNotExist()
        compose.onNodeWithTag(SettingsHeroTag).assertExists()
    }
}
