package app.funput.funput.ui.settings

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.settings.keyboard.KeyboardSettingsSection
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class KeySizeSliderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sliderShowsTheCurrentSizeAndItsBounds() {
        setSection(KeyboardSizingProfile.scaled(1f)) {}

        compose.onNodeWithText("100%").assertIsDisplayed()
        compose.onNodeWithText("85%").assertIsDisplayed()
        compose.onNodeWithText("120%").assertIsDisplayed()
    }

    @Test
    fun settlingTheSliderReportsAWholePercent() {
        var settled: KeyboardSizingProfile? = null
        setSection(KeyboardSizingProfile.scaled(1f)) { settled = it }

        compose.onNodeWithContentDescription("Kích thước phím")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1.153f) }

        compose.runOnIdle { assertEquals(1.15f, settled!!.heightScale, 0.0001f) }
    }

    private fun setSection(
        profile: KeyboardSizingProfile,
        onKeySizeSelected: (KeyboardSizingProfile) -> Unit,
    ) {
        compose.setContent {
            FunputTheme {
                KeyboardSettingsSection(
                    setupStatus = KeyboardSetupStatus.READY,
                    inputMethod = KeyboardInputMethod.TELEX,
                    showsNumberRow = true,
                    toneStyle = ToneStyle.TRADITIONAL,
                    keySizeProfile = profile,
                    onOpenPicker = {},
                    onShowsNumberRowChanged = {},
                    onToneStyleSelected = {},
                    onKeySizeSelected = onKeySizeSelected,
                    onEnableKeyboard = {},
                    onSelectKeyboard = {},
                )
            }
        }
    }
}
