package app.funput.funput.ime.suggestions

import androidx.test.platform.app.InstrumentationRegistry
import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.ime.editing.support.ImeEditingScenario
import app.funput.funput.ime.editing.support.onMainThread
import app.funput.funput.ime.editing.support.type
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.ime.suggestions.lexicon.LexiconInstaller
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class LexiconInputInstrumentedTest {
    @Test fun switchingLanguageClearsTrackedPrefix() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.type("wh")
            assertEquals("wh", scenario.handler.takeSuggestionUpdate().prefix)
            scenario.handler.onKeyAction(KeyAction.ToggleLanguage(KeyboardLanguage.ENGLISH))
            assertEquals("", scenario.handler.takeSuggestionUpdate().prefix)
            scenario.handler.type("ip")
            assertEquals("ip", scenario.handler.takeSuggestionUpdate().prefix)
        }
    }

    @Test fun bothLanguagesAcceptOneSpaceAndLearnExactlyOnce() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (language in KeyboardLanguage.entries) {
            val shown = LinkedBlockingQueue<List<String>>()
            val root = context.cacheDir.resolve("input-test-${System.nanoTime()}")
            lateinit var scenario: ImeEditingScenario
            lateinit var service: PersonalSuggestionService
            lateinit var worker: PersonalSuggestionWorker
            onMainThread {
                scenario = ImeEditingScenario.create()
                scenario.handler.onKeyAction(KeyAction.ToggleLanguage(language))
                service = PersonalSuggestionService(context, { shown.offer(it) }, acknowledgeReset = {},
                    createWorker = { _, publish ->
                        PersonalSuggestionWorker(storeDirectory = { root.resolve("personal") }, attachLexicon = {
                            check(LexiconInstaller({ context.assets.open("lexicon/en.lex") }, { root.resolve("lexicon") }).attach(it::attachLexicon))
                        }, publish = publish).also { worker = it }
                    })
                service.start(EditorInfoPolicy.Default)
                scenario.handler.type("wh")
                service.consume(scenario.handler.takeSuggestionUpdate())
            }
            try {
                val candidates = shown.poll(20, TimeUnit.SECONDS)
                assertEquals(listOf("which", "when", "what"), candidates)
                onMainThread {
                    assertTrue(service.select(SuggestionSelection(0, "which"), scenario.handler))
                    assertEquals("which ", scenario.text)
                    assertNull(scenario.handler.takeSuggestionUpdate().completedToken)
                }
                val closed = java.util.concurrent.CountDownLatch(1)
                worker.close { closed.countDown() }
                assertTrue(closed.await(20, TimeUnit.SECONDS))
                requireNotNull(PersonalSuggestionEngine.open(root.resolve("personal"))).use {
                    assertEquals(1L, it.stats().words)
                    assertEquals(0L, it.stats().promotedWords)
                }
            } finally {
                onMainThread { scenario.close() }
                root.deleteRecursively()
            }
        }
    }
}
