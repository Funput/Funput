package app.funput.funput.ui.settings.speech.model

import app.funput.funput.ime.speech.preparation.SpeechCapability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class SpeechSetupModelTest {
    @Test fun productionGatePreventsPreparationAndPreferenceWrites() = withModel(false) { model, service, enabled ->
        model.resume(false)
        model.setEnabled(false)
        assertEquals(0, service.probes)
        assertTrue(service.checks.isEmpty())
        assertTrue(enabled.value)
    }
    @Test fun deniedAndGrantedPermissionNeverStartDownloadsOrRecording() = withModel { model, service, _ ->
        model.resume(false)
        assertFalse(model.state.permissionGranted)
        assertEquals(2, service.checks.size)
        model.permissionResult(true)
        assertTrue(model.state.permissionGranted)
        assertEquals(2, service.checks.size)
        assertTrue(service.downloads.isEmpty())
    }
    @Test fun duplicateResumeKeepsOperationsUntilAnActualPause() = withModel { model, service, _ ->
        model.resume(false)
        val first = service.checks.toList()
        model.resume(true)
        assertTrue(model.state.permissionGranted)
        assertEquals(1, service.probes)
        assertEquals(first, service.checks)
        model.pause()
        model.resume(true)
        assertEquals(2, service.probes)
        assertEquals(4, service.checks.size)
    }
    @Test fun disablingPersistsAndOwnerCloseStopsCallbacksAndFlow() = withModel { model, service, enabled ->
        model.resume(true)
        model.setEnabled(false)
        assertFalse(enabled.value)
        assertFalse(model.state.enabled)
        assertFalse(model.state.saving)
        model.close()
        service.checks.forEach { it.emit(SpeechCapability.READY) }
        enabled.value = true
        assertFalse(model.state.enabled)
        assertTrue(model.state.locales.values.all { it.checking })
        model.resume(true)
        assertEquals(2, service.checks.size)
    }
    @Test fun saveFailureIsVisibleAndDoesNotChangePreference() {
        val service = FakeSpeechPreparation()
        val enabled = MutableStateFlow(true)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = SpeechSetupModel(service, enabled, { error("save") }, scope, true)
        try {
            model.setEnabled(false)
            assertTrue(model.state.saveError)
            assertTrue(model.state.enabled)
            assertFalse(model.state.saving)
        } finally { model.close(); scope.cancel() }
    }
    private fun withModel(feature: Boolean = true,
        assertions: (SpeechSetupModel, FakeSpeechPreparation, MutableStateFlow<Boolean>) -> Unit) {
        val service = FakeSpeechPreparation()
        val enabled = MutableStateFlow(true)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = SpeechSetupModel(service, enabled, { enabled.value = it }, scope, feature)
        try { assertions(model, service, enabled) } finally { model.close(); scope.cancel() }
    }
}
