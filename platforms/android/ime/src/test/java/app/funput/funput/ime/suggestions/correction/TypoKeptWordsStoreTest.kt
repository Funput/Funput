package app.funput.funput.ime.suggestions.correction

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Executor

class TypoKeptWordsStoreTest {
    private val directory = Files.createTempDirectory("kept").toFile()
    private val file = File(directory, "TypoCorrection/kept-words.txt")
    private val store = TypoKeptWordsStore(file, Executor(Runnable::run), Executor(Runnable::run))

    @Test
    fun roundTrip() {
        store.write(KeptWords("token", listOf("abh", "dduwowfnh")))
        assertEquals(KeptWords("token", listOf("abh", "dduwowfnh")), store.read())
    }

    @Test
    fun noTokenYet() {
        store.write(KeptWords(null, listOf("abh")))
        assertEquals(KeptWords(null, listOf("abh")), store.read())
    }

    @Test
    fun missingFileReadsEmpty() {
        assertEquals(KeptWords(null, emptyList()), store.read())
    }

    @Test
    fun loadAnswersOnMain() {
        store.write(KeptWords("t", listOf("abh")))
        var loaded: KeptWords? = null
        store.load { loaded = it }
        assertEquals(listOf("abh"), loaded?.words)
    }
}
