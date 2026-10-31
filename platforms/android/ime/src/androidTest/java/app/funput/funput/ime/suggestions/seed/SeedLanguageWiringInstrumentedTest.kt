package app.funput.funput.ime.suggestions

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.lifecycle.createImeEditingSession
import app.funput.funput.ime.lifecycle.createServiceSettings
import app.funput.funput.ime.settings.ToneStyleSettings
import app.funput.funput.keyboard.model.ShiftState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The real factories, not hand-built ones: both the document's composition (VI/EN) and the
 * settings controller (tone style) must drive the composer through
 * [SeedLanguageReportingEngine], or the shipped suggestion data stops following one of them
 * without any other test noticing.
 */
class SeedLanguageWiringInstrumentedTest {
    @Test fun theCompositionAndTheSettingsBothGoThroughTheReportingEngine() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            val session = createImeEditingSession(
                context, scope, InputConnectionEditor(), { null },
                { ShiftState.OFF }, {}, {}, {},
            )
            try {
                val composer = session.composer
                assertTrue(composer is SeedLanguageReportingEngine)
                // The settings controller configures the engine as it is built: that is the
                // tone style half. The VI/EN half is what a field start would send.
                createServiceSettings(session, { null }, {}, {})
                composer.setEnabled(true)
                assertEquals(
                    SeedLanguage.Vietnamese(ToneStyleSettings.FallbackToneStyle),
                    (composer as SeedLanguageReportingEngine).reported,
                )
            } finally {
                session.close()
                session.closeEngines()
                scope.cancel()
            }
        }
    }
}
