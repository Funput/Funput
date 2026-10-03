package app.funput.funput.ime.speech.preparation

import android.os.Build
import android.os.Bundle
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.platform.SpeechRequestFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Test
import org.junit.runner.RunWith

/** Read-only P0 diagnostic: checks models but never starts listening or downloads them. */
@RunWith(AndroidJUnit4::class)
class OnDeviceSpeechProbeInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test
    fun reportOnDeviceAvailabilityAndVietnameseEnglishModels() {
        report("device=${Build.MODEL} api=${Build.VERSION.SDK_INT}; no audio captured")
        if (Build.VERSION.SDK_INT < 31) {
            report("availability=unsupported_os")
            return
        }
        var available = false
        instrumentation.runOnMainSync {
            available = SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        }
        report("availability=$available")
        if (!available) return
        if (Build.VERSION.SDK_INT < 33) {
            report("models=unknown; checkRecognitionSupport requires API 33")
            return
        }
        probe(SpeechLocale.VI)
        probe(SpeechLocale.EN)
    }

    @RequiresApi(33)
    private fun probe(locale: SpeechLocale) {
        val completed = CountDownLatch(1)
        val active = AtomicBoolean(true)
        val result = AtomicReference("timeout_after_10s")
        var recognizer: SpeechRecognizer? = null
        fun complete(message: String) {
            if (!active.compareAndSet(true, false)) return
            result.set(message)
            completed.countDown()
        }
        try {
            instrumentation.runOnMainSync {
                try {
                    recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    val request = SpeechRequestFactory.toIntent(SpeechRequestFactory.create(locale))
                    recognizer.checkRecognitionSupport(request, context.mainExecutor,
                        object : RecognitionSupportCallback {
                            override fun onSupportResult(support: RecognitionSupport) {
                                complete("installed=${support.installedOnDeviceLanguages.sorted()} " +
                                    "pending=${support.pendingOnDeviceLanguages.sorted()} " +
                                    "supported=${support.supportedOnDeviceLanguages.sorted()}")
                            }

                            override fun onError(error: Int) = complete("error_code=$error")
                        },
                    )
                } catch (error: Exception) {
                    complete("exception=${error.javaClass.simpleName}")
                }
            }
            completed.await(10, TimeUnit.SECONDS)
            report("locale=${locale.tag} ${result.get()}")
        } finally {
            active.set(false)
            instrumentation.runOnMainSync { recognizer?.destroy() }
            report("locale=${locale.tag} temporary_client_closed=true")
        }
    }

    private fun report(message: String) {
        val line = "SpeechProbe: $message\n"
        println(line)
        instrumentation.sendStatus(0, Bundle().apply { putString("stream", line) })
    }
}
