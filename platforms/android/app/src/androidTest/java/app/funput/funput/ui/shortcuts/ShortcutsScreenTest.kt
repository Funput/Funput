package app.funput.funput.ui.shortcuts

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import app.funput.funput.shortcuts.model.ShortcutLibrary
import app.funput.funput.shortcuts.persistence.ShortcutsStorageError
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ShortcutsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun searchAddAndCaseSensitiveDuplicateValidation() {
        val store = show(ShortcutLibrary(entries = listOf(testShortcut("vn", "việt nam"))))
        compose.onNodeWithTag("shortcuts-search").performTextInput("NAM")
        compose.onNodeWithText("việt nam").assertExists()
        compose.onNodeWithTag("shortcuts-search").performTextInput("-none")
        compose.onNodeWithText("Không tìm thấy gõ tắt").assertExists()

        compose.onNodeWithContentDescription("Thêm gõ tắt").performClick()
        compose.onNodeWithTag("shortcuts-editor-trigger").performTextInput("vn")
        compose.onNodeWithTag("shortcuts-editor-expansion").performTextInput("duplicate")
        compose.onNodeWithText("Chữ tắt này đã có trong danh sách. Hãy chọn chữ tắt khác.")
            .assertExists()
        compose.onNodeWithText("Lưu").assertIsNotEnabled()
        compose.onNodeWithTag("shortcuts-editor-trigger").performTextInput("X")
        compose.onNodeWithText("Lưu").performClick()

        compose.waitUntil { store.value.entries.size == 2 }
        assertEquals("vnX", store.value.entries.last().trigger)
    }

    @Test
    fun swipeDeleteRequiresConfirmationAndCancelPreservesEntry() {
        val entry = testShortcut("dc", "được")
        val store = show(ShortcutLibrary(entries = listOf(entry)))
        val row = compose.onNodeWithTag("shortcuts-entry-${entry.id}")
        row.performTouchInput { swipeLeft() }
        compose.onNodeWithText("Huỷ").performClick()
        compose.runOnIdle { assertEquals(0, store.saveCount) }

        row.performTouchInput { swipeLeft() }
        compose.onNodeWithText("Xoá").performClick()
        compose.waitUntil { store.value.entries.isEmpty() }
        assertEquals(1, store.saveCount)
    }

    @Test
    fun optionsPersistAndDirtyEditorAsksBeforeDismiss() {
        val store = show(ShortcutLibrary())
        compose.onNodeWithContentDescription("Tuỳ chọn").performClick()
        compose.onNodeWithText("Tự nhận diện hoa/thường").performClick()
        compose.waitUntil { !store.value.smartCase }
        compose.onNodeWithText("Gõ tắt khi dùng tiếng Anh").assertIsOn()
        compose.onNodeWithText("Xong").performClick()

        compose.onNodeWithContentDescription("Thêm gõ tắt").performClick()
        compose.onNodeWithTag("shortcuts-editor-trigger").performTextInput("bt")
        compose.onNodeWithText("Huỷ").performClick()
        compose.onNodeWithText("Bỏ thay đổi?").assertExists()
        compose.onNodeWithText("Tiếp tục sửa").performClick()
        compose.onNodeWithTag("shortcuts-editor-trigger").assertExists()
    }

    @Test
    fun optionWriteFailureRollsBackAndReportsError() {
        val store = show(ShortcutLibrary()).apply {
            saveFailure = ShortcutsStorageError.WriteFailed
        }
        compose.onNodeWithContentDescription("Tuỳ chọn").performClick()
        compose.onNodeWithText("Tự nhận diện hoa/thường").performClick()

        compose.onNodeWithText("Không thể lưu Gõ tắt").assertExists()
        compose.runOnIdle { assert(store.value.smartCase) }
        compose.onNodeWithText("Đóng").performClick()
        compose.onNodeWithText("Tự nhận diện hoa/thường").assertIsOn()
    }

    @Test
    fun focusedExpansionRemainsVisibleWithSoftwareKeyboard() {
        show(ShortcutLibrary())
        compose.onNodeWithContentDescription("Thêm gõ tắt").performClick()

        compose.onNodeWithTag("shortcuts-editor-expansion").performClick()
        compose.onNodeWithTag("shortcuts-editor-expansion").performTextInput("Nội dung dài")

        compose.onNodeWithTag("shortcuts-editor-expansion").assertIsDisplayed()
    }

    private fun show(initial: ShortcutLibrary): ShortcutTestStore {
        val store = ShortcutTestStore(initial)
        compose.setContent {
            val scope = rememberCoroutineScope()
            val model = remember { ShortcutsScreenModel(store, scope) }
            LaunchedEffect(model) { model.reload() }
            FunputTheme { ShortcutsScreen(model, {}) }
        }
        compose.waitUntil { compose.onAllNodesWithText("Bật gõ tắt").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil { compose.onAllNodesWithText("Danh sách · ${initial.entries.size} mục")
            .fetchSemanticsNodes().isNotEmpty() }
        return store
    }
}
