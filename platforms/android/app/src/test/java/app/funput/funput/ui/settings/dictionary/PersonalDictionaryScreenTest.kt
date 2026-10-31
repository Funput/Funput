package app.funput.funput.ui.settings.dictionary

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import app.funput.funput.ime.suggestions.dictionary.DictionaryEditKind
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.dictionary.add.DictionaryAddFieldTag
import app.funput.funput.ui.settings.dictionary.components.dictionaryWordTag
import app.funput.funput.ui.settings.dictionary.model.PersonalDictionaryScreenModel
import app.funput.funput.uitesting.SCREENSHOT_SDK
import app.funput.funput.uitesting.ScreenshotDevices
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The dictionary page end to end against an in-memory store, in Vietnamese as users see it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class PersonalDictionaryScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the lists switch by their counts and the blocked one has nothing to sort`() {
        compose.showDictionary(DictionaryTestStore(learned("được", 4), added("Funput"), blocked("đm")))

        compose.onNodeWithText("được").assertExists()
        compose.onNodeWithText("Dùng nhiều nhất").assertExists()
        compose.onNodeWithText("Tự thêm 1").performClick()
        compose.waitUntil { compose.onAllText("Funput") }
        compose.onNodeWithText("Đã chặn 1").performClick()
        compose.waitUntil { compose.onAllText("đm") }
        compose.onNodeWithText("Dùng nhiều nhất").assertDoesNotExist()
    }

    @Test
    fun `adding explains a spaced word, then saves and closes`() {
        val store = compose.showDictionary(DictionaryTestStore(learned("được")))

        compose.onNodeWithContentDescription("Thêm từ").performClick()
        compose.onNodeWithTag(DictionaryAddFieldTag).performTextInput("hai từ")
        compose.onNodeWithText("Mỗi lần thêm một từ, không có dấu cách. Muốn thêm cả cụm, hãy dùng Gõ tắt.").assertExists()
        compose.onNodeWithText("Thêm").assertIsNotEnabled()
        compose.onNodeWithTag(DictionaryAddFieldTag).performTextReplacement("Funput")
        compose.onNodeWithText("Thêm").assertIsEnabled().performClick()

        compose.waitUntil { store.edits.isNotEmpty() }
        assertEquals(DictionaryEditKind.ADD to "Funput", store.edits.single())
        compose.waitUntil { !compose.onAllText("Thanh gợi ý sẽ hiện") }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `forgetting asks first and blocking does not`() {
        val store = compose.showDictionary(DictionaryTestStore(learned("được"), learned("không")))

        compose.onNodeWithTag(dictionaryWordTag("được")).performCustomAccessibilityActionWithLabel("Chặn")
        compose.waitUntil { store.edits.isNotEmpty() }
        compose.onNodeWithTag(dictionaryWordTag("không")).performCustomAccessibilityActionWithLabel("Xoá")
        compose.onNodeWithText("Xoá “không” khỏi từ điển?").assertExists()
        compose.onNodeWithText("Xoá").performClick()

        compose.waitUntil { store.edits.size == 2 }
        assertEquals(listOf(DictionaryEditKind.BLOCK to "được", DictionaryEditKind.FORGET to "không"), store.edits)
    }

    @Test
    fun `a pending wipe hides the words and cannot be asked for twice`() {
        compose.showDictionary(DictionaryTestStore(learned("được")), resetPending = true)

        compose.onNodeWithText("Các từ đã học sẽ được xoá khi bạn mở bàn phím lần tới. Danh sách từ bị chặn vẫn được giữ.")
            .assertExists()
        compose.onNodeWithText("Chưa có từ nào").assertExists()
        compose.onNodeWithContentDescription("Xoá toàn bộ từ đã học").assertIsNotEnabled()
    }

    private fun ComposeContentTestRule.showDictionary(
        store: DictionaryTestStore,
        resetPending: Boolean = false,
    ): DictionaryTestStore {
        setContent {
            val scope = rememberCoroutineScope()
            val model = remember { PersonalDictionaryScreenModel(store, scope).also { it.reload() } }
            FunputUiTheme(isDark = false) {
                PersonalDictionaryScreen(model, DictionarySettings(true, resetPending, {}, {}), onBack = {})
            }
        }
        waitForIdle()
        return store
    }

    private fun ComposeContentTestRule.onAllText(text: String): Boolean =
        onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
}
