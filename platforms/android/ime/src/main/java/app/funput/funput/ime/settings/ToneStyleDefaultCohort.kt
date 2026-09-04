package app.funput.funput.ime.settings

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

internal enum class ToneStyleDefaultCohort(val persistedValue: String, val default: ToneStyle) {
    LEGACY("legacy", ToneStyle.TRADITIONAL),
    MODERN("modern", ToneStyle.MODERN),
    ;

    companion object {
        fun resolve(
            value: String?,
            hasPriorSettings: Boolean,
            isUpgrade: Boolean,
        ): ToneStyleDefaultCohort {
            entries.firstOrNull { it.persistedValue == value }?.let { return it }
            return if (hasPriorSettings || isUpgrade) LEGACY else MODERN
        }
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
        val cohort = ToneStyleDefaultCohort.resolve(
            value = currentData[ToneStyleDefaultCohortKey],
            hasPriorSettings = currentData.asMap().isNotEmpty(),
            isUpgrade = isUpgrade(),
        )
        return currentData.toMutablePreferences().apply {
            this[ToneStyleDefaultCohortKey] = cohort.persistedValue
        }
    }

    override suspend fun cleanUp() = Unit
}

private fun Context.isUpgradedInstallation(): Boolean = runCatching {
    @Suppress("DEPRECATION")
    packageManager.getPackageInfo(packageName, 0).run { firstInstallTime < lastUpdateTime }
}.getOrDefault(false)
