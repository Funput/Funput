package app.funput.funput.ime.suggestions

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.lifecycle.createImeEditingSession
import app.funput.funput.keyboard.model.ShiftState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The real session factory, not a hand-built one: the document's composition must drive
 * the composer through [SeedLanguageReportingEngine], or the shipped suggestion data stops
 * following the language key and the tone style without any test noticing.
 */
class SeedLanguageWiringInstrumentedTest {
    @Test fun theDocumentsCompositionGoesThroughTheReportingEngine() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            val session = createImeEditingSession(
                context, scope, InputConnectionEditor(), { null },
                { ShiftState.OFF }, {}, {}, {},
            )
            try {
                assertTrue(session.composition.engine is SeedLanguageReportingEngine)
            } finally {
                session.close()
                session.closeEngines()
                scope.cancel()
            }
        }
    }
}
