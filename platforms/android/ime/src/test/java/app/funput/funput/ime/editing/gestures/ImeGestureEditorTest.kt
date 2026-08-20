package app.funput.funput.ime.editing.gestures

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.keyboard.model.KeyAction
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Test

class ImeGestureEditorTest {
    @Test
    fun aSecondQuickSpaceBecomesAFullStop() {
        val document = GestureDocument("xin chao")
        spaceTwice(document)
        assertEquals("xin chao. ", document.text)
    }

    @Test
    fun disablingTheSettingResetsTheDoubleTapSequence() {
        val document = GestureDocument("xin chao")
        val handler = handler(document)
        handler.onKeyAction(KeyAction.Space)
        handler.smartGesturesEnabled = false
        handler.smartGesturesEnabled = true
        handler.onKeyAction(KeyAction.Space)
        assertEquals("xin chao  ", document.text)
    }

    @Test
    fun aKeyBetweenTheSpacesBreaksTheSequence() {
        val document = GestureDocument("xin")
        val handler = handler(document)
        handler.onKeyAction(KeyAction.Space)
        handler.onKeyAction(KeyAction.Input("character-a", "a"))
        handler.onKeyAction(KeyAction.Space)
        handler.onKeyAction(KeyAction.Space)
        assertEquals("xin a. ", document.text)
    }

    @Test
    fun nothingIsSubstitutedWhenTheSettingIsOff() {
        val document = GestureDocument("xin chao")
        val handler = handler(document)
        handler.smartGesturesEnabled = false
        spaceTwice(document, handler)
        assertEquals("xin chao  ", document.text)
    }

    @Test
    fun aPunctuatedEndingIsNotSubstitutedAgain() {
        val document = GestureDocument("xin chao. ")
        spaceTwice(document)
        assertEquals("xin chao.   ", document.text)
    }

    @Test
    fun finishResetsTheDoubleTapSequence() {
        val document = GestureDocument("xin chao")
        val handler = handler(document)
        handler.onKeyAction(KeyAction.Space)
        handler.finish()
        handler.start(allowComposition = false)
        handler.onKeyAction(KeyAction.Space)
        assertEquals("xin chao  ", document.text)
    }

    private fun spaceTwice(
        document: GestureDocument,
        handler: ImeKeyActionHandler = handler(document),
    ) {
        handler.onKeyAction(KeyAction.Space)
        handler.onKeyAction(KeyAction.Space)
    }
}

internal fun handler(document: GestureDocument) = ImeKeyActionHandler(
    composition = AndroidCompositionSession(IdleEngine()),
    editor = InputConnectionEditor(),
    connection = { document.proxy },
    enterCommand = { ImeEditCommand.CommitText("\n") },
).also { it.start(allowComposition = false) }

internal class IdleEngine : VietnameseEngine {
    override fun process(codePoint: Int): String = ""
    override fun processBoundary(codePoint: Int): String? = null
    override fun backspace(): String = ""
    override fun configure(configuration: EngineConfiguration) = Unit
    override fun adopt(word: String): Boolean = false
    override fun setEnabled(enabled: Boolean) = Unit
    override fun clear() = Unit
    override fun close() = Unit
}

internal class GestureDocument(initial: String) {
    private val document = StringBuilder(initial)
    var cursor: Int = initial.length
        private set
    val text: String get() = document.toString()
    val proxy: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "commitText" -> true.also {
                val value = arguments?.first().toString()
                document.insert(cursor, value)
                cursor += value.length
            }
            "deleteSurroundingText" -> true.also {
                val count = arguments?.first() as Int
                document.delete(cursor - count, cursor)
                cursor -= count
            }
            "getSelectedText" -> null
            "getTextBeforeCursor" -> document.substring(
                (cursor - arguments?.first() as Int).coerceAtLeast(0), cursor,
            )
            "getTextAfterCursor" -> document.substring(
                cursor, (cursor + arguments?.first() as Int).coerceAtMost(document.length),
            )
            "setSelection" -> true.also { cursor = arguments?.first() as Int }
            "getExtractedText" -> null
            "finishComposingText", "beginBatchEdit", "endBatchEdit" -> true
            else -> false
        }
    } as InputConnection
}
