package app.funput.funput.ime.speech.preparation.navigation

import android.content.Context
import android.content.Intent
import app.funput.funput.ime.speech.model.SpeechLocale

enum class SpeechSetupReason { PERMISSION, MODEL }

data class SpeechSetupRequest(val reason: SpeechSetupReason, val locale: SpeechLocale?) {
    fun intent(context: Context): Intent = Intent().setClassName(context.packageName,
        "${context.packageName}.speech.SpeechSetupActivity")
        .putExtra(ReasonKey, reason.name)
        .putExtra(LocaleKey, locale?.name)

    companion object {
        private const val ReasonKey = "speech_setup_reason"
        private const val LocaleKey = "speech_setup_locale"
        fun read(intent: Intent): SpeechSetupRequest = SpeechSetupRequest(
            SpeechSetupReason.entries.firstOrNull { it.name == intent.getStringExtra(ReasonKey) }
                ?: SpeechSetupReason.PERMISSION,
            SpeechLocale.entries.firstOrNull { it.name == intent.getStringExtra(LocaleKey) },
        )
    }
}
