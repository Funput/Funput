package app.funput.funput.ime.speech.integration.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ImeSpeechSettingsBindingTest {
    @Test fun disablingIsImmediateAndClosingTheOwnerStopsDelivery() = runBlocking {
        val enabled = MutableSharedFlow<Boolean>(replay = 1)
        val applied = mutableListOf<Boolean>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            enabled.emit(true)
            ImeSpeechSettingsBinding(enabled) { applied += it }.observe(scope)
            enabled.emit(false)
            enabled.emit(false)
            enabled.emit(true)
            assertEquals(listOf(true, false, true), applied)
            scope.cancel()
            enabled.emit(false)
            assertEquals(listOf(true, false, true), applied)
        } finally {
            scope.cancel()
        }
    }
}
