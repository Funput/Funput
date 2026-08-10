package app.funput.funput.ime.clipboard.platform

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

internal object ClipboardSourceToken {
    fun fromTimestamp(timestamp: Long): String? =
        timestamp.takeIf { it > 0 }?.let { "android:v1:timestamp:$it" }

    fun fromText(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(StandardCharsets.UTF_8))
        return "android:v1:text-sha256:${digest.toHex()}"
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
