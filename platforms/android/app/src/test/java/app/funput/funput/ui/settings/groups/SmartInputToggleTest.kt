package app.funput.funput.ui.settings.groups

import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ui.settings.smart.SmartInputToggle
import app.funput.funput.ui.settings.testSettingsState
import org.junit.Assert.assertEquals
import org.junit.Test

/** The smart switches as data: one title each, and each reads and writes its own setting. */
class SmartInputToggleTest {
    @Test
    fun `titles and icons are distinct`() {
        val titles = SmartInputToggle.entries.map { it.title }

        assertEquals(titles.distinct(), titles)
    }

    @Test
    fun `each switch writes only its own setting`() {
        val writes = mutableListOf<String>()
        val state = testSettingsState(onSmartChanged = { key, _ -> writes += key })

        SmartInputToggle.entries.forEach { toggle -> toggle.set(state, enabled = true) }

        assertEquals(writes.distinct(), writes)
        assertEquals(SmartInputToggle.entries.size, writes.size)
    }

    @Test
    fun `each switch reads its own setting`() {
        val spellCheckOn = SmartCompositionPreferences.Default.copy(spellCheckEnabled = true)
        val before = testSettingsState()
        val after = testSettingsState(smartComposition = spellCheckOn)

        val changed = SmartInputToggle.entries.filter { it.isOn(before) != it.isOn(after) }

        assertEquals(listOf(SmartInputToggle.SPELL_CHECK), changed)
    }
}
