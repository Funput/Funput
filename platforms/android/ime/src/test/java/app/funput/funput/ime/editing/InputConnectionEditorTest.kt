package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InputConnectionEditorTest {
    @Test
    fun `perform action command is sent to input connection`() {
        var receivedAction: Int? = null
        val connection = Proxy.newProxyInstance(
            InputConnection::class.java.classLoader,
            arrayOf(InputConnection::class.java),
        ) { _, method, arguments ->
            when (method.name) {
                "performEditorAction" -> true.also { receivedAction = arguments?.first() as Int }
                "toString" -> "InputConnectionTestDouble"
                else -> false
            }
        } as InputConnection

        val result = InputConnectionEditor().execute(
            connection,
            ImeEditCommand.PerformEditorAction(73),
        )

        assertTrue(result)
        assertEquals(73, receivedAction)
    }
}
