package app.funput.funput.ime.suggestions

import androidx.test.platform.app.InstrumentationRegistry
import app.funput.funput.ime.suggestions.lexicon.LexiconInstaller
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class LexiconEngineInstrumentedTest {
    @Test fun actualAssetCrossesJniAndPreservesLexiconAcrossFailureAndReset() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "lexicon-test-${System.nanoTime()}")
        try {
            requireNotNull(PersonalSuggestionEngine.inMemory()).use { engine ->
                assertTrue(LexiconInstaller({ context.assets.open("lexicon/en.lex") }, { directory })
                    .attach(engine::attachLexicon))
                assertEquals(listOf("which", "when", "what"), engine.query("wh"))
                assertEquals(emptyList<String>(), engine.query(""))
                assertTrue(engine.query("ip").contains("iPhone"))
                repeat(2) { engine.learn("whizzbang"); engine.learn("WHICH") }
                assertEquals(setOf("whizzbang", "which"), engine.query("wh").take(2).toSet())
                assertEquals(1, engine.query("wh").count { it.equals("which", true) })
                assertFalse(engine.attachLexicon(File(directory, "missing")))
                val broken = File(directory, "broken").apply { writeText("invalid") }
                assertFalse(engine.attachLexicon(broken))
                assertTrue(engine.reset())
                assertEquals(listOf("which", "when", "what"), engine.query("wh"))
                repeat(2) { engine.learn("ăn") }
                assertEquals(3, engine.query("an").size)
                repeat(199) { index ->
                    val token = "từ" + ('a' + index / 26) + ('a' + index % 26)
                    repeat(2) { engine.learn(token) }
                }
                assertEquals(listOf("ăn"), engine.query("an"))
                assertEquals(3, engine.query("wh").size)
            }
        } finally { directory.deleteRecursively() }
    }
}
