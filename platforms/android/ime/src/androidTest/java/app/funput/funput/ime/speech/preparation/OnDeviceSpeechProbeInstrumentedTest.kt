package app.funput.funput.ime.speech.preparation

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.funput.funput.ime.speech.mlkit.MlKitSpeech
import app.funput.funput.ime.speech.model.SpeechLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith

/** Read-only diagnostic: reports ML Kit model status but never listens or downloads. */
@RunWith(AndroidJUnit4::class)
class OnDeviceSpeechProbeInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test
    fun reportMlKitStatusForVietnameseAndEnglish() {
        report("device=${Build.MANUFACTURER} ${Build.MODEL} api=${Build.VERSION.SDK_INT}; no audio captured")
        listOf("com.google.android.tts", "com.google.android.aicore", "com.google.android.as",
            "com.google.android.gms").forEach { report("package $it=${versionOf(it)}") }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            report("availability=unsupported_os")
            return
        }
        SpeechLocale.entries.forEach(::probe)
    }

    private fun probe(locale: SpeechLocale) {
        val result = runBlocking {
            withContext(Dispatchers.Main) {
                val recognizer = MlKitSpeech.recognizer(locale.tag)
                try {
                    withTimeout(10_000) { "status=${statusName(recognizer.checkStatus())}" }
                } catch (error: Exception) {
                    "error=${error.javaClass.simpleName}:${error.message} code=${MlKitSpeech.errorCode(error)}"
                } finally {
                    recognizer.close()
                }
            }
        }
        report("locale=${locale.tag} $result")
    }

    private fun statusName(status: Int) = when (status) {
        0 -> "UNAVAILABLE"
        1 -> "DOWNLOADABLE"
        2 -> "DOWNLOADING"
        3 -> "AVAILABLE"
        else -> "UNKNOWN($status)"
    }

    private fun versionOf(name: String) = try {
        context.packageManager.getPackageInfo(name, 0).versionName ?: "installed"
    } catch (_: PackageManager.NameNotFoundException) {
        "missing"
    }

    private fun report(message: String) {
        val line = "SpeechProbe: $message\n"
        println(line)
        instrumentation.sendStatus(0, Bundle().apply { putString("stream", line) })
    }
}
