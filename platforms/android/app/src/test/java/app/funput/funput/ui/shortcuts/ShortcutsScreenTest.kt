package app.funput.funput.ui.shortcuts

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import app.funput.funput.shortcuts.model.ShortcutLibrary
import app.funput.funput.shortcuts.persistence.ShortcutsStorageError
import app.funput.funput.uitesting.SCREENSHOT_SDK
import app.funput.funput.uitesting.ScreenshotDevices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The shortcuts screen end to end against an in-memory store, in Vietnamese as users see it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class ShortcutsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun editorClosed() =
        compose.onAllNodesWithTag("shortcuts-editor-trigger").fetchSemanticsNodes().isEmpty()

    @Test
    fun `search filters case-insensitively and says when nothing matches`() {
        compose.showShortcuts(ShortcutLibrary(entries = listOf(testShortcut("vn", "việt nam"))))

        compose.onNodeWithTag(ShortcutsSearchTag).performTextInput("NAM")
        compose.onNodeWithText("việt nam").assertExists()
        compose.onNodeWithText("Kết quả · 1/1 mục", ignoreCase = true).assertExists()
        compose.onNodeWithContentDescription("Xoá tìm kiếm").performClick()
        compose.onNodeWithText("Danh sách · 1 mục", ignoreCase = true).assertExists()
        compose.onNodeWithTag(ShortcutsSearchTag).performTextInput("-none")
        compose.onNodeWithText("Không tìm thấy gõ tắt").assertExists()
    }

    @Test
    fun `adding refuses a duplicate trigger, then saves and closes`() {
        val store = compose.showShortcuts(ShortcutLibrary(entries = listOf(testShortcut("vn", "việt nam"))))

        compose.onNodeWithContentDescription("Thêm gõ tắt").performClick()
        compose.onNodeWithTag("shortcuts-editor-trigger").performTextInput("vn")
        compose.onNodeWithTag("shortcuts-editor-expansion").performTextInput("duplicate")
        compose.onNodeWithText("Chữ tắt này đã có trong danh sách. Hãy chọn chữ tắt khác.").assertExists()
        compose.onNodeWithText("Lưu").assertIsNotEnabled()
        compose.onNodeWithTag("shortcuts-editor-trigger").performTextInput("X")
        compose.onNodeWithText("Lưu").performClick()

        compose.waitUntil { store.value.entries.size == 2 }
        assertEquals("vnX", store.value.entries.last().trigger)
        compose.waitUntil { editorClosed() }
    }

    @Test
    fun `the empty state offers to add the first shortcut`() {
        compose.showShortcuts(ShortcutLibrary())

        compose.onNodeWithText("Chưa có gõ tắt").assertExists()
        compose.onNodeWithText("Thêm gõ tắt").performClick()
        compose.onNodeWithTag("shortcuts-editor-trigger").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `tapping a row edits it and swiping asks before deleting`() {
        val entry = testShortcut("dc", "được")
        val store = compose.showShortcuts(ShortcutLibrary(entries = listOf(entry)))
        val row = compose.onNodeWithTag("shortcuts-entry-${entry.id}")

        compose.onNodeWithText("dc").performClick()
        compose.onNodeWithText("Sửa gõ tắt").assertExists()
        compose.onNodeWithText("Huỷ").performClick()
        compose.waitUntil { editorClosed() }

        row.performTouchInput { swipeLeft() }
        compose.onNodeWithText("Xoá gõ tắt?").assertExists()
        compose.onNodeWithText("Huỷ").performClick()
        compose.runOnIdle { assertEquals(0, store.saveCount) }

        row.performCustomAccessibilityActionWithLabel("Xoá")
        compose.onNodeWithText("Xoá").performClick()
        compose.waitUntil { store.value.entries.isEmpty() }
        assertEquals(1, store.saveCount)
    }

    @Test
    fun `options persist, and a dirty editor asks before closing`() {
        val store = compose.showShortcuts(ShortcutLibrary())

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
    fun `a failed write rolls the option back and reports it`() {
        val store = compose.showShortcuts(ShortcutLibrary())
        store.saveFailure = ShortcutsStorageError.WriteFailed

        compose.onNodeWithContentDescription("Tuỳ chọn").performClick()
        compose.onNodeWithText("Tự nhận diện hoa/thường").performClick()

        compose.waitUntil { hasText("Không thể lưu Gõ tắt") }
        compose.runOnIdle { assertTrue(store.value.smartCase) }
        compose.onNodeWithText("Đóng").performClick()
        compose.onNodeWithText("Tự nhận diện hoa/thường").assertIsOn()
    }

    private fun hasText(text: String) =
        runCatching { compose.onNodeWithText(text).assertExists() }.isSuccess
}
