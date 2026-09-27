package app.funput.funput.ui.settings.groups

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.data.DataSection
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Erasing data always asks first; cancelling keeps it, confirming erases exactly once. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class DataSectionTest {
    @get:Rule
    val compose = createComposeRule()

    private var resets = 0
    private var clears = 0

    private fun show() = compose.setContent {
        FunputUiTheme(isDark = false) {
            DataSection(onResetPersonalSuggestions = { resets += 1 }, onClearClipboardHistory = { clears += 1 })
        }
    }

    @Test
    fun `cancelling the suggestion reset keeps the data`() {
        show()

        compose.onNodeWithText("Từ đã học").performClick()
        compose.onNodeWithText("Xóa các từ đã học?").assertExists()
        compose.onNodeWithText("Giữ lại").performClick()

        assertEquals(0, resets)
        compose.onNodeWithText("Xóa các từ đã học?").assertDoesNotExist()
    }

    @Test
    fun `confirming the suggestion reset dispatches once`() {
        show()

        compose.onNodeWithText("Từ đã học").performClick()
        compose.onNodeWithText("Xóa từ").performClick()

        assertEquals(1, resets)
    }

    @Test
    fun `confirming the clipboard clear dispatches once`() {
        show()

        compose.onNodeWithText("Lịch sử bảng nhớ tạm").performClick()
        compose.onNodeWithText("Xoá tất cả").performClick()

        assertEquals(1, clears)
    }
}
