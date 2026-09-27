package app.funput.funput.ui.settings

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.placement.KeyboardPlacementMode
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.keyboard.LayoutSection
import app.funput.funput.uitesting.SCREENSHOT_SDK
import app.funput.funput.uitesting.ScreenshotDevices
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The layout group's rules: which controls appear for which mode and method, and what they report.
 * Runs on a phone-sized screen, because the elevation limit is computed from the screen height.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class LayoutSectionTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(
        inputMethod: KeyboardInputMethod = KeyboardInputMethod.TELEX,
        placement: KeyboardPlacementPreferences = KeyboardPlacementPreferences.Default,
        onShowsNumberRowChanged: (Boolean) -> Unit = {},
        onKeySizeSelected: (KeyboardSizingProfile) -> Unit = {},
    ) = compose.setContent {
        FunputUiTheme(isDark = false) {
            LayoutSection(
                inputMethod = inputMethod,
                showsNumberRow = false,
                keySizeProfile = KeyboardSizingProfile.scaled(1f),
                placement = placement,
                onShowsNumberRowChanged = onShowsNumberRowChanged,
                onKeySizeSelected = onKeySizeSelected,
                onOpenPlacement = {},
                onElevationSelected = {},
                onOneHandedWidthSelected = {},
                onOneHandedSideSelected = {},
            )
        }
    }

    @Test
    fun `telex lets the number row be switched`() = assertNumberRowToggles(KeyboardInputMethod.TELEX)

    @Test
    fun `advanced telex lets the number row be switched`() =
        assertNumberRowToggles(KeyboardInputMethod.TELEX_ADVANCED)

    private fun assertNumberRowToggles(method: KeyboardInputMethod) {
        var shows = false
        show(inputMethod = method, onShowsNumberRowChanged = { shows = it })

        compose.onNodeWithText("Hàng phím số").assertIsOff().performClick()

        assertEquals(true, shows)
    }

    @Test
    fun `vni does not offer the number row, which it always needs`() {
        show(inputMethod = KeyboardInputMethod.VNI)

        compose.onNodeWithText("Hàng phím số").assertDoesNotExist()
    }

    @Test
    fun `key size shows its value and bounds and settles on a whole percent`() {
        var settled: KeyboardSizingProfile? = null
        show(onKeySizeSelected = { settled = it })

        listOf("100%", "85%", "150%").forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithContentDescription("Kích thước phím")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1.153f) }

        assertEquals(1.15f, settled!!.heightScale, 0.0001f)
    }

    @Test
    fun `elevated mode shows its live offset control`() {
        show(placement = KeyboardPlacementPreferences(KeyboardPlacementMode.ELEVATED, 96f))

        compose.onNodeWithText("96 dp").assertExists()
        compose.onNodeWithContentDescription("Mức nâng").assertExists()
    }
}
