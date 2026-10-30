package app.funput.funput.ui.settings.enterkey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val Messenger = "com.facebook.orca"

/** What the Enter key page shows for a given store and set of installed apps. */
class EnterKeyScreenStateTest {
    private val installed = listOf(
        InstalledApp("org.telegram.messenger", "Telegram", null),
        InstalledApp(Messenger, "Messenger", null),
        InstalledApp("com.zing.zalo", "Zalo", null),
        InstalledApp("com.example.dao", "Đào", null),
    )

    @Test fun `nothing shows as empty until the store has been read`() {
        val state = state(packages = null)

        assertFalse(state.isLoaded)
        assertTrue(state.chosen.isEmpty())
    }

    @Test fun `messenger is suggested only while it is installed and not chosen`() {
        assertEquals(listOf(Messenger), state(emptySet()).suggestions.map { it.packageName })
        assertTrue(state(setOf(Messenger)).suggestions.isEmpty())
        assertTrue(state(emptySet(), installed = installed.filterNot { it.packageName == Messenger }).suggestions.isEmpty())
    }

    @Test fun `chosen apps sort by vietnamese name with uninstalled ones last`() {
        val state = state(setOf("com.zing.zalo", "com.gone.app", "com.example.dao", Messenger))

        assertEquals(listOf("Đào", "Messenger", "Zalo", "com.gone.app"), state.chosen.map { it.label })
        assertEquals(listOf(false, false, false, true), state.chosen.map { it.isMissing })
    }

    @Test fun `while apps load nothing is reported missing`() {
        val state = state(setOf(Messenger), installed = null)

        assertFalse(state.chosen.single().isMissing)
        assertNull(state.available)
    }

    @Test fun `the picker leaves out apps already chosen`() {
        val available = state(setOf(Messenger)).available.orEmpty().map { it.packageName }

        assertEquals(listOf("org.telegram.messenger", "com.zing.zalo", "com.example.dao"), available)
    }

    @Test fun `the picker search ignores accents and matches package names`() {
        assertEquals(listOf("Đào"), filterApps(installed, "dao").map { it.label })
        assertEquals(listOf("Zalo"), filterApps(installed, "zing").map { it.label })
        assertEquals(installed, filterApps(installed, "  "))
    }

    private fun state(packages: Set<String>?, installed: List<InstalledApp>? = this.installed) =
        EnterKeyScreenState(packages, installed, onAdd = {}, onRemove = {})
}
