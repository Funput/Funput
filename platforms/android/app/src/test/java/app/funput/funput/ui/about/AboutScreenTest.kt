package app.funput.funput.ui.about

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.ui.kit.glass.GlassTier
import app.funput.funput.ui.kit.glass.LocalGlassTier
import app.funput.funput.ui.kit.layout.FunputScreenListTag
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.uitesting.SCREENSHOT_SDK
import app.funput.funput.uitesting.ScreenshotDevices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The about page's links open where they say, and the licences row opens the licences. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class AboutScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `each link opens its own address`() {
        val opened = mutableListOf<String>()
        var licenses = 0
        compose.setContent {
            FunputUiTheme(isDark = false) {
                CompositionLocalProvider(LocalGlassTier provides GlassTier.SOLID) {
                    AboutScreen(versionName = "1.2026.70", onOpenLink = { opened += it }, onOpenLicenses = { licenses += 1 })
                }
            }
        }

        for (title in listOf("Website", "GitHub", "Báo lỗi", "Liên hệ", "Chính sách quyền riêng tư", "Giấy phép bên thứ ba")) {
            compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(hasText(title))
            compose.onNodeWithText(title).performClick()
        }

        compose.runOnIdle {
            assertEquals(
                listOf(
                    "https://funput.app/",
                    "https://github.com/Funput/Funput",
                    "https://github.com/Funput/Funput/issues",
                    "mailto:hello@funput.app",
                    "https://funput.app/privacy",
                ),
                opened,
            )
            assertTrue(licenses == 1)
        }
    }
}
