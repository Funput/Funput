package app.funput.funput.ui.settings.extraonsets

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetter
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetters
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The section's user interaction and TalkBack semantics, independent of storage. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class ExtraOnsetsSectionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `master reveals every letter and removing the last turns it off`() {
        show()
        compose.onNodeWithTag(ExtraOnsetsMasterTag).assertIsOff().performClick()
        ExtraOnsetLetter.Ordered.forEach { letter ->
            compose.onNodeWithTag(extraOnsetLetterTag(letter)).assertIsOn()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
                .performClick()
        }
        compose.onNodeWithTag(ExtraOnsetsMasterTag).assertIsOff()
        ExtraOnsetLetter.Ordered.forEach { compose.onNodeWithTag(extraOnsetLetterTag(it)).assertDoesNotExist() }
        compose.onNodeWithTag(ExtraOnsetsMasterTag).performClick()
        ExtraOnsetLetter.Ordered.forEach { compose.onNodeWithTag(extraOnsetLetterTag(it)).assertIsOn() }
    }

    @Test fun `individual switches preserve their peers and the master clears all`() {
        show(ExtraOnsetLetters.All)
        compose.onNodeWithTag(extraOnsetLetterTag(ExtraOnsetLetter.F)).performClick().assertIsOff()
        compose.onNodeWithTag(extraOnsetLetterTag(ExtraOnsetLetter.Z)).assertIsOn()
        compose.onNodeWithTag(ExtraOnsetsMasterTag).assertIsOn().performClick().assertIsOff()
        compose.onNodeWithTag(extraOnsetLetterTag(ExtraOnsetLetter.Z)).assertDoesNotExist()
    }

    @Test fun `advanced hint only appears while w is selected`() {
        show(ExtraOnsetLetters.All, KeyboardInputMethod.TELEX_ADVANCED)
        compose.onNodeWithText("Telex nâng cao", substring = true).assertExists()
        compose.onNodeWithTag(extraOnsetLetterTag(ExtraOnsetLetter.W)).performClick()
        compose.onNodeWithText("Telex nâng cao", substring = true).assertDoesNotExist()
        compose.onNodeWithText("fast → fát", substring = true).assertExists()
    }

    @Test fun `regular Telex does not show the advanced w hint`() {
        show(ExtraOnsetLetters.All)
        compose.onNodeWithText("Telex nâng cao", substring = true).assertDoesNotExist()
    }

    @Test fun `controls are disabled during a save and errors are announced`() {
        show(ExtraOnsetLetters.All, isSaving = true, hasSaveError = true)
        compose.onNodeWithTag(ExtraOnsetsMasterTag).assertIsNotEnabled()
        ExtraOnsetLetter.Ordered.forEach { compose.onNodeWithTag(extraOnsetLetterTag(it)).assertIsNotEnabled() }
        compose.onNodeWithText("Không thể lưu lựa chọn.", substring = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    private fun show(
        initial: ExtraOnsetLetters = ExtraOnsetLetters.None,
        method: KeyboardInputMethod = KeyboardInputMethod.TELEX,
        isSaving: Boolean = false,
        hasSaveError: Boolean = false,
    ) {
        val selection = mutableStateOf(initial)
        compose.setContent {
            FunputUiTheme(isDark = false) {
                ExtraOnsetsSection(ExtraOnsetsSectionState(
                    selection = selection.value,
                    inputMethod = method,
                    isSaving = isSaving,
                    hasSaveError = hasSaveError,
                    onEnabledChanged = { selection.value = if (it) ExtraOnsetLetters.All else ExtraOnsetLetters.None },
                    onLetterChanged = { letter, enabled -> selection.value = selection.value.withLetter(letter, enabled) },
                ))
            }
        }
    }
}
