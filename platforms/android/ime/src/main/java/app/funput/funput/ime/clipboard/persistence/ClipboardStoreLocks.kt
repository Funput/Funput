package app.funput.funput.ime.clipboard.persistence

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** One lock per history file, shared by every store instance opened on it. */
internal object ClipboardStoreLocks {
    private val locks = ConcurrentHashMap<String, Any>()

    fun forFile(file: File): Any {
        val path = runCatching { file.canonicalPath }.getOrElse { file.absolutePath }
        return locks.computeIfAbsent(path) { Any() }
    }
}
