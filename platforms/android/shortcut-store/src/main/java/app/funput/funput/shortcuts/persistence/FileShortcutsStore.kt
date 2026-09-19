package app.funput.funput.shortcuts.persistence

import android.content.Context
import app.funput.funput.shortcuts.model.ShortcutLibrary
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/** Atomic, app-private shortcut document. Only the containing app writes it. */
class FileShortcutsStore internal constructor(
    directory: File,
    replace: (Path, Path) -> Unit,
) : ShortcutsStoring {
    private val destination = directory.resolve(FileName)
    private val file = AtomicShortcutFile(destination, replace)
    private val lock = StoreLocks.forFile(destination)

    constructor(directory: File) : this(directory, AtomicShortcutFile::replaceAtomically)

    override fun load(): ShortcutLibrary = synchronized(lock) { read() }

    override fun save(library: ShortcutLibrary) = synchronized(lock) {
        library.validate()
        read()
        file.write(ShortcutLibraryJson.encode(library))
    }

    private fun read(): ShortcutLibrary {
        val text = file.read() ?: return ShortcutLibrary()
        return try {
            ShortcutLibraryJson.decode(text)
        } catch (error: ShortcutsStorageError) {
            if (error == ShortcutsStorageError.DuplicateTrigger) {
                throw ShortcutsStorageError.InvalidData
            }
            throw error
        } catch (_: Exception) {
            throw ShortcutsStorageError.InvalidData
        }
    }

    companion object {
        private const val DirectoryName = "Shortcuts"
        private const val FileName = "shortcuts.json"

        fun from(context: Context): FileShortcutsStore = FileShortcutsStore(
            context.applicationContext.filesDir.resolve(DirectoryName),
        )
    }
}

private object StoreLocks {
    private val values = ConcurrentHashMap<String, Any>()

    fun forFile(file: File): Any = values.computeIfAbsent(
        runCatching { file.canonicalPath }.getOrElse { file.absolutePath },
    ) { Any() }
}
