package app.funput.funput.ime.settings.entersend

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Real Preferences DataStore writes, including concurrent edits from Settings. */
class EnterToSendSettingsTest {
    @get:Rule val temporary = TemporaryFolder()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(scope = scope) { File(temporary.root, "settings.preferences_pb") }
    }
    private val store by lazy { EnterToSendSettings(dataStore) }

    @After fun closeStore() = runBlocking { job.cancelAndJoin() }

    @Test fun `no app is chosen until one is added`() = runBlocking {
        assertEquals(emptySet<String>(), store.packages.first())
    }

    @Test fun `apps are added and removed one at a time`() = runBlocking {
        store.add("com.facebook.orca")
        store.add("org.telegram.messenger")
        store.add("com.facebook.orca")
        assertEquals(setOf("com.facebook.orca", "org.telegram.messenger"), store.packages.first())

        store.remove("com.facebook.orca")
        assertEquals(setOf("org.telegram.messenger"), store.packages.first())
    }

    @Test fun `parallel adds read the latest list`() = runBlocking {
        val packages = (1..8).map { "com.example.app$it" }
        packages.map { launch { store.add(it) } }.forEach { it.join() }
        assertEquals(packages.toSet(), store.packages.first())
    }

    @Test fun `invalid names are never stored and stored junk is skipped`() = runBlocking {
        store.add("not a package")
        store.add("")
        assertEquals(emptySet<String>(), store.packages.first())

        dataStore.edit { it[EnterToSendSettings.PackagesKey] = "com.facebook.orca\n\n../etc\nnoDots" }
        assertEquals(setOf("com.facebook.orca"), store.packages.first())
    }

    @Test fun `the codec writes a sorted canonical list`() {
        assertEquals("a.b\nz.y", EnterToSendSettingCodec.encode(setOf("z.y", "a.b", "bad")))
        assertTrue(EnterToSendSettingCodec.isPackageName("com.facebook.orca"))
        assertFalse(EnterToSendSettingCodec.isPackageName("com.facebook."))
        assertFalse(EnterToSendSettingCodec.isPackageName("1com.example"))
    }
}
