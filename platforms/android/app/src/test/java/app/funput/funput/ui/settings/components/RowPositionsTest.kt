package app.funput.funput.ui.settings.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The corner of a settings row is decided by where it sits, and rows come and go with the device
 * and the settings state — the dynamic-colour row only exists on Android 12+. These cases are the
 * ones that would show up as a group with two rounded ends in the middle of it.
 */
class RowPositionsTest {

    @Test
    fun `single row is rounded on both ends`() {
        assertEquals(listOf(RowPosition.ONLY), rowPositions(1))
    }

    @Test
    fun `two rows are the ends of the group with nothing between`() {
        assertEquals(listOf(RowPosition.FIRST, RowPosition.LAST), rowPositions(2))
    }

    @Test
    fun `middle rows stay square`() {
        assertEquals(
            listOf(RowPosition.FIRST, RowPosition.MIDDLE, RowPosition.MIDDLE, RowPosition.LAST),
            rowPositions(4),
        )
    }

    @Test
    fun `an empty group asks for no positions`() {
        assertEquals(emptyList<RowPosition>(), rowPositions(0))
    }
}
