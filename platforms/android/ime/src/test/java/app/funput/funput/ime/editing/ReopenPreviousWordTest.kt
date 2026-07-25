package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.VietnameseEngine
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Re-opening a committed word after Backspace, so the next keystroke retones it.
 * The document must be left untouched whenever the engine refuses the word.
 */
class ReopenPreviousWordTest {
    @Test
    fun `reopens the word before the caret`() {
        val editor = FakeEditor(textBeforeCursor = "chào")
        val engine = AdoptingEngine(adoptable = setOf("chào"))
        val session = testSession(engine)

        assertTrue(session.reopenPreviousWord(editor.proxy))

        assertEquals("chào", engine.adopted)
        assertEquals(listOf(4), editor.deletedBefore)
        assertEquals(listOf("chào"), editor.composingTexts)
        assertEquals("chào", session.composingText)
        assertEquals(1, editor.batchDepthPeak) // one atomic edit, not two
    }

    @Test
    fun `takes only the last word, not the text before it`() {
        val editor = FakeEditor(textBeforeCursor = "xin chào")
        val engine = AdoptingEngine(adoptable = setOf("chào"))

        assertTrue(testSession(engine).reopenPreviousWord(editor.proxy))
        assertEquals(listOf(4), editor.deletedBefore)
    }

    @Test
    fun `leaves the document alone when the engine refuses the word`() {
        val editor = FakeEditor(textBeforeCursor = "hello")
        val engine = AdoptingEngine(adoptable = emptySet())
        val session = testSession(engine)

        assertFalse(session.reopenPreviousWord(editor.proxy))

        assertTrue(editor.deletedBefore.isEmpty())
        assertTrue(editor.composingTexts.isEmpty())
        assertEquals("", session.composingText)
    }

    @Test
    fun `skips when the caret sits on a boundary`() {
        val editor = FakeEditor(textBeforeCursor = "chào ")
        val engine = AdoptingEngine(adoptable = setOf("chào"))

        assertFalse(testSession(engine).reopenPreviousWord(editor.proxy))
        assertNull(engine.adopted)
    }

    @Test
    fun `skips while a selection is active`() {
        val editor = FakeEditor(textBeforeCursor = "chào", selectedText = "chào")
        val engine = AdoptingEngine(adoptable = setOf("chào"))

        assertFalse(testSession(engine).reopenPreviousWord(editor.proxy))
        assertNull(engine.adopted)
    }

    @Test
    fun `skips while a composition is already live`() {
        val editor = FakeEditor(textBeforeCursor = "chào")
        val engine = AdoptingEngine(adoptable = setOf("chào"))
        val session = testSession(engine)
        session.input(editor.proxy, "a") // starts composing
        editor.composingTexts.clear()

        assertFalse(session.reopenPreviousWord(editor.proxy))
        assertNull(engine.adopted)
        assertTrue(editor.composingTexts.isEmpty())
    }

    @Test
    fun `skips when there is no connection`() {
        assertFalse(testSession(AdoptingEngine(setOf("chào"))).reopenPreviousWord(null))
    }
}

/** Editor stub covering the calls the re-open path makes. */
private class FakeEditor(
    private val textBeforeCursor: String,
    private val selectedText: String? = null,
) {
    val composingTexts = mutableListOf<String>()
    val deletedBefore = mutableListOf<Int>()
    var batchDepthPeak = 0
        private set
    private var batchDepth = 0

    val proxy: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "getTextBeforeCursor" -> textBeforeCursor.takeLast(arguments?.first() as Int)
            "getSelectedText" -> selectedText
            "deleteSurroundingText" -> true.also { deletedBefore += arguments?.first() as Int }
            "setComposingText" -> true.also {
                composingTexts += (arguments?.first() as CharSequence).toString()
            }
            "beginBatchEdit" -> true.also {
                batchDepth += 1
                batchDepthPeak = maxOf(batchDepthPeak, batchDepth)
            }
            "endBatchEdit" -> true.also { batchDepth -= 1 }
            "finishComposingText" -> true
            "toString" -> "FakeEditor"
            else -> false
        }
    } as InputConnection
}

/** Engine stub whose `adopt` accepts a fixed word set, standing in for the syllable gate. */
private class AdoptingEngine(private val adoptable: Set<String>) : VietnameseEngine {
    var adopted: String? = null
        private set

    override fun adopt(word: String): Boolean =
        adoptable.contains(word).also { if (it) adopted = word }

    override fun process(codePoint: Int): String = "a"
    override fun processBoundary(codePoint: Int): String? = null
    override fun backspace(): String = ""
    override fun configure(configuration: EngineConfiguration) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
    override fun clear() = Unit
    override fun close() = Unit
}
