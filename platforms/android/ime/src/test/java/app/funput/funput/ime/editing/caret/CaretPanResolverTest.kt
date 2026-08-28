package app.funput.funput.ime.editing.caret

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The resolver against a document that hands back no `ExtractedText` but whose readable window
 * reaches the start of the document.
 *
 * A window that does not is [ClippedWindowPanTest]; the `ExtractedText` path needs a real editor
 * and lives in `CaretPanInstrumentedTest`; the line arithmetic all three share is covered by
 * [CaretLineGeometryTest].
 */
class CaretPanResolverTest {
    @Test
    fun aHorizontalStepMovesWholeCharacters() {
        val document = PanDocument("xin chao")

        assertEquals(true, CaretPanResolver().apply(document.proxy, columns = -3, lines = 0))
        assertEquals(5, document.cursor)
    }

    @Test
    fun aVerticalStepLandsOnTheSameColumnOfTheLineAbove() {
        val document = PanDocument("abcdefgh\nij\nklmnop")

        CaretPanResolver().apply(document.proxy, columns = 0, lines = -1)

        // Column 6 does not fit on "ij", so the caret stops at its end.
        assertEquals(11, document.cursor)
    }

    @Test
    fun aSecondVerticalStepReturnsToTheColumnTheFirstOneWanted() {
        val document = PanDocument("abcdefgh\nij\nklmnop")
        val resolver = CaretPanResolver()

        resolver.apply(document.proxy, columns = 0, lines = -1)
        resolver.apply(document.proxy, columns = 0, lines = -1)

        assertEquals(6, document.cursor)
    }

    @Test
    fun movingTheCaretInBetweenForgetsTheRememberedColumn() {
        val document = PanDocument("abcdefgh\nij\nklmnop")
        val resolver = CaretPanResolver()

        resolver.apply(document.proxy, columns = 0, lines = -1)
        document.moveTo(12)
        resolver.apply(document.proxy, columns = 0, lines = -1)

        // Column 0 of "ij", not column 6 of the line the earlier pan started on.
        assertEquals(9, document.cursor)
    }

    @Test
    fun upWithNoLineAboveLeavesTheCaretWhereItIs() {
        val document = PanDocument("abcdef")

        assertFalse(CaretPanResolver().apply(document.proxy, columns = 0, lines = -1))
        assertEquals(6, document.cursor)
    }

    @Test
    fun aStepWithNowhereToGoWritesNothing() {
        val document = PanDocument("abcdef")
        document.moveTo(0)

        assertFalse(CaretPanResolver().apply(document.proxy, columns = 0, lines = -1))
        assertEquals(0, document.cursor)
    }

    @Test
    fun aWindowThatReachesTheStartOfTheDocumentStillMoves() {
        val document = PanDocument("x".repeat(1000))

        assertEquals(true, CaretPanResolver().apply(document.proxy, columns = -3, lines = 0))
        assertEquals(997, document.cursor)
    }

    @Test
    fun aZeroStepWritesNothing() {
        val document = PanDocument("xin chao")

        assertFalse(CaretPanResolver().apply(document.proxy, columns = 0, lines = 0))
        assertEquals(8, document.cursor)
    }
}
