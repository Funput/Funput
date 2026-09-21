package app.funput.funput.keyboard.placement

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardPlacementResolverTest {
    @Test
    fun `standard mode never adds an offset`() {
        val result = resolve(KeyboardPlacementPreferences.Default)
        assertEquals(0, result.appliedOffsetPx)
    }

    @Test
    fun `elevated mode applies the requested offset when it fits`() {
        val preferences = KeyboardPlacementPreferences(KeyboardPlacementMode.ELEVATED, 96f)
        assertEquals(192, resolve(preferences).appliedOffsetPx)
    }

    @Test
    fun `hard maximum caps elevation on a tall viewport`() {
        val preferences = KeyboardPlacementPreferences(KeyboardPlacementMode.ELEVATED, 500f)
        val result = resolve(preferences, viewportHeightPx = 2_000, keyboardHeightPx = 800)
        assertEquals(320, result.appliedOffsetPx)
        assertEquals(320, result.maximumOffsetPx)
    }

    @Test
    fun `offset still leaves thirty percent of the viewport for the app`() {
        val preferences = KeyboardPlacementPreferences(KeyboardPlacementMode.ELEVATED, 500f)
        val result = resolve(preferences, viewportHeightPx = 1_200, keyboardHeightPx = 600)
        assertEquals(240, result.appliedOffsetPx)
    }

    @Test
    fun `absolute minimum wins on a short viewport`() {
        val preferences = KeyboardPlacementPreferences(KeyboardPlacementMode.ELEVATED, 500f)
        val result = resolve(preferences, viewportHeightPx = 1_000, keyboardHeightPx = 600)
        assertEquals(80, result.appliedOffsetPx)
    }

    @Test
    fun `an oversized keyboard safely collapses the elevation`() {
        val preferences = KeyboardPlacementPreferences(KeyboardPlacementMode.ELEVATED, 96f)
        val result = resolve(preferences, viewportHeightPx = 700, keyboardHeightPx = 500)
        assertEquals(0, result.appliedOffsetPx)
        assertEquals(0, result.maximumOffsetPx)
    }

    @Test
    fun `switching modes preserves the requested elevation`() {
        val elevated = KeyboardPlacementPreferences(KeyboardPlacementMode.ELEVATED, 128f)
        val standard = elevated.copy(activeMode = KeyboardPlacementMode.STANDARD)

        assertEquals(128f, standard.elevatedOffsetDp)
        assertEquals(128f, standard.copy(activeMode = KeyboardPlacementMode.ELEVATED).elevatedOffsetDp)
    }

    private fun resolve(
        preferences: KeyboardPlacementPreferences,
        viewportHeightPx: Int = 2_000,
        keyboardHeightPx: Int = 800,
    ) = KeyboardPlacementResolver.resolve(preferences, 2f, viewportHeightPx, keyboardHeightPx)
}
