package app.funput.funput.ime.settings.extraonsets

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Real Preferences DataStore writes, including concurrent edits to different letters. */
class ExtraOnsetsSettingsTest {
    @get:Rule val temporary = TemporaryFolder()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val settingsFile get() = File(temporary.root, "settings.preferences_pb")
    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(scope = scope) {
            settingsFile
        }
    }
    private val store by lazy { ExtraOnsetsSettings(dataStore) }

    @After fun closeStore() = runBlocking { job.cancelAndJoin() }

    @Test fun `master toggles write all or none and enabling again selects all`() = runBlocking {
        store.setEnabled(true)
        assertEquals("zfwj", dataStore.data.first()[ExtraOnsetsSettings.SelectionKey])
        store.setLetter(ExtraOnsetLetter.F, false)
        assertEquals("zwj", dataStore.data.first()[ExtraOnsetsSettings.SelectionKey])
        store.setEnabled(false)
        assertEquals("", dataStore.data.first()[ExtraOnsetsSettings.SelectionKey])
        store.setEnabled(true)
        assertEquals("zfwj", dataStore.data.first()[ExtraOnsetsSettings.SelectionKey])
    }

    @Test fun `parallel letter edits read the latest committed selection`() = runBlocking {
        val edits = ExtraOnsetLetter.Ordered.map { letter -> launch { store.setLetter(letter, true) } }
        edits.forEach { it.join() }
        assertEquals("zfwj", dataStore.data.first()[ExtraOnsetsSettings.SelectionKey])
        ExtraOnsetLetter.Ordered.forEach { store.setLetter(it, false) }
        assertEquals("", dataStore.data.first()[ExtraOnsetsSettings.SelectionKey])
    }

    @Test fun `writes canonicalize legacy values and preserve unrelated preferences`() = runBlocking {
        val unrelated = booleanPreferencesKey("unrelated")
        dataStore.edit {
            it[unrelated] = true
            it[ExtraOnsetsSettings.SelectionKey] = "J?ZFz"
        }
        store.setLetter(ExtraOnsetLetter.W, true)
        val snapshot = dataStore.data.first()
        assertEquals("zfwj", snapshot[ExtraOnsetsSettings.SelectionKey])
        assertTrue(snapshot[unrelated] == true)
    }

    @Test fun `selection survives closing and reopening the settings file`() = runBlocking {
        store.setLetter(ExtraOnsetLetter.J, true)
        store.setLetter(ExtraOnsetLetter.F, true)
        job.cancelAndJoin()
        val reopenedScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            val reopened = PreferenceDataStoreFactory.create(scope = reopenedScope) { settingsFile }
            assertEquals("fj", reopened.data.first()[ExtraOnsetsSettings.SelectionKey])
        } finally {
            reopenedScope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()
        }
    }

    @Test fun `a failed transaction never changes the previously committed selection`() {
        val previous = preferencesOf(ExtraOnsetsSettings.SelectionKey to "z")
        val failing = object : DataStore<Preferences> {
            override val data = flowOf(previous)
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                transform(previous)
                throw IOException("disk write failed")
            }
        }
        assertThrows(IOException::class.java) { runBlocking { ExtraOnsetsSettings(failing).setEnabled(true) } }
        assertEquals("z", previous[ExtraOnsetsSettings.SelectionKey])
    }
}
