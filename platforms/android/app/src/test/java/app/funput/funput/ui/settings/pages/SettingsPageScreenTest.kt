package app.funput.funput.ui.settings.pages

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.ui.kit.layout.FunputScreenListTag
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.SettingsScreenState
import app.funput.funput.ui.settings.testSettingsState
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A settings page shows its own sections, names itself once, and opens its own pickers. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class SettingsPageScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(page: SettingsPage, state: SettingsScreenState = testSettingsState()) = compose.setContent {
        FunputUiTheme(isDark = false) { SettingsPageScreen(page, state, onBack = {}) }
    }

    @Test
    fun `the typing page holds the input method, tone style and extra onsets, and no shortcuts`() {
        var tone: ToneStyle? = null
        show(SettingsPage.TYPING, testSettingsState(onToneStyleSelected = { tone = it }))

        // The large title and the top bar both carry it.
        compose.onAllNodesWithText("Gõ tiếng Việt").onFirst().assertExists()
        // The section titled like the page drops its header; the extra onsets keep theirs.
        compose.onNodeWithText("GÕ TIẾNG VIỆT").assertDoesNotExist()
        compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(hasText("PHỤ ÂM ĐẦU MỞ RỘNG"))
        compose.onNodeWithText("PHỤ ÂM ĐẦU MỞ RỘNG").assertExists()
        compose.onNodeWithText("Gõ tắt").assertDoesNotExist()

        compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(hasText("Hiện đại"))
        compose.onNodeWithText("Hiện đại").performClick()
        assertEquals(ToneStyle.MODERN, tone)
    }

    @Test
    fun `a page opens its own picker`() {
        show(SettingsPage.TYPING)

        compose.onNodeWithText("Kiểu gõ").performClick()

        compose.onNodeWithText("Dùng các phím số để nhập dấu.").assertIsDisplayed()
    }

    @Test
    fun `the data page holds what can be erased`() {
        show(SettingsPage.DATA)

        compose.onNodeWithText("DỮ LIỆU").assertDoesNotExist()
        compose.onNodeWithText("Từ đã học").assertExists()
    }
}
