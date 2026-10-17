package app.funput.funput.ime.suggestions.correction

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.concurrent.Executor

/** The words the user kept as typed, and the personal-suggestion reset they survived. */
internal data class KeptWords(val resetToken: String?, val words: List<String>)

/**
 * Persists [KeptWords] in a no-backup file: the reset token on the first line, then one word
 * per line, oldest first. Reads and writes run on [io]; results come back on [main].
 */
internal class TypoKeptWordsStore(
    private val file: File,
    private val io: Executor,
    private val main: Executor,
) {
    fun load(onLoaded: (KeptWords) -> Unit) = io.execute {
        val value = read()
        main.execute { onLoaded(value) }
    }

    fun save(value: KeptWords) = io.execute { write(value) }

    internal fun read(): KeptWords = runCatching {
        if (!file.isFile) return@runCatching Empty
        val lines = file.readLines()
        KeptWords(lines.firstOrNull()?.ifEmpty { null }, lines.drop(1).filter(String::isNotBlank))
    }.getOrDefault(Empty)

    internal fun write(value: KeptWords) {
        runCatching {
            val directory = file.parentFile?.apply { check(isDirectory || mkdirs()) } ?: return
            val temporary = File.createTempFile(".kept-", ".tmp", directory)
            val body = buildString {
                appendLine(value.resetToken.orEmpty())
                value.words.forEach(::appendLine)
            }
            temporary.writeText(body)
            Files.move(temporary.toPath(), file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        }
    }

    private companion object {
        val Empty = KeptWords(null, emptyList())
    }
}
