package app.funput.funput.ime.settings

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Which tone placement an install starts on before the user picks one.
 *
 * Modern placement became the product default in the release that added this type.
 * New installs get it; anyone already typing here keeps the placement their fingers
 * know, because a tone that moves mid-word during an upgrade reads as a broken
 * keyboard rather than a new default.
 */
internal enum class ToneStyleDefaultCohort(val persistedValue: String, val default: ToneStyle) {
    LEGACY("legacy", ToneStyle.TRADITIONAL),
    MODERN("modern", ToneStyle.MODERN),
    ;

    companion object {
        /** The recorded cohort, or `null` when storage has not answered. */
        fun of(value: String?): ToneStyleDefaultCohort? =
            entries.firstOrNull { it.persistedValue == value }

        /**
         * Decides the cohort from what the device can still tell us. Reserved for
         * [ToneStyleDefaultCohortMigration]: the answer is written down precisely so
         * that nothing else has to ask twice and risk a different answer later.
         */
        fun resolve(hasPriorSettings: Boolean, isUpgrade: Boolean): ToneStyleDefaultCohort =
            if (hasPriorSettings || isUpgrade) LEGACY else MODERN
    }
}

internal val ToneStyleDefaultCohortKey = stringPreferencesKey("tone_style_default_cohort")

internal class ToneStyleDefaultCohortMigration(
    private val isUpgrade: () -> Boolean,
) : DataMigration<Preferences> {
    constructor(context: Context) : this(context::isUpgradedInstallation)

    override suspend fun shouldMigrate(currentData: Preferences) =
        currentData[ToneStyleDefaultCohortKey] == null

    override suspend fun migrate(currentData: Preferences): Preferences {
        // DataStore runs migrations before it serves the first read or write, so this
        // sees the store exactly as an earlier release left it.
        val cohort = ToneStyleDefaultCohort.resolve(
            hasPriorSettings = currentData.asMap().isNotEmpty(),
            isUpgrade = isUpgrade(),
        )
        return currentData.toMutablePreferences().apply {
            this[ToneStyleDefaultCohortKey] = cohort.persistedValue
        }
    }

    override suspend fun cleanUp() = Unit
}

/**
 * True once this package has been updated at least once, which someone who installed
 * it today has not. It covers the user the settings store cannot: one who has typed
 * with Funput for releases without ever changing a setting, leaving nothing behind to
 * recognise them by.
 */
private fun Context.isUpgradedInstallation(): Boolean = runCatching {
    @Suppress("DEPRECATION")
    packageManager.getPackageInfo(packageName, 0).run { firstInstallTime < lastUpdateTime }
}.getOrDefault(false)
