package app.funput.funput.ime.editing.caret

import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy

/** A document that reports no `ExtractedText`, the way a WebView-hosted field does. */
internal class PanDocument(private val text: String) {
    var cursor: Int = text.length
        private set

    fun moveTo(position: Int) {
        cursor = position
    }

    val proxy: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "getExtractedText" -> null
            "getTextBeforeCursor" -> text.substring(
                (cursor - arguments?.first() as Int).coerceAtLeast(0), cursor,
            )
            "getTextAfterCursor" -> text.substring(
                cursor, (cursor + arguments?.first() as Int).coerceAtMost(text.length),
            )
            "setSelection" -> true.also { cursor = arguments?.first() as Int }
            else -> false
        }
    } as InputConnection
}
