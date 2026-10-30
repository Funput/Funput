package app.funput.funput.ui.settings.enterkey

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import app.funput.funput.ui.kit.glass.GlassTier
import app.funput.funput.ui.kit.glass.LocalGlassTier
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.APP_SCREENSHOT_ROOT
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

/** The Enter key page empty and with apps chosen, in every appearance and font scale. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class EnterKeyScreenshotTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val compose = createComposeRule()
    private val installed = listOf(
        InstalledApp("com.facebook.orca", "Messenger", null),
        InstalledApp("org.telegram.messenger", "Telegram", null),
    )

    @Test
    fun empty() = capture("enter-key-empty", emptySet())

    @Test
    fun chosen() = capture("enter-key-chosen", setOf("org.telegram.messenger", "com.example.removed"))

    private fun capture(screen: String, packages: Set<String>) =
        compose.captureScreen(screen, variant, root = APP_SCREENSHOT_ROOT) {
            FunputUiTheme(isDark = variant.isDark) {
                CompositionLocalProvider(LocalGlassTier provides GlassTier.SOLID) {
                    EnterKeyScreen(EnterKeyScreenState(packages, installed, onAdd = {}, onRemove = {}), onBack = {})
                }
            }
        }

    companion object {
        /** Every appearance × font-scale combination. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants(): List<Array<Any>> = ScreenshotVariant.parameters()
    }
}
