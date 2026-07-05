package app.funput.funput.ime.editing

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.ime.editing.support.HostEditor
import app.funput.funput.ime.editing.support.onMainThread
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises editor commands against a real Editable, where grapheme rules actually apply. */
@RunWith(AndroidJUnit4::class)
class InputConnectionEditorInstrumentedTest {
    private val editor = InputConnectionEditor()

    @Test
    fun backspaceDeletesCombiningGraphemeAsOneUnit() = onMainThread {
        val host = newHost()
        host.connection.commitText("é", CursorAfterText) // "e" + combining acute accent

        editor.execute(host.connection, ImeEditCommand.DeleteBackward)

        assertEquals("", host.text)
    }

    @Test
    fun backspaceDeletesSurrogatePairEmojiAsOneUnit() = onMainThread {
        val host = newHost()
        host.connection.commitText("👍", CursorAfterText) // 👍 thumbs-up

        editor.execute(host.connection, ImeEditCommand.DeleteBackward)

        assertEquals("", host.text)
    }

    @Test
    fun backspaceRemovesActiveSelection() = onMainThread {
        val host = newHost()
        host.connection.commitText("abc", CursorAfterText)
        host.connection.setSelection(0, 3)

        editor.execute(host.connection, ImeEditCommand.DeleteBackward)

        assertEquals("", host.text)
    }

    @Test
    fun commitTextInsertsAndAdvancesCursor() = onMainThread {
        val host = newHost()

        editor.execute(host.connection, ImeEditCommand.CommitText("xin"))

        assertEquals("xin", host.text)
        assertEquals(3, host.editText.selectionEnd)
    }

    private fun newHost() = HostEditor(ApplicationProvider.getApplicationContext())

    private companion object {
        const val CursorAfterText = 1
    }
}
