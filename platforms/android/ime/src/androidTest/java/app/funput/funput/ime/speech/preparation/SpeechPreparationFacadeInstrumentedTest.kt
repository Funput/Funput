package app.funput.funput.ime.speech.preparation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.funput.funput.ime.speech.model.SpeechLocale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the public facade against the real provider without recording or downloading. */
@RunWith(AndroidJUnit4::class)
class SpeechPreparationFacadeInstrumentedTest {
    @Test fun checksBothLocalesWithBoundedOwnedOperations() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var service: SpeechPreparationService
        instrumentation.runOnMainSync {
            service = OnDeviceSpeechPreparationService(instrumentation.targetContext)
        }
        for (locale in SpeechLocale.entries) {
            val result = AtomicReference<SpeechCapability?>()
            val deliveries = AtomicInteger()
            val complete = CountDownLatch(1)
            lateinit var operation: SpeechPreparationOperation
            try {
                instrumentation.runOnMainSync {
                    operation = service.check(locale) {
                        deliveries.incrementAndGet()
                        result.set(it)
                        complete.countDown()
                    }
                    operation.start()
                }
                assertTrue(complete.await(10, TimeUnit.SECONDS))
                assertNotNull(result.get())
                assertEquals(1, deliveries.get())
                println("SpeechPreparation: locale=${locale.tag} capability=${result.get()}; no audio/download")
            } finally {
                instrumentation.runOnMainSync { operation.close() }
            }
        }
    }
}
