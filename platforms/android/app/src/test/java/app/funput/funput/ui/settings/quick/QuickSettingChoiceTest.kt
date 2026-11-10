package app.funput.funput.ui.settings.quick

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.R
import app.funput.funput.keyboard.KeyboardHapticStrength
import app.funput.funput.keyboard.sound.KeyboardSoundStyle
import app.funput.funput.ui.kit.layout.FunputScreenListTag
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.SettingsPicker
import app.funput.funput.ui.settings.SettingsScreenState
import app.funput.funput.ui.settings.hub.KeyboardHubScreen
import app.funput.funput.ui.settings.testSettingsState
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The vibration and sound tiles: each names its level and opens its sheet, as on iOS. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class QuickSettingChoiceTest {
    @get:Rule
    val compose = createComposeRule()

    private fun openTile(state: SettingsScreenState, setting: QuickSetting) {
        compose.setContent {
            FunputUiTheme(isDark = false) { KeyboardHubScreen(state, onOpenPage = {}) }
        }
        val tile = hasTestTag(quickSettingTag(setting))
        compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(tile)
        compose.onNode(tile).performClick()
    }

    @Test
    fun `only vibration and sound open a sheet`() {
        assertEquals(
            mapOf(
                QuickSetting.HAPTICS to SettingsPicker.HAPTIC_STRENGTH,
                QuickSetting.SOUNDS to SettingsPicker.KEY_SOUND,
            ),
            QuickSetting.entries.mapNotNull { setting -> setting.choice?.let { setting to it } }.toMap(),
        )
        assertEquals(
            mapOf(
                QuickSetting.HAPTICS to R.string.quick_haptics_choose,
                QuickSetting.SOUNDS to R.string.quick_sounds_choose,
            ),
            QuickSetting.entries.mapNotNull { setting -> setting.choiceLabel?.let { setting to it } }.toMap(),
        )
    }

    @Test
    fun `the vibration tile names the strength and opens the strength sheet`() {
        val strong = testSettingsState(hapticStrength = KeyboardHapticStrength.STRONG)
        val off = testSettingsState(hapticsEnabled = false)
        assertEquals(R.string.haptic_strength_strong, QuickSetting.HAPTICS.stateLabel(strong))
        assertEquals(R.string.haptic_strength_off, QuickSetting.HAPTICS.stateLabel(off))

        openTile(strong, QuickSetting.HAPTICS)

        compose.onNodeWithText("Rõ ràng ở mỗi lần chạm").assertExists()
    }

    @Test
    fun `the sound tile names the style and opens the sound sheet`() {
        val wood = testSettingsState(soundsEnabled = true, soundStyle = KeyboardSoundStyle.WOOD)
        val off = testSettingsState(soundsEnabled = false, soundStyle = KeyboardSoundStyle.WOOD)
        assertEquals(R.string.key_sound_wood, QuickSetting.SOUNDS.stateLabel(wood))
        assertEquals(R.string.key_sound_off, QuickSetting.SOUNDS.stateLabel(off))

        openTile(wood, QuickSetting.SOUNDS)

        compose.onNodeWithText("Trầm và ấm như gõ lên gỗ").assertExists()
    }

    @Test
    fun `switching sound back on keeps the remembered style`() {
        val chosen = mutableListOf<KeyboardSoundStyle?>()
        fun state(enabled: Boolean) = testSettingsState(
            soundsEnabled = enabled,
            soundStyle = KeyboardSoundStyle.MARIMBA,
            onSoundStyleSelected = { chosen += it },
        )

        QuickSetting.SOUNDS.toggle(state(enabled = false))
        QuickSetting.SOUNDS.toggle(state(enabled = true))

        assertEquals(listOf(KeyboardSoundStyle.MARIMBA, null), chosen)
    }
}
