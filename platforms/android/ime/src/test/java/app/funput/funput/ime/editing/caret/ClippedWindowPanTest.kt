package app.funput.funput.ime.editing.caret

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Panning where the readable window does not reach the start of the document.
 *
 * The window's length is then the size of the request rather than the caret's position, so the
 * only absolute reference left is what the host reported through `onUpdateSelection`.
 */
class ClippedWindowPanTest {
    @Test
    fun aClippedWindowRefusesToMoveWhenTheHostHasReportedNothing() {
        // More text behind the caret than the resolver's 1024-character lookback can see.
        val document = PanDocument("x".repeat(3000))
        document.moveTo(2500)

        assertFalse(CaretPanResolver().apply(document.proxy, columns = -3))
        // Reading the window's length as the caret's position used to land it at 1021.
        assertEquals(2500, document.cursor)
    }

    @Test
    fun aClippedWindowMovesFromTheCaretTheHostReported() {
        val document = PanDocument("x".repeat(3000))
        document.moveTo(2500)
        val resolver = CaretPanResolver().apply { onSelectionChanged(2500) }

        assertEquals(true, resolver.apply(document.proxy, columns = -3))
        assertEquals(2497, document.cursor)
    }

    @Test
    fun aClippedWindowKeepsPanningFromItsOwnEarlierStep() {
        val document = PanDocument("x".repeat(3000))
        document.moveTo(2500)
        val resolver = CaretPanResolver().apply { onSelectionChanged(2500) }

        resolver.apply(document.proxy, columns = -3)
        resolver.apply(document.proxy, columns = -3)

        // The resolver's own setSelection keeps the reference current without a fresh report.
        assertEquals(2494, document.cursor)
    }

    @Test
    fun aReportFromBehindTheReadableWindowIsRefusedAsStale() {
        val document = PanDocument("x".repeat(3000))
        document.moveTo(2500)
        // The caret cannot be at 10 when 1024 characters are visible behind it.
        val resolver = CaretPanResolver().apply { onSelectionChanged(10) }

        assertFalse(resolver.apply(document.proxy, columns = -3))
        assertEquals(2500, document.cursor)
    }
}
