package app.funput.funput.ui.settings.groups

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.clipboard.ClipboardSection
import app.funput.funput.ui.settings.feedback.FeedbackSection
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The clipboard and feedback groups show their state and dispatch their actions. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class ClipboardAndFeedbackTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the clipboard group shows its expiry and dispatches both actions`() {
        var enabled = true
        var openedExpiry = false
        compose.setContent {
            FunputUiTheme(isDark = false) {
                ClipboardSection(
                    enabled = true,
                    expiry = ClipboardExpiry.DAY,
                    onEnabledChanged = { enabled = it },
                    onOpenExpiry = { openedExpiry = true },
                )
            }
        }

        compose.onNodeWithText("CLIPBOARD").assertExists()
        compose.onNodeWithText("1 ngày").assertExists()
        compose.onNodeWithText("Lưu lịch sử bảng nhớ tạm").performClick()
        compose.onNodeWithText("Tự xoá sau").performClick()

        assertFalse(enabled)
        assertTrue(openedExpiry)
    }

    @Test
    fun `a narrow screen still shows the clipboard preferences`() {
        compose.setContent {
            FunputUiTheme(isDark = true) {
                Box(Modifier.width(320.dp)) {
                    ClipboardSection(
                        enabled = true,
                        expiry = ClipboardExpiry.WEEK,
                        onEnabledChanged = {},
                        onOpenExpiry = {},
                    )
                }
            }
        }

        compose.onNodeWithText("Lưu lịch sử bảng nhớ tạm").assertIsDisplayed()
        compose.onNodeWithText("1 tuần").assertIsDisplayed()
    }

    @Test
    fun `feedback switches reflect and change their state`() {
        val haptics = mutableListOf<Boolean>()
        compose.setContent {
            FunputUiTheme(isDark = false) {
                FeedbackSection(
                    hapticsEnabled = true,
                    soundsEnabled = false,
                    onHapticsChanged = haptics::add,
                    onSoundsChanged = {},
                )
            }
        }

        compose.onNodeWithText("Âm thanh phím").assertIsOff()
        compose.onNodeWithText("Rung khi nhấn phím").assertIsOn().performClick()

        assertEquals(listOf(false), haptics)
    }
}
