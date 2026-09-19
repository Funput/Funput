package app.funput.funput.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.settings.keyboard.LayoutSettingsSection
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NumberRowSettingsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun telexAllowsTogglingNumberRow() {
        var showsNumberRow = false
        setSection(
            inputMethod = KeyboardInputMethod.TELEX,
            showsNumberRow = showsNumberRow,
            onShowsNumberRowChanged = { showsNumberRow = it },
        )

        compose.onNodeWithText("Hàng phím số").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(showsNumberRow) }
    }

    @Test
    fun vniDoesNotOfferTheNumberRowAtAll() {
        setSection(
            inputMethod = KeyboardInputMethod.VNI,
            showsNumberRow = false,
            onShowsNumberRowChanged = {},
        )

        // VNI types tones with the digits, so the row can only ever be on. It used to be shown
        // disabled, which is a line of text explaining that there is nothing to decide.
        compose.onNodeWithText("Hàng phím số").assertDoesNotExist()
    }

    @Test
    fun advancedTelexKeepsNumberRowEditable() {
        var showsNumberRow = false
        setSection(
            inputMethod = KeyboardInputMethod.TELEX_ADVANCED,
            showsNumberRow = showsNumberRow,
            onShowsNumberRowChanged = { showsNumberRow = it },
        )

        compose.onNodeWithText("Hàng phím số").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(showsNumberRow) }
    }

    private fun setSection(
        inputMethod: KeyboardInputMethod,
        showsNumberRow: Boolean,
        onShowsNumberRowChanged: (Boolean) -> Unit,
    ) {
        compose.setContent {
            FunputTheme {
                LayoutSettingsSection(
                    inputMethod = inputMethod,
                    showsNumberRow = showsNumberRow,
                    keySizeProfile = KeyboardSizingProfile.Normal,
                    onShowsNumberRowChanged = onShowsNumberRowChanged,
                    onKeySizeSelected = {},
                )
            }
        }
    }
}
