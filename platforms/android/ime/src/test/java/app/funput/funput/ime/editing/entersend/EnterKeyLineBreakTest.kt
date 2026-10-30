package app.funput.funput.ime.editing.entersend

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.toImeEditCommand
import app.funput.funput.ime.editing.gestures.IdleEngine
import app.funput.funput.ime.editing.typing.forEditor
import app.funput.funput.keyboard.model.KeyAction
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Test

/** Enter sends in a chosen chat app, and a held Enter (or Shift+Enter) still breaks the line. */
class EnterKeyLineBreakTest {
    private val editor = RecordingEditor()
    private val handler = ImeKeyActionHandler(
        composition = AndroidCompositionSession(IdleEngine()),
        editor = InputConnectionEditor(),
        connection = { editor.proxy },
        enterCommand = { EnterToSendPolicy.Send.command },
    )

    @Test fun `enter performs send`() {
        handler.start(allowComposition = false)
        handler.onKeyAction(KeyAction.Enter)

        assertEquals(listOf(EditorInfo.IME_ACTION_SEND), editor.actions)
        assertEquals(emptyList<String>(), editor.commits)
    }

    @Test fun `a line break commits a new line instead of sending`() {
        handler.start(allowComposition = false)
        handler.onKeyAction(KeyAction.NewLine)

        assertEquals(listOf("\n"), editor.commits)
        assertEquals(emptyList<Int>(), editor.actions)
    }

    @Test fun `a line break goes through composition while typing Vietnamese`() {
        handler.start(allowComposition = true)
        handler.onKeyAction(KeyAction.NewLine)

        assertEquals(emptyList<Int>(), editor.actions)
        assertEquals("\n", editor.commits.joinToString(""))
    }

    @Test fun `a single line field turns a line break back into enter`() {
        val singleLine = EditorInfoPolicy.Default.copy(isMultiline = false)
        val multiLine = EditorInfoPolicy.Default.copy(isMultiline = true)

        assertEquals(KeyAction.Enter, KeyAction.NewLine.forEditor(singleLine))
        assertEquals(KeyAction.NewLine, KeyAction.NewLine.forEditor(multiLine))
        assertEquals(KeyAction.Enter, KeyAction.Enter.forEditor(multiLine))
    }

    @Test fun `the edit command for a line break is always a new line`() {
        assertEquals(
            ImeEditCommand.CommitText("\n"),
            KeyAction.NewLine.toImeEditCommand(enterCommand = EnterToSendPolicy.Send.command),
        )
    }
}

private class RecordingEditor {
    val commits = mutableListOf<String>()
    val actions = mutableListOf<Int>()

    val proxy: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "commitText" -> true.also { commits += arguments?.first().toString() }
            "performEditorAction" -> true.also { actions += arguments?.first() as Int }
            "getSelectedText", "getTextBeforeCursor", "getTextAfterCursor", "getExtractedText" -> null
            "toString" -> "RecordingEditor"
            else -> when (method.returnType) {
                Boolean::class.javaPrimitiveType -> true
                Int::class.javaPrimitiveType -> 0
                else -> null
            }
        }
    } as InputConnection
}
