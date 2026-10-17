package app.funput.funput.ime.suggestions.correction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Executor

class CorrectionWordSourceTest {
    private class FakeLexicon(var known: Set<String>? = emptySet()) : CorrectionLexicon {
        override fun isKnown(word: String): Boolean? = known?.contains(word)
    }

    private val immediate = Executor(Runnable::run)
    private val counts = CorrectionWordCounts { word -> if (word == "đường") 4 else 0 }

    private fun store(directory: File = Files.createTempDirectory("kept").toFile()) =
        TypoKeptWordsStore(File(directory, "kept-words.txt"), immediate, immediate)

    @Test
    fun keeps() {
        val source = CorrectionWordSource(FakeLexicon(), counts)
        assertFalse(source.recognizes("Dduwowfnh"))
        source.keep("Dduwowfnh")
        assertTrue(source.recognizes("dduwowfnh"))
    }

    @Test
    fun capped() {
        val source = CorrectionWordSource(FakeLexicon(), counts)
        repeat(CorrectionWordSource.KeptLimit + 1) { source.keep("w$it") }
        assertFalse("the oldest word gives way", source.recognizes("w0"))
        assertTrue(source.recognizes("w${CorrectionWordSource.KeptLimit}"))
    }

    @Test
    fun restored() {
        val directory = Files.createTempDirectory("kept").toFile()
        CorrectionWordSource(FakeLexicon(), counts, store(directory)).keep("abh")
        val next = CorrectionWordSource(FakeLexicon(), CorrectionWordCounts { 0 }, store(directory))
        assertTrue(next.recognizes("abh"))
    }

    @Test
    fun notPersistedWhereLearningIsOff() {
        val directory = Files.createTempDirectory("kept").toFile()
        CorrectionWordSource(FakeLexicon(), counts, store(directory), persists = { false }).keep("abh")
        val next = CorrectionWordSource(FakeLexicon(), CorrectionWordCounts { 0 }, store(directory))
        assertFalse(next.recognizes("abh"))
    }

    @Test
    fun resetTokenClears() {
        val directory = Files.createTempDirectory("kept").toFile()
        CorrectionWordSource(FakeLexicon(), counts, store(directory)).apply {
            applyResetToken("first")
            keep("abh")
        }
        val next = CorrectionWordSource(FakeLexicon(), counts, store(directory))
        next.applyResetToken("first")
        assertTrue("the same token is not a new reset", next.recognizes("abh"))
        next.applyResetToken("second")
        assertFalse(next.recognizes("abh"))
    }

    @Test
    fun counts() {
        val source = CorrectionWordSource(FakeLexicon(), counts)
        source.learn("Mk")
        assertFalse("once could itself be the slip", source.recognizes("mk"))
        source.learn("mk")
        assertTrue(source.recognizes("mk"))
    }

    @Test
    fun shippedWeight() {
        val source = CorrectionWordSource(FakeLexicon(), counts)
        assertEquals(4, source.weight("đường"))
        source.learn("đường")
        assertEquals(5, source.weight("đường"))
    }

    @Test
    fun cappedCounts() {
        val limited = CorrectionWordCounts { 0 }
        limited.learn("often")
        limited.learn("often")
        repeat(CorrectionWordCounts.CountLimit) { limited.learn("once$it") }
        assertTrue("the rarest word gives way, not the frequent one", limited.isFamiliar("often"))
    }

    @Test
    fun recognizesWhileLoading() {
        val source = CorrectionWordSource(FakeLexicon(known = null), counts)
        assertTrue(source.recognizes("dduwowfnh"))
    }

    @Test
    fun shippedListsVeto() {
        val source = CorrectionWordSource(FakeLexicon(known = setOf("ok")), counts)
        assertTrue(source.recognizes("ok"))
        assertFalse(source.recognizes("abh"))
    }
}
