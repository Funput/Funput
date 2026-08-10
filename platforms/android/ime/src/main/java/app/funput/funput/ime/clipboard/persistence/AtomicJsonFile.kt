package app.funput.funput.ime.clipboard.persistence

import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

internal class AtomicJsonFile(
    private val destination: File,
    private val replace: (Path, Path) -> Unit = ::replaceFile,
) {
    fun read(): String? {
        if (!destination.isFile) return null
        return runCatching { destination.readText(StandardCharsets.UTF_8) }.getOrNull()
    }

    fun write(content: String): Boolean {
        val directory = destination.parentFile ?: return false
        if (!directory.exists() && !directory.mkdirs()) return false
        if (!directory.isDirectory) return false
        var temporary: File? = null
        return try {
            temporary = File.createTempFile("clipboard-", ".tmp", directory)
            FileOutputStream(temporary).use { output ->
                val writer = output.writer(StandardCharsets.UTF_8)
                writer.write(content)
                writer.flush()
                output.fd.sync()
            }
            replace(temporary.toPath(), destination.toPath())
            true
        } catch (_: Exception) {
            false
        } finally {
            temporary?.delete()
        }
    }

    private companion object {
        fun replaceFile(source: Path, destination: Path) {
            try {
                Files.move(source, destination, ATOMIC_MOVE, REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(source, destination, REPLACE_EXISTING)
            }
        }
    }
}
