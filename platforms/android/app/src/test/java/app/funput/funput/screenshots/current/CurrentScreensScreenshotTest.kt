package app.funput.funput.screenshots.current

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.theme.BuiltInKeyboardThemeSource
import app.funput.funput.ui.about.AboutScreen
import app.funput.funput.ui.appearance.AppearanceScreen
import app.funput.funput.ui.appearance.appearancePreviewState
import app.funput.funput.ui.settings.SettingsPreview
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
import app.funput.funput.ui.shortcuts.ShortcutsRoute
import app.funput.funput.ui.theme.FunputTheme
import app.funput.funput.ui.theme.custom.CreateCustomThemeScreen
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
 * The app's screens as they stand before the redesign, kept as the "before" evidence.
 *
 * Nothing here is compared against a golden: these screens are about to be replaced, so the images
 * go to a CI artifact instead of the repository. A plain unit-test run still composes every screen
 * under every variant, which catches a screen that crashes at a large font or in dark mode.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = ScreenshotDevices.PHONE)
class CurrentScreensScreenshotTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val compose = createComposeRule()

    private val appearance: AppearanceMode
        get() = if (variant.isDark) AppearanceMode.DARK else AppearanceMode.LIGHT

    @Test
    fun settingsBeforeSetup() = capture("settings-setup") {
        SettingsPreview(appearance, KeyboardSetupStatus.NOT_ENABLED)
    }

    @Test
    fun settingsReady() = capture("settings-ready") {
        SettingsPreview(appearance, KeyboardSetupStatus.READY)
    }

    @Test
    fun appearance() = capture("appearance") {
        FunputTheme(appearanceMode = appearance) {
            AppearanceScreen(appearancePreviewState(followsAppearance = false))
        }
    }

    @Test
    fun themeStudio() = capture("theme-studio") {
        FunputTheme(appearanceMode = appearance) {
            CreateCustomThemeScreen(
                baseThemes = BuiltInKeyboardThemeSource.loadThemes(),
                onSave = {},
                onBack = {},
            )
        }
    }

    @Test
    fun shortcuts() = capture("shortcuts") {
        FunputTheme(appearanceMode = appearance) { ShortcutsRoute(onBack = {}) }
    }

    @Test
    fun about() = capture("about") {
        FunputTheme(appearanceMode = appearance) {
            AboutScreen(versionName = "1.2026.70", onOpenLink = {})
        }
    }

    private fun capture(screen: String, content: @Composable () -> Unit) =
        compose.captureScreen("current/$screen", variant, content = content)

    companion object {
        /** Every appearance × font-scale combination, one test instance each. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants(): List<Array<Any>> = ScreenshotVariant.parameters()
    }
}
