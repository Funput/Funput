package app.funput.funput.ui.settings.quick

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.R
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.kit.layout.FunputScreenListTag
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.SettingsScreenState
import app.funput.funput.ui.settings.hub.KeyboardHubScreen
import app.funput.funput.ui.settings.testSettingsState
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The quick settings: six tiles that each flip their own setting, and the input method control. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class QuickSettingsTest {
    @get:Rule
    val compose = createComposeRule()

    private val writes = mutableMapOf<String, Boolean>()

    private fun state(method: KeyboardInputMethod = KeyboardInputMethod.TELEX, showsNumberRow: Boolean = false) =
        testSettingsState(
            inputMethod = method,
            showsNumberRow = showsNumberRow,
            onShowsNumberRowChanged = { writes["numberRow"] = it },
            onSmartChanged = { key, value -> writes[key] = value },
            onFeedbackChanged = { key, value -> writes[key] = value },
        )

    private fun show(state: SettingsScreenState) = compose.setContent {
        FunputUiTheme(isDark = false) { KeyboardHubScreen(state, onOpenPage = {}) }
    }

    @Test
    fun `there are six tiles, each its own setting`() {
        assertEquals(6, QuickSetting.entries.size)
        assertEquals(6, QuickSetting.entries.map { it.title }.distinct().size)
        assertEquals(6, QuickSetting.entries.map { it.accessibilityTitle }.distinct().size)
    }

    @Test
    fun `each tile writes the opposite of its own setting, and only that`() {
        val state = state()
        QuickSetting.entries.forEach { setting -> setting.toggle(state) }

        assertEquals(
            mapOf(
                "haptics" to false, "sounds" to true, "capitalization" to false,
                "suggestions" to false, "numberRow" to true, "gestures" to false,
            ),
            writes,
        )
    }

    @Test
    fun `vni keeps the number row on and locked`() {
        val vni = state(KeyboardInputMethod.VNI, showsNumberRow = false)

        assertTrue(QuickSetting.NUMBER_ROW.isLocked(vni))
        assertTrue(QuickSetting.NUMBER_ROW.isOn(vni))
        assertEquals(R.string.quick_locked, QuickSetting.NUMBER_ROW.stateLabel(vni))
        QuickSetting.NUMBER_ROW.toggle(vni)
        assertTrue(writes.isEmpty())
    }

    @Test
    fun `telex unlocks the number row and shows its own value`() {
        val telex = state(KeyboardInputMethod.TELEX, showsNumberRow = false)

        assertFalse(QuickSetting.NUMBER_ROW.isLocked(telex))
        assertEquals(R.string.quick_off, QuickSetting.NUMBER_ROW.stateLabel(telex))
    }

    @Test
    fun `tiles are switches on the hub, and a locked one cannot be used`() {
        show(state(KeyboardInputMethod.VNI))
        val list = compose.onNodeWithTag(FunputScreenListTag)

        list.performScrollToNode(hasTestTag(quickSettingTag(QuickSetting.SOUNDS)))
        compose.onNodeWithTag(quickSettingTag(QuickSetting.SOUNDS)).assertIsOff().performClick()
        compose.onNodeWithTag(quickSettingTag(QuickSetting.HAPTICS)).assertIsOn()
        list.performScrollToNode(hasTestTag(quickSettingTag(QuickSetting.NUMBER_ROW)))
        compose.onNodeWithTag(quickSettingTag(QuickSetting.NUMBER_ROW)).assertIsOn().assertIsNotEnabled()

        compose.runOnIdle { assertEquals(mapOf("sounds" to true), writes) }
    }

    @Test
    fun `the input method control selects the method`() {
        var selected: KeyboardInputMethod? = null
        show(testSettingsState(onInputMethodSelected = { selected = it }))

        compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(hasText("VNI"))
        compose.onNodeWithTag(QuickInputMethodTag).assertExists()
        compose.onNode(hasText("VNI")).performClick()

        compose.runOnIdle { assertEquals(KeyboardInputMethod.VNI, selected) }
    }
}
