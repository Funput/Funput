package app.funput.funput.ui.shortcuts

import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import app.funput.funput.shortcuts.model.ShortcutLibrary
import app.funput.funput.uitesting.SCREENSHOT_SDK
import app.funput.funput.uitesting.ScreenshotDevices
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Where the empty library's message sits on screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class ShortcutsEmptyStateTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the empty state is centred, not parked on the left`() {
        compose.showShortcuts(ShortcutLibrary())

        val screen = compose.onRoot().getBoundsInRoot()
        val screenCentre = (screen.left + screen.right) / 2
        listOf("Chưa có gõ tắt", "Thêm gõ tắt").forEach { text ->
            val bounds = compose.onNodeWithText(text).getBoundsInRoot()
            val offset = ((bounds.left + bounds.right) / 2 - screenCentre).value
            assertTrue("$text is ${offset}dp off centre", abs(offset) < 1f)
        }
    }
}
