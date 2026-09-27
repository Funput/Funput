package app.funput.funput.ui.appearance

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ime.settings.KeyboardThemeSlot
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.kit.glass.GlassTier
import app.funput.funput.ui.kit.glass.LocalGlassTier
import app.funput.funput.ui.kit.layout.FunputScreenListTag
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.uitesting.SCREENSHOT_SDK
import app.funput.funput.uitesting.ScreenshotDevices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The appearance tab's controls and the theme gallery's actions, in Vietnamese as users see them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class AppearanceScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(state: AppearanceScreenState) = compose.setContent {
        FunputUiTheme(isDark = false) {
            CompositionLocalProvider(LocalGlassTier provides GlassTier.SOLID) { AppearanceScreen(state) }
        }
    }

    private fun scrollToTheme(id: KeyboardThemeId) =
        compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(hasTestTag(id.value))

    @Test
    fun `the app mode is one tap, and wallpaper colour is gone`() {
        var mode: AppearanceMode? = null
        show(testAppearanceState(onAppearanceSelected = { mode = it }))

        compose.onNodeWithText("Hệ thống").assertIsSelected()
        compose.onNodeWithText("Tối").performClick()
        compose.onNodeWithText("Màu theo hình nền").assertDoesNotExist()
        compose.runOnIdle { assertEquals(AppearanceMode.DARK, mode) }
    }

    @Test
    fun `the slot choice names the theme in each slot`() {
        var slot: KeyboardThemeSlot? = null
        show(testAppearanceState(followsAppearance = true, activeSlot = KeyboardThemeSlot.LIGHT, onSlotSelected = { slot = it }))

        compose.onNodeWithText("Sáng · Paper").assertIsSelected()
        compose.onNodeWithText("Tối · Ink").performClick()
        compose.runOnIdle { assertEquals(KeyboardThemeSlot.DARK, slot) }
    }

    @Test
    fun `tapping a card selects its theme, the one in use is marked`() {
        var selected: KeyboardThemeId? = null
        show(testAppearanceState(onThemeSelected = { selected = it }))

        scrollToTheme(KeyboardThemeId.Dark)
        compose.onNodeWithTag(KeyboardThemeId.Dark.value).assertIsSelected()
        scrollToTheme(KeyboardThemeId.Slate)
        compose.onNodeWithTag(KeyboardThemeId.Slate.value).assertIsNotSelected().performClick()
        compose.runOnIdle { assertEquals(KeyboardThemeId.Slate, selected) }
    }

    @Test
    fun `a custom theme edits and deletes from its sheet, deleting only once confirmed`() {
        var edited: KeyboardThemeId? = null
        var deleted: KeyboardThemeId? = null
        show(testAppearanceState(extraThemes = listOf(testCustomTheme), onEditTheme = { edited = it }, onDeleteTheme = { deleted = it }))
        scrollToTheme(testCustomTheme.id)

        compose.onNodeWithContentDescription("Tuỳ chọn cho chủ đề Ocean").performClick()
        compose.onNodeWithText("Sửa").performClick()
        compose.runOnIdle { assertEquals(testCustomTheme.id, edited) }

        compose.onNodeWithContentDescription("Tuỳ chọn cho chủ đề Ocean").performClick()
        compose.onNodeWithText("Xóa").performClick()
        compose.onNodeWithText("Giữ lại").performClick()
        compose.runOnIdle { assertNull(deleted) }

        compose.onNodeWithContentDescription("Tuỳ chọn cho chủ đề Ocean").performClick()
        compose.onNodeWithText("Xóa").performClick()
        compose.onNodeWithText("Xóa chủ đề").performClick()
        compose.runOnIdle { assertEquals(testCustomTheme.id, deleted) }
    }

    @Test
    fun `built-in themes have no actions, and both create buttons create`() {
        var creates = 0
        show(testAppearanceState(onCreateTheme = { creates += 1 }))

        compose.onNodeWithContentDescription("Tuỳ chọn cho chủ đề Ink").assertDoesNotExist()
        compose.onNodeWithTag(CreateThemeTag).performClick()
        compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(hasText("Chưa có chủ đề nào của bạn"))
        compose.onNodeWithText("Tạo chủ đề riêng").performClick()
        compose.runOnIdle { assertTrue(creates == 2) }
    }
}
