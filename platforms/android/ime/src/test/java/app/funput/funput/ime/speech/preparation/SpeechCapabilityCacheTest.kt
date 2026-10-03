package app.funput.funput.ime.speech.preparation

import app.funput.funput.ime.speech.model.SpeechLocale
import org.junit.Assert.*
import org.junit.Test

class SpeechCapabilityCacheTest {
    @Test fun sameLocaleUsesCacheUntilExactFiveMinuteBoundary() {
        val f = PreparationFixture()
        f.service.check(SpeechLocale.VI) {}.start()
        f.client.languages(installed = listOf("vi-VN"))
        f.time.advance(299_999)
        f.service.check(SpeechLocale.VI) { assertEquals(SpeechCapability.READY, it) }.start()
        assertEquals(1, f.factory.clients.size)
        f.time.advance(1)
        f.service.check(SpeechLocale.VI) {}.start()
        assertEquals(2, f.factory.clients.size)
    }
    @Test fun localeEntriesAreIndependentAndExplicitInvalidationForcesClient() {
        val f = PreparationFixture()
        f.service.check(SpeechLocale.VI) {}.start()
        f.client.languages(installed = listOf("vi-VN"))
        f.service.check(SpeechLocale.EN) {}.start()
        f.client.languages(supported = listOf("en-US"))
        f.service.invalidate(SpeechLocale.VI)
        f.service.check(SpeechLocale.EN) { assertEquals(SpeechCapability.DOWNLOADABLE, it) }.start()
        assertEquals(2, f.factory.clients.size)
        f.service.check(SpeechLocale.VI) {}.start()
        assertEquals(3, f.factory.clients.size)
    }
    @Test fun transientUnknownIsNotCached() {
        val f = PreparationFixture()
        f.service.check(SpeechLocale.VI) {}.start()
        f.time.advance(3_000)
        f.service.check(SpeechLocale.VI) {}.start()
        assertEquals(2, f.factory.clients.size)
    }
    @Test fun invalidatingWhileCheckingPreventsStaleResponseRepopulatingCache() {
        val f = PreparationFixture()
        f.service.check(SpeechLocale.VI) {}.start()
        f.service.invalidate()
        f.client.languages(installed = listOf("vi-VN"))
        f.service.check(SpeechLocale.VI) {}.start()
        assertEquals(2, f.factory.clients.size)
    }
    @Test fun availabilityIsRecheckedEvenWithReadyCache() {
        val f = PreparationFixture()
        f.service.check(SpeechLocale.VI) {}.start()
        f.client.languages(installed = listOf("vi-VN"))
        f.factory.available = false
        f.service.check(SpeechLocale.VI) { assertEquals(SpeechCapability.UNSUPPORTED, it) }.start()
        assertEquals(1, f.factory.clients.size)
    }
}
