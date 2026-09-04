package app.funput.funput.ime.settings

import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ToneStyleDefaultCohortTest {
    @Test
    fun `a clean install receives the modern default`() {
        assertEquals(ToneStyleDefaultCohort.MODERN, resolve(hasPriorSettings = false, isUpgrade = false))
    }

    @Test
    fun `an upgrade or restored settings retain the legacy default`() {
        assertEquals(ToneStyleDefaultCohort.LEGACY, resolve(hasPriorSettings = false, isUpgrade = true))
        assertEquals(ToneStyleDefaultCohort.LEGACY, resolve(hasPriorSettings = true, isUpgrade = false))
    }

    @Test
    fun `a stored cohort remains stable across later upgrades`() {
        val modern = ToneStyleDefaultCohort.resolve("modern", hasPriorSettings = true, isUpgrade = true)
        val legacy = ToneStyleDefaultCohort.resolve("legacy", hasPriorSettings = false, isUpgrade = false)
        assertEquals(ToneStyleDefaultCohort.MODERN, modern)
        assertEquals(ToneStyleDefaultCohort.LEGACY, legacy)
    }

    @Test
    fun `migration persists the fresh cohort exactly once`() = runBlocking {
        val migration = ToneStyleDefaultCohortMigration(isUpgrade = { false })
        val migrated = migration.migrate(emptyPreferences())

        assertEquals("modern", migrated[ToneStyleDefaultCohortKey])
        assertFalse(migration.shouldMigrate(migrated))
    }

    private fun resolve(hasPriorSettings: Boolean, isUpgrade: Boolean) =
        ToneStyleDefaultCohort.resolve(null, hasPriorSettings, isUpgrade)
}
