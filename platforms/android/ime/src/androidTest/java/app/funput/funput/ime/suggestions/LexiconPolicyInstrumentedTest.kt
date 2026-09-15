package app.funput.funput.ime.suggestions

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.test.platform.app.InstrumentationRegistry
import app.funput.funput.ime.editing.AuthoredSuggestionUpdate
import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.ime.editing.EditorInfoPolicyResolver
import app.funput.funput.ime.editing.ImeSuggestionSource
import app.funput.funput.ime.editing.support.onMainThread
import app.funput.funput.ime.settings.PersonalSuggestionPreferences
import app.funput.funput.keyboard.ui.KeyboardPanel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class LexiconPolicyInstrumentedTest {
    @Test fun staleResultsAreRejectedAfterPanelEditorPreferenceAndSessionChanges() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pending = LinkedBlockingQueue<() -> Unit>()
        val visible = mutableListOf<List<String>>()
        lateinit var worker: PersonalSuggestionWorker
        lateinit var service: PersonalSuggestionService
        onMainThread {
            service = PersonalSuggestionService(context, { visible.add(it) }, acknowledgeReset = {},
                createWorker = { _, publish ->
                    PersonalSuggestionWorker(storeDirectory = { error("memory") },
                        attachLexicon = { lexiconInstaller(context).attach(it::attachLexicon) },
                        publish = { request, values -> pending.offer { publish(request, values) } })
                        .also { worker = it }
                })
        }
        val changes: List<() -> Unit> = listOf(
            { service.updatePanel(KeyboardPanel.SYMBOLS) },
            { service.configure(PersonalSuggestionPreferences.Default.copy(enabled = false)) },
            { service.start(EditorInfoPolicy.Default.copy(suggestionSource = ImeSuggestionSource.EDITOR)) },
            { service.start(EditorInfoPolicy.Default.copy(allowsPersonalSuggestions = false)) },
            { service.finish() },
            { service.start(EditorInfoPolicy.Default) },
            { service.consume(AuthoredSuggestionUpdate("i", null)) },
            { service.consume(AuthoredSuggestionUpdate.Empty) },
        )
        try {
            for (change in changes) {
                onMainThread {
                    service.configure(PersonalSuggestionPreferences.Default)
                    service.start(EditorInfoPolicy.Default)
                    service.consume(AuthoredSuggestionUpdate("wh", null))
                }
                val deliver = requireNotNull(pending.poll(20, TimeUnit.SECONDS))
                onMainThread { change(); deliver(); assertTrue(visible.isEmpty()) }
            }
        } finally {
            val closed = CountDownLatch(1)
            worker.close { closed.countDown() }
            assertTrue(closed.await(20, TimeUnit.SECONDS))
        }
    }

    @Test fun noPersonalizedLearningStillSuggestsButPasswordDoesNotLearn() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = context.cacheDir.resolve("policy-test-${System.nanoTime()}")
        val results = LinkedBlockingQueue<List<String>>()
        lateinit var service: PersonalSuggestionService
        lateinit var worker: PersonalSuggestionWorker
        onMainThread {
            service = PersonalSuggestionService(context, { results.offer(it) }, acknowledgeReset = {},
                createWorker = { _, publish ->
                    PersonalSuggestionWorker(storeDirectory = { root },
                        attachLexicon = { lexiconInstaller(context).attach(it::attachLexicon) }, publish = publish)
                        .also { worker = it }
                })
            val policy = EditorInfoPolicyResolver.resolve(InputType.TYPE_CLASS_TEXT,
                EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)
            service.start(policy)
            service.consume(AuthoredSuggestionUpdate("wh", "privateword"))
        }
        try {
            assertEquals(listOf("which", "when", "what"), results.poll(20, TimeUnit.SECONDS))
            onMainThread {
                val password = EditorInfoPolicyResolver.resolve(
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, 0)
                assertFalse(password.allowsPersonalSuggestions)
                service.start(password)
                service.consume(AuthoredSuggestionUpdate("wh", "password"))
            }
            val closed = CountDownLatch(1)
            worker.close { closed.countDown() }
            assertTrue(closed.await(20, TimeUnit.SECONDS))
            requireNotNull(PersonalSuggestionEngine.open(root)).use { assertEquals(0L, it.stats().words) }
        } finally { root.deleteRecursively() }
    }
}
