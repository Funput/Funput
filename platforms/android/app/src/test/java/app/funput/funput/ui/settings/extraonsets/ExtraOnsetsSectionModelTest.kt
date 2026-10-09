package app.funput.funput.ui.settings.extraonsets

import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetter
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetters
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetsStoring
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Failure, retry and pending-write behavior without optimistic selection changes. */
class ExtraOnsetsSectionModelTest {
    @Test fun `failed save keeps the committed selection and retry clears the error`() {
        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        try {
            val store = RecordingOnsetStore(ExtraOnsetLetters.parse("z"))
            val model = ExtraOnsetsSectionModel(store, scope)
            store.fails = true
            model.setEnabled(true)
            assertTrue(model.hasSaveError)
            assertFalse(model.isSaving)
            assertEquals("z", store.selection.configurationValue)
            store.fails = false
            model.setLetter(ExtraOnsetLetter.F, true)
            assertFalse(model.hasSaveError)
            assertEquals("zf", store.selection.configurationValue)
            model.setEnabled(false)
            assertEquals(ExtraOnsetLetters.None, store.selection)
        } finally {
            scope.cancel()
        }
    }

    @Test fun `pending writes disable reentry and do not change the committed selection`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        try {
            val gate = CompletableDeferred<Unit>()
            val store = RecordingOnsetStore().also { it.gate = gate }
            val model = ExtraOnsetsSectionModel(store, scope)
            model.setEnabled(true)
            assertTrue(model.isSaving)
            assertEquals(ExtraOnsetLetters.None, store.selection)
            model.setLetter(ExtraOnsetLetter.F, false)
            assertEquals(1, store.writes)
            gate.complete(Unit)
            assertFalse(model.isSaving)
            assertEquals(ExtraOnsetLetters.All, store.selection)
        } finally {
            scope.cancel()
        }
    }

    @Test fun `cancellation is not reported as a failed save`() {
        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val store = RecordingOnsetStore().also { it.gate = CompletableDeferred() }
        val model = ExtraOnsetsSectionModel(store, scope)
        model.setEnabled(true)
        assertTrue(model.isSaving)
        scope.cancel()
        assertFalse(model.isSaving)
        assertFalse(model.hasSaveError)
        assertEquals(ExtraOnsetLetters.None, store.selection)
    }
}

private class RecordingOnsetStore(var selection: ExtraOnsetLetters = ExtraOnsetLetters.None) : ExtraOnsetsStoring {
    var fails = false
    var writes = 0
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun setEnabled(enabled: Boolean) = write {
        if (enabled) ExtraOnsetLetters.All else ExtraOnsetLetters.None
    }

    override suspend fun setLetter(letter: ExtraOnsetLetter, enabled: Boolean) = write {
        selection.withLetter(letter, enabled)
    }

    private suspend fun write(value: () -> ExtraOnsetLetters) {
        writes++
        gate?.await()
        if (fails) throw IOException("storage unavailable")
        selection = value()
    }
}
