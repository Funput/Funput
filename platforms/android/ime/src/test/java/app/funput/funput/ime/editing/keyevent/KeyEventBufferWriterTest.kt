package app.funput.funput.ime.editing.keyevent

import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyEventBufferWriterTest {
    @Test
    fun prefixGrowthSendsOnlyTheSuffix() {
        val host = RecordingHost()
        val writer = KeyEventBufferWriter(host::record)

        writer.replace(host.connection, "", "p")
        writer.replace(host.connection, "p", "ph")
        writer.replace(host.connection, "ph", "phu")

        assertEquals(
            listOf(
                KeyEventStroke.Text("p"),
                KeyEventStroke.Text("h"),
                KeyEventStroke.Text("u"),
            ),
            host.strokes,
        )
    }

    @Test
    fun toneTransformDeletesThenInsertsUnicode() {
        val host = RecordingHost()
        val writer = KeyEventBufferWriter(host::record)

        writer.replace(host.connection, "", "a")
        writer.replace(host.connection, "a", "á")

        assertEquals(
            listOf(KeyEventStroke.Text("a"), KeyEventStroke.Delete, KeyEventStroke.Text("á")),
            host.strokes,
        )
    }
}

private class RecordingHost {
    val strokes = mutableListOf<KeyEventStroke>()
    val connection: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "beginBatchEdit", "endBatchEdit" -> true
            "toString" -> "RecordingHost"
            else -> false
        }
    } as InputConnection

    fun record(connection: InputConnection, stroke: KeyEventStroke): Boolean {
        check(connection === this.connection)
        strokes += stroke
        return true
    }
}
