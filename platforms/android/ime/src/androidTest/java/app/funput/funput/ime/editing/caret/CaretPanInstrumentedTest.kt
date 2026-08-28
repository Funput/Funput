package app.funput.funput.ime.editing.caret

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.ime.editing.support.HostEditor
import app.funput.funput.ime.editing.support.onMainThread
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The caret pan against a real editor, which is the only place the `ExtractedText` branch runs.
 *
 * The unit tests cover the windowed fallback and the line arithmetic; what needs a real editor is
 * that `startOffset` and `selectionStart` land the caret where the geometry asked.
 */
@RunWith(AndroidJUnit4::class)
class CaretPanInstrumentedTest {
    @Test
    fun draggingUpLandsOnTheSameColumnOfTheLineAbove() = onMainThread {
        val host = newHost("abcdefgh\nij\nklmnop")

        CaretPanResolver().apply(host.connection, columns = 0, lines = -1)

        // Column 6 does not fit on "ij", so the caret stops at its end.
        assertEquals(11, host.editText.selectionStart)
    }

    @Test
    fun aSecondDragUpReturnsToTheColumnTheFirstOneWanted() = onMainThread {
        val host = newHost("abcdefgh\nij\nklmnop")
        val resolver = CaretPanResolver()

        resolver.apply(host.connection, columns = 0, lines = -1)
        resolver.apply(host.connection, columns = 0, lines = -1)

        assertEquals(6, host.editText.selectionStart)
    }

    @Test
    fun draggingDownOnTheLastLineGoesToTheEndOfTheText() = onMainThread {
        val host = newHost("abc\ndefgh")
        host.moveCursorTo(6)

        CaretPanResolver().apply(host.connection, columns = 0, lines = 1)

        assertEquals(9, host.editText.selectionStart)
    }

    @Test
    fun aDiagonalDragAppliesBothAxes() = onMainThread {
        val host = newHost("abcd\nefghij")
        host.moveCursorTo(2)

        CaretPanResolver().apply(host.connection, columns = 2, lines = 1)

        assertEquals(9, host.editText.selectionStart)
    }

    private fun newHost(text: String): HostEditor {
        val host = HostEditor(ApplicationProvider.getApplicationContext())
        host.connection.commitText(text, CursorAfterText)
        return host
    }

    private companion object {
        const val CursorAfterText = 1
    }
}
