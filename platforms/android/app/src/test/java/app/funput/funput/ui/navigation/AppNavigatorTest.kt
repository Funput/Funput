package app.funput.funput.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigatorTest {
    @Test
    fun navigatesWithoutDuplicatingCurrentDestination() {
        val navigator = AppNavigator()

        navigator.navigate(AppDestination.CREATE_CUSTOM_THEME)
        navigator.navigate(AppDestination.CREATE_CUSTOM_THEME)

        assertEquals(AppDestination.CREATE_CUSTOM_THEME, navigator.currentDestination)
        assertTrue(navigator.navigateBack())
        assertEquals(AppDestination.THEME_GALLERY, navigator.currentDestination)
    }

    @Test
    fun `navigating into another tab switches to it`() {
        val navigator = AppNavigator()

        navigator.navigate(AppDestination.THEME_GALLERY)

        assertEquals(TopLevelDestination.APPEARANCE, navigator.currentTab)
        assertEquals(AppDestination.THEME_GALLERY, navigator.currentDestination)
    }

    @Test
    fun `a tab keeps where it was left`() {
        val navigator = AppNavigator()
        navigator.navigate(AppDestination.CREATE_CUSTOM_THEME)

        navigator.selectTab(TopLevelDestination.SETTINGS)
        navigator.selectTab(TopLevelDestination.APPEARANCE)

        // Half way into the appearance tab, away and back: still half way in, not at its root.
        assertEquals(AppDestination.CREATE_CUSTOM_THEME, navigator.currentDestination)
    }

    @Test
    fun `back empties the tab before it leaves the tab`() {
        val navigator = AppNavigator()
        navigator.navigate(AppDestination.CREATE_CUSTOM_THEME)

        assertTrue(navigator.navigateBack())
        assertEquals(AppDestination.THEME_GALLERY, navigator.currentDestination)
        assertTrue(navigator.navigateBack())
        assertEquals(TopLevelDestination.SETTINGS, navigator.currentTab)
        // At the start tab's root there is nothing left to pop; the system takes over from here.
        assertFalse(navigator.canNavigateBack)
        assertFalse(navigator.navigateBack())
    }

    @Test
    fun `previous destination is what back would reveal`() {
        val navigator = AppNavigator()
        assertEquals(null, navigator.previousDestination)

        navigator.navigate(AppDestination.THEME_GALLERY)
        // Back leaves the tab from here, so what it reveals is the start tab's own screen.
        assertEquals(AppDestination.SETTINGS, navigator.previousDestination)

        navigator.navigate(AppDestination.CREATE_CUSTOM_THEME)
        assertEquals(AppDestination.THEME_GALLERY, navigator.previousDestination)
    }

    @Test
    fun `every tab has exactly one root`() {
        // rootOf picks the depth-zero screen of a tab and would throw on a tab that has none.
        TopLevelDestination.entries.forEach { tab ->
            val roots = AppDestination.entries.filter { it.tab == tab && it.depth == 0 }
            assertEquals(listOf(AppDestination.rootOf(tab)), roots)
        }
    }
}
