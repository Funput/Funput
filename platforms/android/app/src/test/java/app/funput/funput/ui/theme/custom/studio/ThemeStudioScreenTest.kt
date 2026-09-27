package app.funput.funput.ui.theme.custom.studio

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
import app.funput.funput.theme.store.custom.CustomThemeDraft
import app.funput.funput.ui.theme.custom.color.picker.ColorPickerHexTag
import app.funput.funput.ui.theme.custom.color.picker.ColorPickerState
import app.funput.funput.ui.theme.custom.draft.withAccent
import app.funput.funput.uitesting.SCREENSHOT_SDK
import app.funput.funput.uitesting.ScreenshotDevices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The theme studio end to end, from the controls a user touches to the draft that gets saved. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class ThemeStudioScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val light = studioBaseThemes.single { theme -> theme.id == KeyboardThemeId.Light }

    @Test
    fun `name, base and accent chosen on the first page are what gets saved`() {
        var saved: CustomThemeDraft? = null
        compose.setContent { ThemeStudioTestHost(onSave = { saved = it }) }

        // A new theme arrives named, so saving is never blocked on the least interesting decision.
        compose.onNodeWithText("Lưu chủ đề").assertIsEnabled()
        compose.onNodeWithTag(ThemeNameTag).performTextClearance()
        compose.onNodeWithText("Lưu chủ đề").assertIsNotEnabled()
        compose.onNodeWithTag(ThemeNameTag).performTextInput("Ocean")
        compose.onNodeWithText("Bắt đầu từ").performClick()
        compose.onAllNodesWithText(light.name).onLast().performClick()
        compose.onNodeWithTag(AccentColorTag).performClick()
        compose.onNodeWithTag(ColorPickerHexTag).performTextReplacement("2F9BFF")
        compose.onNodeWithText("Chọn").performClick()
        compose.onNodeWithText("Lưu chủ đề").performClick()

        compose.runOnIdle {
            assertEquals("Ocean", saved?.name)
            assertEquals(KeyboardThemeId.Light, saved?.baseThemeId)
            // The re-dye is proved against every base in ThemeRecolorTest; this proves the colour
            // typed into the picker is the accent that got saved.
            assertEquals(ColorPickerState.fromHexOrNull("2F9BFF", alpha = 1f)!!.argb, saved?.theme?.accentColor)
            assertNotEquals(light.theme, saved?.theme)
        }
    }

    @Test
    fun `saving an opened theme untouched drifts no token`() {
        var saved: CustomThemeDraft? = null
        val editing = KeyboardThemeDescriptor(
            id = KeyboardThemeId.of("custom.ocean"),
            version = 1,
            name = "Ocean",
            author = "Me",
            origin = KeyboardThemeOrigin.CUSTOM,
            baseThemeId = KeyboardThemeId.Light,
            theme = light.theme.withAccent(0xFF9F5CFF.toInt()),
        )
        compose.setContent { ThemeStudioTestHost(editingTheme = editing, onSave = { saved = it }) }

        compose.onNodeWithText("Sửa chủ đề").assertExists()
        compose.onNodeWithText("Lưu chủ đề").performClick()
        compose.runOnIdle {
            assertEquals("Ocean", saved?.name)
            assertEquals(KeyboardThemeId.Light, saved?.baseThemeId)
            assertEquals(editing.theme, saved?.theme)
        }
    }

    @Test
    fun `after an accent is chosen, the keys page shows what follows it`() {
        compose.setContent { ThemeStudioTestHost() }
        compose.onNodeWithTag(AccentColorTag).performClick()
        compose.onNodeWithTag(ColorPickerHexTag).performTextReplacement("2F9BFF")
        compose.onNodeWithText("Chọn").performClick()

        compose.onNodeWithText("Phím & chữ").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Chữ chính").assertExists()
        // The Enter key and the leading suggestion both took the new accent and keep following it.
        compose.onAllNodesWithText("Tự động · theo Màu nhấn").assertCountEquals(2)
    }
}
