package app.funput.funput.ime.editing.caret

import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The resolver against a document that hands back no `ExtractedText`, which is the windowed
 * fallback path. The `ExtractedText` path needs a real editor and lives in
 * `CaretPanInstrumentedTest`; the line arithmetic both share is covered by
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
    fun upOnTheFirstLineGoesToTheStartOfTheLine() {
        val document = PanDocument("abcdef")

        CaretPanResolver().apply(document.proxy, columns = 0, lines = -1)

        assertEquals(0, document.cursor)
    }

    @Test
    fun aStepWithNowhereToGoWritesNothing() {
        val document = PanDocument("abcdef")
        document.moveTo(0)

        assertFalse(CaretPanResolver().apply(document.proxy, columns = 0, lines = -1))
        assertEquals(0, document.cursor)
    }

    @Test
    fun aClippedWindowRefusesToMoveRatherThanGuessTheCaret() {
        // More text behind the caret than the resolver's 1024-character lookback can see.
        val document = PanDocument("x".repeat(3000))
        document.moveTo(2500)

        assertFalse(CaretPanResolver().apply(document.proxy, columns = -3, lines = 0))
        // Reading the window's length as the caret's position used to land it at 1021.
        assertEquals(2500, document.cursor)
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

/** A document that reports no `ExtractedText`, the way a WebView-hosted field does. */
internal class PanDocument(private val text: String) {
    var cursor: Int = text.length
        private set

    fun moveTo(position: Int) {
        cursor = position
    }

    val proxy: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "getExtractedText" -> null
            "getTextBeforeCursor" -> text.substring(
                (cursor - arguments?.first() as Int).coerceAtLeast(0), cursor,
            )
            "getTextAfterCursor" -> text.substring(
                cursor, (cursor + arguments?.first() as Int).coerceAtMost(text.length),
            )
            "setSelection" -> true.also { cursor = arguments?.first() as Int }
            else -> false
        }
    } as InputConnection
}
