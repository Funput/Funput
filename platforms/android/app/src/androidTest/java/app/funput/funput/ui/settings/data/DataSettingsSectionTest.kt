package app.funput.funput.ui.settings.data

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DataSettingsSectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun cancellingSuggestionResetKeepsData() {
        var resets = 0
        setSection(onResetSuggestions = { resets += 1 })

        compose.onNodeWithText("Từ đã học").performClick()
        compose.onNodeWithText("Xóa các từ đã học?").assertExists()
        compose.onNodeWithText("Giữ lại").performClick()

        compose.runOnIdle { assertEquals(0, resets) }
    }

    @Test
    fun confirmingSuggestionResetDispatchesOnce() {
        var resets = 0
        setSection(onResetSuggestions = { resets += 1 })

        compose.onNodeWithText("Từ đã học").performClick()
        compose.onNodeWithText("Xóa từ").performClick()

        compose.runOnIdle { assertEquals(1, resets) }
    }

    @Test
    fun confirmingClipboardClearDispatchesOnce() {
        var clears = 0
        setSection(onClearClipboard = { clears += 1 })

        compose.onNodeWithText("Lịch sử bảng nhớ tạm").performClick()
        compose.onNodeWithText("Xoá tất cả").performClick()

        compose.runOnIdle { assertEquals(1, clears) }
    }

    private fun setSection(
        onResetSuggestions: () -> Unit = {},
        onClearClipboard: () -> Unit = {},
    ) {
        compose.setContent {
            FunputTheme {
                DataSettingsSection(
                    onResetPersonalSuggestions = onResetSuggestions,
                    onClearClipboardHistory = onClearClipboard,
                )
            }
        }
    }
}
