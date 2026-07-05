package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
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

        handler.start(KeyboardInputMethod.TELEX, allowComposition = false)
        handler.onKeyAction(KeyAction.Input("character-s", "s"))

        assertFalse(engine.enabledValue)
        assertEquals(0, engine.processCount)
        assertEquals(listOf("s"), connection.committedTexts)
    }
}

private class RecordingEngine : VietnameseEngine {
    var enabledValue = true
    var processCount = 0

    override fun process(codePoint: Int): String = "".also { processCount++ }
    override fun processBoundary(codePoint: Int): String? = null
    override fun backspace(): String = ""
    override fun setInputMethod(method: KeyboardInputMethod) = Unit
    override fun setToneStyle(style: ToneStyle) = Unit
    override fun setEnabled(enabled: Boolean) { enabledValue = enabled }
    override fun clear() = Unit
    override fun close() = Unit
}

private class RecordingCommitConnection {
    val committedTexts = mutableListOf<String>()
    val proxy: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "commitText" -> true.also { committedTexts += arguments?.first().toString() }
            "toString" -> "RecordingCommitConnection"
            else -> false
        }
    } as InputConnection
}
