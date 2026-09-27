package app.funput.funput.ui.shortcuts

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.createComposeRule
import app.funput.funput.shortcuts.model.ShortcutLibrary
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

/** The shortcuts screen with a short list, and with nothing yet, in every appearance and font scale. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class ShortcutsScreenshotTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun list() = capture(
        "shortcuts-list",
        ShortcutLibrary(
            entries = listOf(
                testShortcut("vn", "Việt Nam"),
                testShortcut("hn", "Hà Nội"),
                testShortcut("ks", "Kính gửi anh chị,\nEm xin gửi lại tài liệu cuộc họp sáng nay."),
                testShortcut("@", "hello@funput.app"),
            ),
        ),
    )

    @Test
    fun empty() = capture("shortcuts-empty", ShortcutLibrary())

    private fun capture(screen: String, library: ShortcutLibrary) {
        val store = ShortcutTestStore(library)
        var model: ShortcutsScreenModel? = null
        compose.captureScreen(
            screen,
            variant,
            root = APP_SCREENSHOT_ROOT,
            prepare = { waitUntil { model?.hasLoaded == true && model?.isLoading == false } },
        ) {
            val scope = rememberCoroutineScope()
            val screenModel = remember { ShortcutsScreenModel(store, scope).also { model = it } }
            LaunchedEffect(screenModel) { screenModel.reload() }
            FunputUiTheme(isDark = variant.isDark) {
                CompositionLocalProvider(LocalGlassTier provides GlassTier.SOLID) {
                    ShortcutsScreen(screenModel, onBack = {})
                }
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
