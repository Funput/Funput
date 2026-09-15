package app.funput.funput.ime.suggestions.lexicon

import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LexiconInstallerTest {
    @get:Rule val temporary = TemporaryFolder()
    private var bytes = fixture("one")
    private var reads = 0
    private val root get() = temporary.root.resolve("Lexicon")
    private fun installer() = LexiconInstaller({ reads++; bytes.inputStream() }, { root })

    @Test fun reusesUnsignedVersionAndCleansOnlyManagedOldFilesAfterSuccess() {
        val crc = CRC32().apply { update(bytes, 24, bytes.size - 24) }.value
        assertTrue(crc > Int.MAX_VALUE)
        var file: File? = null
        assertTrue(installer().attach { file = it; true })
        assertEquals("en-${crc.toString(16).padStart(8, '0')}.lex", file!!.name)
        val timestamp = file!!.lastModified()
        reads = 0
        assertTrue(installer().attach { true })
        assertEquals(1, reads)
        assertEquals(timestamp, file!!.lastModified())
        val unrelated = root.resolve("user.lex").apply { writeText("keep") }
        bytes = fixture("two")
        assertFalse(installer().attach { false })
        assertTrue(file!!.exists())
        assertTrue(installer().attach { true })
        assertFalse(file!!.exists())
        assertTrue(unrelated.exists())
    }

    @Test fun repairsRejectedCacheExactlyOnceWithoutOverwritingMappedInode() {
        lateinit var cached: File
        assertTrue(installer().attach { cached = it; true })
        cached.writeText("damaged")
        var attempts = 0
        cached.inputStream().use { oldInode ->
            assertTrue(installer().attach {
                attempts++
                it.readBytes().contentEquals(bytes)
            })
            assertEquals("damaged", oldInode.bufferedReader().readText())
        }
        assertEquals(2, attempts)
        attempts = 0
        assertFalse(installer().attach { attempts++; false })
        assertEquals(2, attempts)
    }

    @Test fun rejectsTruncationCorruptionAndCopyFailuresWithoutPublishing() {
        for (invalid in listOf(bytes.copyOf(10), bytes.copyOf(bytes.size - 1), bytes + 0,
            bytes.copyOf().apply { this[lastIndex] = 0 })) {
            bytes = invalid
            assertFalse(installer().attach { fail("must not attach invalid bytes"); true })
            assertTrue(root.listFiles().orEmpty().isEmpty())
        }
        assertFalse(LexiconInstaller({ throw IOException("missing") }, { root }).attach { true })
        val blocked = temporary.newFile("blocked")
        bytes = fixture("one")
        assertFalse(LexiconInstaller({ bytes.inputStream() }, { blocked }).attach { true })
    }

    @Test fun interruptedCopyRemovesTemporaryFileAndKeepsPriorVersion() {
        assertTrue(installer().attach { true })
        val previous = root.listFiles()!!.single()
        val original = previous.readBytes()
        bytes = fixture("two")
        var opens = 0
        val failing = LexiconInstaller({
            opens++
            if (opens == 1) bytes.inputStream() else object : java.io.ByteArrayInputStream(bytes) {
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                    if (pos >= 24) throw IOException("interrupted copy")
                    return super.read(buffer, offset, length)
                }
            }
        }, { root })
        assertFalse(failing.attach { fail("copy must fail before JNI"); true })
        assertArrayEquals(original, previous.readBytes())
        assertEquals(listOf(previous), root.listFiles()!!.toList())
    }

    private fun fixture(word: String): ByteArray {
        val body = byteArrayOf(0, 0, 0, 0, word.length.toByte(), 0, 0) + word.toByteArray()
        val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            .put("FPLX".toByteArray()).putShort(1).putShort(0).putInt(1)
            .putInt(word.length + 3).putInt(0).putInt(CRC32().apply { update(body) }.value.toInt())
        return header.array() + body
    }
}
