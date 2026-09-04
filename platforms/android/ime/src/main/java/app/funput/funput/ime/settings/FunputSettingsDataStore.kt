package app.funput.funput.ime.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.funputSettingsStore by preferencesDataStore(
    name = "funput_settings",
    produceMigrations = { context -> listOf(ToneStyleDefaultCohortMigration(context)) },
)
