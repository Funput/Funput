package app.funput.funput.shortcuts.persistence

import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

internal class AtomicShortcutFile(
    private val destination: File,
    private val replace: (Path, Path) -> Unit = Companion::replaceAtomically,
) {
    fun read(): String? {
        if (!destination.exists()) return null
        if (!destination.isFile) throw ShortcutsStorageError.ReadFailed
        return runCatching { destination.readText(StandardCharsets.UTF_8) }
            .getOrElse { throw ShortcutsStorageError.ReadFailed }
    }

    fun write(content: String) {
        val directory = destination.parentFile ?: throw ShortcutsStorageError.WriteFailed
        if (!directory.exists() && !directory.mkdirs()) throw ShortcutsStorageError.WriteFailed
        if (!directory.isDirectory) throw ShortcutsStorageError.WriteFailed
        var temporary: File? = null
        try {
            temporary = File.createTempFile("shortcuts-", ".tmp", directory)
            FileOutputStream(temporary).use { output ->
                output.write(content.toByteArray(StandardCharsets.UTF_8))
                output.flush()
                output.fd.sync()
            }
            replace(temporary.toPath(), destination.toPath())
        } catch (error: ShortcutsStorageError) {
            throw error
        } catch (_: Exception) {
            throw ShortcutsStorageError.WriteFailed
        } finally {
            temporary?.delete()
        }
    }

    companion object {
        fun replaceAtomically(source: Path, destination: Path) {
            try {
                Files.move(source, destination, ATOMIC_MOVE, REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(source, destination, REPLACE_EXISTING)
            }
        }
    }
}
