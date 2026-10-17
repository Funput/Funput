package app.funput.funput.ui.settings.pages

import app.funput.funput.ui.navigation.AppDestination
import app.funput.funput.ui.navigation.TopLevelDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The page list is the one source the hub, its rows and the navigation all follow. */
class SettingsPageTest {
    @Test
    fun `the hub lists every page but data, once each`() {
        val rows = SettingsPage.hubRows(speechAvailable = true)

        assertEquals(rows.distinct(), rows)
        assertEquals(SettingsPage.entries.toSet(), (rows + SettingsPage.DATA).toSet())
        assertFalse(SettingsPage.DATA in rows)
    }

    @Test
    fun `voice input is left out where the build does not ship it`() {
        assertFalse(SettingsPage.SPEECH in SettingsPage.hubRows(speechAvailable = false))
        assertTrue(SettingsPage.SPEECH in SettingsPage.hubRows(speechAvailable = true))
    }

    @Test
    fun `page titles are distinct`() {
        val titles = SettingsPage.entries.map { page -> page.title }

        assertEquals(titles.distinct(), titles)
    }

    @Test
    fun `every page opens one step into its tab, data in me and the rest in the keyboard tab`() {
        SettingsPage.entries.forEach { page ->
            val home = page.destinations.first()
            val tab = if (page == SettingsPage.DATA) TopLevelDestination.ME else TopLevelDestination.KEYBOARD
            assertEquals(tab, home.tab)
            assertEquals(1, home.depth)
            assertEquals(page, SettingsPage.of(home))
        }
    }

    @Test
    fun `a page asked for in a tab it has no screen in opens in its home tab`() {
        assertEquals(AppDestination.TYPING, SettingsPage.TYPING.destinationIn(TopLevelDestination.ME))
        assertEquals(AppDestination.DATA, SettingsPage.DATA.destinationIn(TopLevelDestination.ME))
    }

    @Test
    fun `screens that are not settings pages have no page`() {
        listOf(AppDestination.KEYBOARD, AppDestination.SHORTCUTS, AppDestination.ME).forEach { destination ->
            assertEquals(null, SettingsPage.of(destination))
        }
    }
}
