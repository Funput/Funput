package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.VietnameseEngine
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidCompositionSessionTest {
    @Test
    fun `whitespace and ascii punctuation are boundaries`() {
        listOf(' ', '\n', '\t', ',', '.', '!', '?', '-', '"').forEach { character ->
            assertTrue(CompositionBoundary.isBoundary(character.code))
        }
    }

    @Test
    fun `letters digits emoji and unicode punctuation are not boundaries`() {
        listOf('a'.code, '1'.code, '9'.code, 'á'.code, '—'.code, 0x1F600).forEach { codePoint ->
            assertFalse(CompositionBoundary.isBoundary(codePoint))
        }
    }

    @Test
    fun `engine buffer is rendered as composing text after every key`() {
        val engine = ScriptedEngine(processed = ArrayDeque(listOf("a", "an", "án", "ánh")))
        val connection = RecordingConnection()
        val session = testSession(engine)

        "an1h".forEach { character -> session.input(connection.proxy, character.toString()) }

        assertEquals(listOf("a", "an", "án", "ánh"), connection.composingTexts)
        assertEquals("ánh", session.composingText)
    }

    @Test
    fun `ordinary boundary finishes composition and commits itself`() {
        val connection = RecordingConnection()
        val session = testSession(ScriptedEngine(ArrayDeque(listOf("á"))))
        session.input(connection.proxy, "a")

        session.input(connection.proxy, " ")

        assertEquals(1, connection.finishCount)
        assertEquals(listOf(" "), connection.committedTexts)
        assertFalse(session.isComposing)
    }

    @Test
    fun `boundary replacement commits engine output over composing span`() {
        val connection = RecordingConnection()
        val engine = ScriptedEngine(ArrayDeque(listOf("cảd")), boundaryOutput = "card ")
        val session = testSession(engine)
        session.input(connection.proxy, "d")

        session.input(connection.proxy, " ")

        assertEquals(listOf("card "), connection.committedTexts)
        assertEquals(0, connection.finishCount)
    }

    @Test
    fun `backspace updates active composing text`() {
        val connection = RecordingConnection()
        val engine = ScriptedEngine(ArrayDeque(listOf("an")), backspaceOutput = "a")
        val session = testSession(engine)
        session.input(connection.proxy, "n")

        assertTrue(session.backspace(connection.proxy))

        assertEquals(listOf("an", "a"), connection.composingTexts)
        assertEquals("a", session.composingText)
    }

    @Test
    fun `enabled state is forwarded to the native engine adapter`() {
        val engine = ScriptedEngine(ArrayDeque())
        val session = testSession(engine)

        session.setEnabled(false)

        assertEquals(false, engine.enabled)
    }
}

internal fun testSession(engine: VietnameseEngine) = AndroidCompositionSession(
    engine = engine,
    composingTextFactory = { text -> text },
)

internal class ScriptedEngine(
    private val processed: ArrayDeque<String>,
    private val boundaryOutput: String? = null,
    private val backspaceOutput: String = "",
) : VietnameseEngine {
    var configuration: EngineConfiguration? = null
        private set
    var enabled: Boolean? = null
        private set

    override fun process(codePoint: Int): String = processed.removeFirst()
    override fun processBoundary(codePoint: Int): String? = boundaryOutput
    override fun backspace(): String = backspaceOutput
    override fun configure(configuration: EngineConfiguration) {
        this.configuration = configuration
    }
    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
    override fun clear() = Unit
    override fun close() = Unit
}

internal class RecordingConnection {
    val composingTexts = mutableListOf<String>()
    val committedTexts = mutableListOf<String>()
    var finishCount = 0

    val proxy: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "setComposingText" -> true.also {
                val value = arguments?.first() as CharSequence
                composingTexts += value.toString()
            }
            "commitText" -> true.also { committedTexts += arguments?.first().toString() }
            "finishComposingText" -> true.also { finishCount++ }
            "toString" -> "RecordingInputConnection"
            else -> false
        }
    } as InputConnection
}
