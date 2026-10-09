package app.funput.funput.ime.editing.voice

import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy

internal class VoiceTestConnection {
    val commits = mutableListOf<String>()
    var attempts = 0
    var accepts = true
    val proxy = Proxy.newProxyInstance(InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java)) { _, method, arguments ->
        when (method.name) {
            "commitText" -> {
                attempts++
                if (accepts) commits += arguments!!.first().toString()
                accepts
            }
            "toString" -> "VoiceTestConnection"
            else -> null
        }
    } as InputConnection
}
