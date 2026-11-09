package app.funput.funput.ui.settings.feedback.sound

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.funput.funput.keyboard.sound.KeyboardSoundStyle
import app.funput.funput.keyboard.sound.KeyboardSoundVolume
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The sound sheet marks the current style, stays open while the user tries each one, and sets the volume. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class KeySoundSheetTest {
    @get:Rule
    val compose = createComposeRule()

    private val styles = mutableListOf<KeyboardSoundStyle?>()
    private val volumes = mutableListOf<KeyboardSoundVolume>()
    private var dismissed = 0

    private fun show(initial: KeyboardSoundStyle?) = compose.setContent {
        var selected by remember { mutableStateOf(initial) }
        var volume by remember { mutableStateOf(KeyboardSoundVolume.MEDIUM) }
        FunputUiTheme(isDark = false) {
            KeySoundSheet(
                selected = selected,
                volume = volume,
                onSelect = { styles += it; selected = it },
                onVolumeSelect = { volumes += it; volume = it },
                onDismiss = { dismissed += 1 },
            )
        }
    }

    private fun tap(option: KeySoundOption) =
        compose.onNodeWithTag(keySoundTag(option)).performScrollTo().performClick()

    @Test
    fun `choosing styles in turn keeps the sheet open, and off is a choice too`() {
        show(KeyboardSoundStyle.SYSTEM)

        compose.onNodeWithTag(keySoundTag(KeySoundOption.SYSTEM)).assertIsSelected()
        tap(KeySoundOption.WOOD)
        tap(KeySoundOption.MARIMBA)
        tap(KeySoundOption.OFF)

        compose.onNodeWithTag(keySoundTag(KeySoundOption.OFF)).performScrollTo().assertIsSelected()
        assertEquals(listOf(KeyboardSoundStyle.WOOD, KeyboardSoundStyle.MARIMBA, null), styles)
        assertEquals(0, dismissed)
    }

    @Test
    fun `the volume sets the level while sound is on, and stays in place once it is off`() {
        show(KeyboardSoundStyle.BUBBLE)

        compose.onNodeWithText("To").performClick()
        tap(KeySoundOption.OFF)

        compose.onNodeWithTag(KeySoundVolumeTag).assertIsDisplayed()
        compose.onNodeWithText("To").assertIsSelected().assertIsNotEnabled()
        assertEquals(listOf(KeyboardSoundVolume.LOUD), volumes)
    }

    @Test
    fun `the volume cannot be changed while sound is off`() {
        show(null)

        compose.onNodeWithText("Nhỏ").assertIsNotEnabled().performClick()

        assertEquals(emptyList<KeyboardSoundVolume>(), volumes)
    }

    @Test
    fun `every option maps back to itself, and every style has one`() {
        KeySoundOption.entries.forEach { option -> assertEquals(option, KeySoundOption.of(option.style)) }
        assertEquals(KeyboardSoundStyle.entries.toSet(), KeySoundOption.entries.mapNotNull { it.style }.toSet())
    }
}
