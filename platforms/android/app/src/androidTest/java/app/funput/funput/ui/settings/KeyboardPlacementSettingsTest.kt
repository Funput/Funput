package app.funput.funput.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import app.funput.funput.R
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.placement.KeyboardPlacementMode
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import app.funput.funput.ui.settings.keyboard.LayoutSettingsSection
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Rule
import org.junit.Test

class KeyboardPlacementSettingsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun elevatedModeShowsItsLiveOffsetControl() {
        compose.setContent {
            FunputTheme {
                LayoutSettingsSection(
                    inputMethod = KeyboardInputMethod.TELEX,
                    showsNumberRow = true,
                    keySizeProfile = KeyboardSizingProfile.Normal,
                    placement = KeyboardPlacementPreferences(KeyboardPlacementMode.ELEVATED, 96f),
                    onShowsNumberRowChanged = {},
                    onKeySizeSelected = {},
                )
            }
        }

        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        compose.onNodeWithText(resources.getString(R.string.keyboard_mode_elevated)).assertIsDisplayed()
        compose.onNodeWithText("96 dp").assertIsDisplayed()
        compose.onNodeWithContentDescription(resources.getString(R.string.settings_elevation_title))
            .assertIsDisplayed()
    }
}
