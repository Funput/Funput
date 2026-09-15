package app.funput.funput.ime.suggestions.lexicon

import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.zip.CRC32

/** Owns only versioned lexicon files; replacement never truncates a mapped inode. */
internal class LexiconInstaller(
    private val source: () -> InputStream,
    private val directory: () -> File,
) {
    fun attach(attachFile: (File) -> Boolean): Boolean = runCatching {
        val header = source().use(::readHeader)
        val root = directory().apply { check(isDirectory || mkdirs()) }
        val file = File(root, "en-${header.crc.toString(16).padStart(8, '0')}.lex")
        val reused = file.isFile
        if (!reused) install(file, header)
        var attached = attachFile(file)
        if (!attached && reused) {
            install(file, header)
            attached = attachFile(file)
        }
        if (attached) {
            root.listFiles()?.filter { it != file && ManagedName.matches(it.name) && it.isFile }
                ?.forEach { it.delete() }
        }
        attached
    }.getOrDefault(false)

    private fun install(destination: File, expected: Header) {
        val temporary = File.createTempFile(".en-", ".tmp", destination.parentFile)
        try {
            source().use { input ->
                val header = readHeader(input)
                check(header == expected) { "Lexicon asset changed during installation" }
                temporary.outputStream().use { output ->
                    output.write(header.bytes)
                    val crc = CRC32()
                    val buffer = ByteArray(8192)
                    var size = HeaderSize
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        size += count
                        check(size <= MaximumSize)
                        crc.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                    check(size == header.size && crc.value == header.crc) { "Invalid lexicon body" }
                    output.fd.sync()
                }
            }
            Files.move(temporary.toPath(), destination.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            temporary.delete()
        }
    }

    private class Header(val bytes: ByteArray, val crc: Long, val size: Int) {
        override fun equals(other: Any?) = other is Header && bytes.contentEquals(other.bytes)
        override fun hashCode() = bytes.contentHashCode()
    }

    private fun readHeader(input: InputStream): Header {
        val bytes = ByteArray(HeaderSize)
        DataInputStream(input).readFully(bytes)
        val data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        check(bytes.take(4) == listOf<Byte>(70, 80, 76, 88) && data.getShort(4).toInt() == 1)
        val words = data.getInt(8).toLong() and 0xffffffffL
        val wordBytes = data.getInt(12).toLong() and 0xffffffffL
        val heavy = data.getInt(16).toLong() and 0xffffffffL
        check(words <= 65535 && data.getShort(6).toInt() == 0)
        val size = HeaderSize + ((words + 15) / 16) * 4 + wordBytes + heavy * 9
        check(size in HeaderSize.toLong()..MaximumSize.toLong())
        return Header(bytes, data.getInt(20).toLong() and 0xffffffffL, size.toInt())
    }

    private companion object {
        const val HeaderSize = 24
        const val MaximumSize = 524288
        val ManagedName = Regex("en-[0-9a-f]{8}\\.lex")
    }
}
