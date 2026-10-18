package app.funput.funput.ime.speech.services

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.speech.RecognizerIntent
import app.funput.funput.ime.speech.model.SpeechLocale

/**
 * The Google app behind ML Kit speech (Basic mode). ML Kit only talks to it over Binder: the app
 * downloads the language pack and runs recognition. Its Play name is now "Speech Recognition &
 * Synthesis from Google", formerly "Speech Services by Google"; only the package id is stable.
 */
object GoogleSpeechServices {
    const val PACKAGE = "com.google.android.tts"
    private const val PLAY_STORE = "com.android.vending"

    /**
     * Speech Services asks before it downloads a pack, from its own (background) process. ROMs
     * such as HyperOS and ColorOS block that launch, the download is only scheduled, and nothing
     * ever asks the user. The activity is exported, so the foreground settings screen opens it.
     */
    private const val ACTION_DOWNLOAD_LANGUAGE = "com.google.recognition.action.DOWNLOAD_LANGUAGE"

    enum class Status { INSTALLED, DISABLED, MISSING }

    fun status(context: Context): Status = try {
        val info = context.packageManager.getApplicationInfo(PACKAGE, 0)
        if (info.enabled) Status.INSTALLED else Status.DISABLED
    } catch (_: PackageManager.NameNotFoundException) {
        Status.MISSING
    }

    fun hasPlayStore(context: Context): Boolean = try {
        context.packageManager.getApplicationInfo(PLAY_STORE, 0).enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun consentIntent(locale: SpeechLocale): Intent = Intent(ACTION_DOWNLOAD_LANGUAGE)
        .setPackage(PACKAGE)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.tag)

    /** The Play app first, then the web page for devices whose Play app cannot take the link. */
    fun playStoreIntents(): List<Intent> = listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PACKAGE")).setPackage(PLAY_STORE),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$PACKAGE")),
    )

    /** System app info, where the user enables the app and its pop-up permission. */
    fun appSettingsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", PACKAGE, null))
}
