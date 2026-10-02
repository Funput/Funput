package app.funput.funput.ime.extraonsets

import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.funput.funput.ime.ImeSettingsController
import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.NativeVietnameseEngine
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetters
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetsSettings
import app.funput.funput.ime.settings.funputSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/** Real DataStore emissions reach the controller without restarting the IME service. */
@RunWith(AndroidJUnit4::class)
class ExtraOnsetsControllerInstrumentedTest {
    @Test fun controllerReceivesEveryPersistedSelection() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dataStore = context.funputSettingsStore
        val previous = dataStore.data.first()[ExtraOnsetsSettings.SelectionKey]
        val store = ExtraOnsetsSettings(context)
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Main.immediate + job)
        val engine = withContext(Dispatchers.Main.immediate) { ConfigurationRecordingEngine() }
        try {
            store.setEnabled(false)
            withContext(Dispatchers.Main.immediate) {
                ImeSettingsController(engine, {}, {}, {}).observe(context, scope)
            }
            listOf("zfwj", "z", "f", "w", "j", "").forEach { letters ->
                dataStore.edit { it[ExtraOnsetsSettings.SelectionKey] = letters }
                val selection = ExtraOnsetLetters.parse(letters)
                val applied = withTimeout(5_000) { engine.configurations.first { it?.extraOnsets == selection } }
                assertEquals(selection, applied?.extraOnsets)
                assertFalse(requireNotNull(applied).autoCapitalize)
            }
        } finally {
            withContext(NonCancellable) {
                job.cancelAndJoin()
                withContext(Dispatchers.Main.immediate) { engine.close() }
                dataStore.edit { values ->
                    if (previous == null) values.remove(ExtraOnsetsSettings.SelectionKey)
                    else values[ExtraOnsetsSettings.SelectionKey] = previous
                }
            }
        }
    }
}

private class ConfigurationRecordingEngine(
    private val native: NativeVietnameseEngine = NativeVietnameseEngine(),
) : VietnameseEngine by native {
    val configurations = MutableStateFlow<EngineConfiguration?>(null)

    override fun configure(configuration: EngineConfiguration) {
        native.configure(configuration)
        configurations.value = configuration
    }
}
