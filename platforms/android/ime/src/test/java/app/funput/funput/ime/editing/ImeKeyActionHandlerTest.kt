package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.keyboard.model.KeyAction
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ImeKeyActionHandlerTest {
    @Test
    fun `ASCII editor commits directly without invoking composition`() {
        val engine = RecordingEngine()
        val connection = RecordingCommitConnection()
        val handler = ImeKeyActionHandler(
            composition = AndroidCompositionSession(engine),
            editor = InputConnectionEditor(),
            connection = { connection.proxy },
            enterCommand = { ImeEditCommand.CommitText("\n") },
        )

        handler.start(allowComposition = false)
        handler.onKeyAction(KeyAction.Input("character-s", "s"))

        assertFalse(engine.enabledValue)
        assertEquals(0, engine.processCount)
        assertEquals(listOf("s"), connection.committedTexts)
    }

    @Test
    fun `clipboard selection finishes composition and commits exact text once`() {
        val connection = RecordingCommitConnection()
        val handler = ImeKeyActionHandler(
            composition = AndroidCompositionSession(RecordingEngine()),
            editor = InputConnectionEditor(),
            connection = { connection.proxy },
            enterCommand = { ImeEditCommand.CommitText("\n") },
        )
        handler.start()
        handler.onClipboardSelected(" Việt,\\\nline ")

        assertEquals(listOf(" Việt,\\\nline "), connection.committedTexts)
        assertEquals(AuthoredSuggestionUpdate.Empty, handler.takeSuggestionUpdate())
    }

    @Test
    fun `ASCII editor tracks and accepts a direct suggestion`() {
        val connection = RecordingCommitConnection()
        val handler = handler(connection)
        handler.start(allowComposition = false)
        handler.onKeyAction(KeyAction.Input("character-b", "b"))
        handler.onKeyAction(KeyAction.Input("character-a", "a"))

        assertEquals("ba", handler.takeSuggestionUpdate().prefix)
        assertEquals(true, handler.acceptSuggestion("bạn", "ba"))
        assertEquals("bạn ", connection.document.toString())
    }

    @Test
    fun `direct selection updates preserve matching prefix and clear moved cursor`() {
        val connection = RecordingCommitConnection()
        val handler = handler(connection)
        handler.start(allowComposition = false)
        handler.onKeyAction(KeyAction.Input("character-b", "b"))
        handler.onKeyAction(KeyAction.Input("character-a", "a"))

        handler.onSelectionChanged(2, 2, -1)
        assertEquals("ba", handler.takeSuggestionUpdate().prefix)
        connection.moveCursor(0)
        handler.onSelectionChanged(0, 0, -1)
        handler.onKeyAction(KeyAction.Input("character-x", "x"))

        assertEquals("x", handler.takeSuggestionUpdate().prefix)
    }

    @Test
    fun `secure editor never tracks direct input`() {
        val connection = RecordingCommitConnection()
        val handler = handler(connection)
        handler.start(allowComposition = false, allowSuggestions = false)
        handler.onKeyAction(KeyAction.Input("character-p", "password"))

        assertEquals(AuthoredSuggestionUpdate.Empty, handler.takeSuggestionUpdate())
        assertFalse(handler.acceptSuggestion("password", "pa"))
    }
}

private class RecordingEngine : VietnameseEngine {
    var enabledValue = true
    var processCount = 0

    override fun process(codePoint: Int): String = "".also { processCount++ }
    override fun processBoundary(codePoint: Int): String? = null
    override fun backspace(): String = ""
    override fun configure(configuration: EngineConfiguration) = Unit
    override fun adopt(word: String): Boolean = false
    override fun setEnabled(enabled: Boolean) { enabledValue = enabled }
    override fun clear() = Unit
    override fun close() = Unit
}

private class RecordingCommitConnection {
    val committedTexts = mutableListOf<String>()
    val document = StringBuilder()
    private var cursor = 0

    fun moveCursor(index: Int) {
        cursor = index
    }

    val proxy: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "commitText" -> true.also {
                val text = arguments?.first().toString()
                committedTexts += text
                document.insert(cursor, text)
                cursor += text.length
            }
            "deleteSurroundingText" -> true.also {
                val count = arguments?.first() as Int
                document.delete(cursor - count, cursor)
                cursor -= count
            }
            "getSelectedText" -> null
            "getTextBeforeCursor" -> {
                val count = arguments?.first() as Int
                document.substring((cursor - count).coerceAtLeast(0), cursor)
            }
            "beginBatchEdit", "endBatchEdit" -> true
            "toString" -> "RecordingCommitConnection"
            else -> false
        }
    } as InputConnection
}

private fun handler(connection: RecordingCommitConnection) = ImeKeyActionHandler(
    composition = AndroidCompositionSession(RecordingEngine()),
    editor = InputConnectionEditor(),
    connection = { connection.proxy },
    enterCommand = { ImeEditCommand.CommitText("\n") },
)
