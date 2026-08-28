package app.funput.funput.ime.editing.caret

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The line arithmetic behind vertical caret panning, exercised without a document.
 *
 * Offsets are stated relative to the caret, which is what the resolver adds to the caret position
 * before calling `setSelection`.
 */
class CaretLineGeometryTest {
    @Test
    fun movingUpKeepsTheColumnWhenTheLineAboveIsLongEnough() {
        val geometry = geometry(before = "abcdefgh\nij\nklmno", after = "p")

        // Column 5 does not exist on "ij", so the caret settles on its end.
        assertEquals(
            CaretLineGeometry.Resolution(offset = -6, column = 5),
            geometry.resolve(columns = 0, lines = -1, desiredColumn = null),
        )
    }

    @Test
    fun aRememberedColumnSurvivesAPassThroughAShortLine() {
        val geometry = geometry(before = "abcdefgh\nij", after = "\nklmnop")

        assertEquals(
            CaretLineGeometry.Resolution(offset = -6, column = 5),
            geometry.resolve(columns = 0, lines = -1, desiredColumn = 5),
        )
    }

    @Test
    fun aDiagonalDragAppliesItsHorizontalComponentToo() {
        val geometry = geometry(before = "abcd\nefg", after = "hi\njklmnop")

        assertEquals(
            CaretLineGeometry.Resolution(offset = 8, column = 5),
            geometry.resolve(columns = 2, lines = 1, desiredColumn = null),
        )
    }

    @Test
    fun upOnTheFirstLineStaysPutRatherThanSlidingToItsStart() {
        val geometry = geometry(before = "abc", after = "def\nghi")

        assertEquals(
            CaretLineGeometry.Resolution(offset = 0, column = null),
            geometry.resolve(columns = 0, lines = -1, desiredColumn = null),
        )
    }

    @Test
    fun downOnTheLastLineStaysPutRatherThanSlidingToItsEnd() {
        val geometry = geometry(before = "abc\ndef", after = "gh")

        assertEquals(
            CaretLineGeometry.Resolution(offset = 0, column = null),
            geometry.resolve(columns = 0, lines = 1, desiredColumn = null),
        )
    }

    @Test
    fun aSingleLineFieldHasNowhereToGoAndDoesNotMove() {
        val geometry = geometry(before = "hello", after = " world")

        assertEquals(0, geometry.resolve(columns = 0, lines = -1, desiredColumn = null).offset)
        assertEquals(0, geometry.resolve(columns = 0, lines = 1, desiredColumn = null).offset)
    }

    @Test
    fun aBlockedVerticalStepStillCarriesItsHorizontalHalf() {
        val geometry = geometry(before = "abc", after = "def\nghi")

        assertEquals(
            CaretLineGeometry.Resolution(offset = 2, column = null),
            geometry.resolve(columns = 2, lines = -1, desiredColumn = null),
        )
    }

    @Test
    fun anEmptyDocumentMovesNowhere() {
        val geometry = geometry(before = "", after = "")

        assertEquals(0, geometry.resolve(columns = 0, lines = -1, desiredColumn = null).offset)
        assertEquals(0, geometry.resolve(columns = 0, lines = 1, desiredColumn = null).offset)
    }

    @Test
    fun moreLinesThanTheDocumentHasClampsToTheOutermostOne() {
        val geometry = geometry(before = "a\nbc", after = "d")

        assertEquals(
            CaretLineGeometry.Resolution(offset = -3, column = 2),
            geometry.resolve(columns = 0, lines = -5, desiredColumn = null),
        )
    }

    @Test
    fun aHorizontalOnlyStepIsTheOffsetItWasAskedForWithNoColumn() {
        val geometry = geometry(before = "abc\ndef", after = "gh")

        assertEquals(
            CaretLineGeometry.Resolution(offset = -3, column = null),
            geometry.resolve(columns = -3, lines = 0, desiredColumn = 7),
        )
    }

    private fun geometry(before: String, after: String) =
        CaretLineGeometry(KeyboardCaretContext(before, after, caretPosition = before.length))
}
