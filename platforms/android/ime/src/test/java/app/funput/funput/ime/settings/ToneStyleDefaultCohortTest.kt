package app.funput.funput.ime.settings

import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ToneStyleDefaultCohortTest {
    @Test
    fun `a clean install receives the modern default`() {
        assertEquals(
            ToneStyleDefaultCohort.MODERN,
            ToneStyleDefaultCohort.resolve(hasPriorSettings = false, isUpgrade = false),
        )
    }

    @Test
    fun `an upgrade or restored settings retain the legacy default`() {
        assertEquals(
            ToneStyleDefaultCohort.LEGACY,
            ToneStyleDefaultCohort.resolve(hasPriorSettings = false, isUpgrade = true),
        )
        assertEquals(
            ToneStyleDefaultCohort.LEGACY,
            ToneStyleDefaultCohort.resolve(hasPriorSettings = true, isUpgrade = false),
        )
    }

    @Test
    fun `a recorded cohort reads back and an unrecorded one does not`() {
        assertEquals(ToneStyleDefaultCohort.MODERN, ToneStyleDefaultCohort.of("modern"))
        assertEquals(ToneStyleDefaultCohort.LEGACY, ToneStyleDefaultCohort.of("legacy"))
        assertNull(ToneStyleDefaultCohort.of(null))
        assertNull(ToneStyleDefaultCohort.of("neither"))
    }

    @Test
    fun `migration persists the fresh cohort exactly once`() = runBlocking {
        val migration = ToneStyleDefaultCohortMigration(isUpgrade = { false })
        val migrated = migration.migrate(emptyPreferences())

        assertEquals("modern", migrated[ToneStyleDefaultCohortKey])
        assertFalse(migration.shouldMigrate(migrated))
    }

    /**
     * The whole point of recording the answer: an upgrade lands later and would
     * otherwise flip a user this store already called new.
     */
    @Test
    fun `a recorded cohort survives a later upgrade`() = runBlocking {
        val migrated = ToneStyleDefaultCohortMigration(isUpgrade = { false })
            .migrate(emptyPreferences())

        assertFalse(ToneStyleDefaultCohortMigration(isUpgrade = { true }).shouldMigrate(migrated))
        assertEquals(ToneStyleDefaultCohort.MODERN, ToneStyleDefaultCohort.of(migrated[ToneStyleDefaultCohortKey]))
    }
}
