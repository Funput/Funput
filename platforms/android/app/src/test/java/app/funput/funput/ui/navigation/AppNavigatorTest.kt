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
}
