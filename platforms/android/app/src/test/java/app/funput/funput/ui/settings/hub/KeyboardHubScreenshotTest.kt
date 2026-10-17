package app.funput.funput.ui.settings.hub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.ui.kit.glass.GlassTier
import app.funput.funput.ui.kit.glass.LocalGlassTier
import app.funput.funput.ui.kit.layout.FunputScreenListTag
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.APP_SCREENSHOT_ROOT
import app.funput.funput.ui.settings.pages.SettingsPage
import app.funput.funput.ui.settings.pages.SettingsPageScreen
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
import app.funput.funput.ui.settings.testSettingsState
import app.funput.funput.uitesting.SCREENSHOT_SDK
import app.funput.funput.uitesting.ScreenshotDevices
import app.funput.funput.uitesting.ScreenshotVariant
import app.funput.funput.uitesting.captureScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The keyboard hub on first launch and ready, and the typing, layout, smart and data pages, in
 * every appearance and font scale.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class KeyboardHubScreenshotTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun setup() = capture("settings-setup") {
        KeyboardHubScreen(testSettingsState(KeyboardSetupStatus.NOT_ENABLED), onOpenPage = {})
    }

    @Test
    fun hub() = capture("settings-hub", prepare = {
        onNodeWithTag(FunputScreenListTag).performScrollToNode(hasText("TẤT CẢ CÀI ĐẶT"))
    }) { KeyboardHubScreen(testSettingsState(), onOpenPage = {}) }

    @Test
    fun typing() = capture("settings-typing") { PageScreen(SettingsPage.TYPING) }

    @Test
    fun layout() = capture("settings-layout") { PageScreen(SettingsPage.LAYOUT) }

    @Test
    fun smart() = capture("settings-smart", prepare = {
        onNodeWithTag(FunputScreenListTag).performScrollToNode(hasText("Tự về bảng chữ"))
    }) { PageScreen(SettingsPage.SMART_INPUT) }

    @Test
    fun data() = capture("settings-data") { PageScreen(SettingsPage.DATA) }

    @Composable
    private fun PageScreen(page: SettingsPage) = SettingsPageScreen(page, testSettingsState(), onBack = {})

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
