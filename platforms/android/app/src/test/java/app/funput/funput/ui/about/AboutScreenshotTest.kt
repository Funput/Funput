package app.funput.funput.ui.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.ui.about.licenses.LicensesRoute
import app.funput.funput.ui.kit.glass.GlassTier
import app.funput.funput.ui.kit.glass.LocalGlassTier
import app.funput.funput.ui.kit.layout.FunputScreenListTag
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.APP_SCREENSHOT_ROOT
import app.funput.funput.uitesting.SCREENSHOT_SDK
import app.funput.funput.uitesting.ScreenshotDevices
import app.funput.funput.uitesting.ScreenshotVariant
import app.funput.funput.uitesting.captureScreen
import app.funput.funput.uitesting.waitForText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The about page at its top and its end, and the licences page, in every appearance and font scale. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class AboutScreenshotTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun top() = capture("about-top") { AboutScreen(versionName = "1.2026.70", onOpenLink = {}) }

    @Test
    fun end() = capture("about-end", prepare = {
        onNodeWithTag(FunputScreenListTag).performScrollToNode(hasText("Được xây dựng cho cộng đồng"))
    }) { AboutScreen(versionName = "1.2026.70", onOpenLink = {}) }

    @Test
    fun licenses() = capture("licenses", prepare = { waitForText("SCOWL", substring = true) }) {
        LicensesRoute(onBack = {})
    }

    private fun capture(
        screen: String,
        prepare: ComposeContentTestRule.() -> Unit = {},
        content: @Composable () -> Unit,
    ) = compose.captureScreen(screen, variant, root = APP_SCREENSHOT_ROOT, prepare = prepare) {
        FunputUiTheme(isDark = variant.isDark) {
            CompositionLocalProvider(LocalGlassTier provides GlassTier.SOLID) { content() }
        }
    }

    companion object {
        /** Every appearance × font-scale combination. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants(): List<Array<Any>> = ScreenshotVariant.parameters()
    }
}
