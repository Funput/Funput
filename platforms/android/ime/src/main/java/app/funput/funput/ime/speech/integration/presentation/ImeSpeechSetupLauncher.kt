package app.funput.funput.ime.speech.integration.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.navigation.SpeechSetupReason
import app.funput.funput.ime.speech.preparation.navigation.SpeechSetupRequest

internal object ImeSpeechSetupLauncher {
    fun open(context: Context, locale: SpeechLocale, permitted: Boolean): Boolean = try {
        val reason = if (permitted) SpeechSetupReason.MODEL else SpeechSetupReason.PERMISSION
        context.startActivity(SpeechSetupRequest(reason, locale).intent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
