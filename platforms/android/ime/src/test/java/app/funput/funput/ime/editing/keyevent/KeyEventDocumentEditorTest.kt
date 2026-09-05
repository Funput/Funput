package app.funput.funput.ime.editing.keyevent

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.CompositionDocumentEditor
import app.funput.funput.ime.editing.CompositionRenderMode
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyEventDocumentEditorTest {
    @Test
    fun keyEventModeNeverUsesInputConnectionTextApis() {
        val host = SandboxHost()
        val editor = CompositionDocumentEditor(
            composingTextFactory = { it },
            keyEventWriter = KeyEventBufferWriter(host::record),
        )

        assertTrue(editor.update(host.connection, CompositionRenderMode.KEY_EVENT, "", "a"))
        assertTrue(editor.update(host.connection, CompositionRenderMode.KEY_EVENT, "a", "á"))
        assertTrue(
            editor.commitBoundary(
                host.connection,
                CompositionRenderMode.KEY_EVENT,
                "á",
                replacement = null,
                boundary = " ",
            ),
        )

        assertEquals(0, host.commitTextCount)
        assertEquals(0, host.setComposingCount)
        assertEquals(
            listOf(
                KeyEventStroke.Text("a"),
                KeyEventStroke.Delete,
                KeyEventStroke.Text("á"),
                KeyEventStroke.Text(" "),
            ),
            host.strokes,
        )
    }
}

private class SandboxHost {
    val strokes = mutableListOf<KeyEventStroke>()
    var commitTextCount = 0
    var setComposingCount = 0
    val connection: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "beginBatchEdit", "endBatchEdit" -> true
            "commitText" -> true.also { commitTextCount += 1 }
            "setComposingText" -> true.also { setComposingCount += 1 }
            "finishComposingText" -> true
            "toString" -> "SandboxHost"
            else -> false
        }
    } as InputConnection

    fun record(connection: InputConnection, stroke: KeyEventStroke): Boolean {
        check(connection === this.connection)
        strokes += stroke
        return true
    }
}
