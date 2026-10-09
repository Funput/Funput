package app.funput.funput.ime.editing.layout

import android.view.inputmethod.InputConnection
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.ui.KeyboardPanel
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Test

class LetterPageReturnTest {
    private val document = StringBuilder()
    private var selection: String? = null
    private val events = mutableListOf<String>()
    private var connection: InputConnection? = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "getTextBeforeCursor" -> document.takeLast(arguments?.first() as Int).toString()
            "getSelectedText" -> selection
            else -> null
        }
    } as InputConnection
    private val letterReturn = LetterPageReturn { connection }

    @Test
    fun `a space after punctuation on the symbol panel returns once the space is in`() {
        document.append("chao!")

        space(KeyboardPanel.SYMBOLS)

        assertEquals(listOf("commit:chao! ", "letters:chao! "), events)
    }

    @Test
    fun `numbers keep the symbol panel`() {
        document.append("10")

        space(KeyboardPanel.SYMBOLS)

        assertEquals(listOf("commit:10 "), events)
    }

    @Test
    fun `the letters panel and other keys are left alone`() {
        document.append("chao.")

        space(KeyboardPanel.LETTERS)
        letterReturn.dispatch(KeyAction.Enter, KeyboardPanel.SYMBOLS, ::commit, ::showLetters)

        assertEquals(listOf("commit:chao. ", "commit:chao.  "), events)
    }

    @Test
    fun `a selection keeps the symbol panel`() {
        document.append("xong?")
        selection = "xong"

        space(KeyboardPanel.SYMBOLS)

        assertEquals(listOf("commit:xong? "), events)
    }

    @Test
    fun `a missing editor keeps the symbol panel`() {
        document.append("xong?")
        connection = null

        space(KeyboardPanel.SYMBOLS)

        assertEquals(listOf("commit:xong? "), events)
    }

    @Test
    fun `turning the setting off keeps the symbol panel`() {
        document.append("xong?")
        letterReturn.enabled = false

        space(KeyboardPanel.SYMBOLS)

        assertEquals(listOf("commit:xong? "), events)
    }

    private fun space(panel: KeyboardPanel) =
        letterReturn.dispatch(KeyAction.Space, panel, ::commit, ::showLetters)

    private fun commit() {
        document.append(' ')
        events += "commit:$document"
    }

    private fun showLetters() {
        events += "letters:$document"
    }
}
