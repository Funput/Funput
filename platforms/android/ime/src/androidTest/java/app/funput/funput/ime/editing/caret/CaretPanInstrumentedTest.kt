package app.funput.funput.ime.editing.caret

import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.ime.editing.support.HostEditor
import app.funput.funput.ime.editing.support.onMainThread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The caret pan against a real editor, which is where the two halves that cannot run on the JVM
 * live: the `ExtractedText` branch of sideways panning, and vertical panning, which is nothing but
 * arrow keys the editor answers itself.
 */
@RunWith(AndroidJUnit4::class)
class CaretPanInstrumentedTest {
    @Test
    fun sidewaysPanningMovesWholeCharacters() = onMainThread {
        val host = newHost("xin chao")

        CaretPanResolver().apply(host.connection, columns = -3)

        assertEquals(5, host.editText.selectionStart)
    }

    @Test
    fun theEditorAnswersAnArrowKeyByMovingUpAVisualLine() = onMainThread {
        // One paragraph with no newline in it, laid out narrow so the editor wraps it. This is the
        // case iOS cannot serve at all: there is no line break to measure, only a wrap the editor
        // alone knows about.
        val host = newHost(
            "mot doan van rat dai khong he co xuong dong nao ca no se tu dong wrap " +
                "thanh nhieu dong hien thi tren man hinh",
        )
        host.layOutNarrow()
        host.moveCursorTo(host.editText.text.length)
        val startLine = host.lineOfCaret()
        assertTrue("the text must wrap for this test to mean anything", startLine > 0)

        // Dispatched to the view rather than through the connection: an unattached EditText has no
        // window for sendKeyEvent to route through. What this pins is the half that matters and
        // that no unit test can reach — that the editor reads DPAD_UP as a *visual* line. The IME
        // half is CaretLineKeysTest, and sendKeyEvent's delivery is already relied on in
        // production by the backspace path in CommittedBufferWriter.
        host.pressKey(KeyEvent.KEYCODE_DPAD_UP)

        assertEquals(startLine - 1, host.lineOfCaret())
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
