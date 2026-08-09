package app.funput.funput.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigatorTest {
    @Test
    fun navigatesWithoutDuplicatingCurrentDestination() {
        val navigator = AppNavigator()

        navigator.navigate(AppDestination.THEME_GALLERY)
        navigator.navigate(AppDestination.THEME_GALLERY)

        assertEquals(AppDestination.THEME_GALLERY, navigator.currentDestination)
        assertTrue(navigator.canNavigateBack)
        assertTrue(navigator.navigateBack())
        assertEquals(AppDestination.SETTINGS, navigator.currentDestination)
        assertFalse(navigator.canNavigateBack)
        assertFalse(navigator.navigateBack())
    }

    @Test
    fun `previous destination is what back would reveal`() {
        val navigator = AppNavigator()

        // A predictive back gesture draws this screen while the finger is still down, so it has to
        // be readable before anything is popped.
        assertEquals(null, navigator.previousDestination)
        navigator.navigate(AppDestination.THEME_GALLERY)
        assertEquals(AppDestination.SETTINGS, navigator.previousDestination)
        navigator.navigate(AppDestination.CREATE_CUSTOM_THEME)
        assertEquals(AppDestination.THEME_GALLERY, navigator.previousDestination)
    }

    @Test
    fun `depth increases away from the root`() {
        // Transitions read this to decide which way to slide. Equal or inverted depths would send
        // a screen in from the wrong edge.
        val depths = AppDestination.entries.map(AppDestination::depth)

        assertEquals(depths.sorted(), depths)
        assertEquals(depths.distinct(), depths)
        assertEquals(0, AppDestination.SETTINGS.depth)
    }
}
