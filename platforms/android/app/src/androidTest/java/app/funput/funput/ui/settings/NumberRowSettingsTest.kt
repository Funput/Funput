package app.funput.funput.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.settings.keyboard.KeyboardSettingsSection
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun vniLocksNumberRowOnWithoutWritingPreference() {
        var showsNumberRow = false
        var writes = 0
        setSection(
            inputMethod = KeyboardInputMethod.VNI,
            showsNumberRow = showsNumberRow,
            onShowsNumberRowChanged = {
                showsNumberRow = it
                writes += 1
            },
        )

        compose.onNodeWithText("Hàng phím số").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Hàng phím số").performClick()
        compose.runOnIdle {
            assertFalse(showsNumberRow)
            assertEquals(0, writes)
        }
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
                KeyboardSettingsSection(
                    setupStatus = KeyboardSetupStatus.READY,
                    inputMethod = inputMethod,
                    showsNumberRow = showsNumberRow,
                    toneStyle = ToneStyle.TRADITIONAL,
                    keySizeProfile = KeyboardSizingProfile.Normal,
                    keyboardThemeLabel = "Ink",
                    onOpenPicker = {},
                    onShowsNumberRowChanged = onShowsNumberRowChanged,
                    onOpenThemeGallery = {},
                    onEnableKeyboard = {},
                    onSelectKeyboard = {},
                )
            }
        }
    }
}
