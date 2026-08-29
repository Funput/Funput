package app.funput.funput.ime.editing.caret

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Sideways panning against a document that hands back no `ExtractedText` but whose readable
 * window reaches the start of the document.
 *
 * A window that does not is [ClippedWindowPanTest]; the `ExtractedText` path needs a real
 * editor and lives in `CaretPanInstrumentedTest`. Vertical movement is not the resolver's job —
 * see `CaretLineKeysTest`.
 */
class CaretPanResolverTest {
    @Test
    fun aHorizontalStepMovesWholeCharacters() {
        val document = PanDocument("xin chao")

        assertEquals(true, CaretPanResolver().apply(document.proxy, columns = -3))
        assertEquals(5, document.cursor)
    }

    @Test
    fun aWindowThatReachesTheStartOfTheDocumentStillMoves() {
        val document = PanDocument("x".repeat(1000))

        assertEquals(true, CaretPanResolver().apply(document.proxy, columns = -3))
        assertEquals(997, document.cursor)
    }

    @Test
    fun aZeroStepWritesNothing() {
        val document = PanDocument("xin chao")

        assertFalse(CaretPanResolver().apply(document.proxy, columns = 0))
        assertEquals(8, document.cursor)
    }
}
