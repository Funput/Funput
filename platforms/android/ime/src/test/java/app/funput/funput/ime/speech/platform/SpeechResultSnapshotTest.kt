package app.funput.funput.ime.speech.platform

import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.ime.speech.model.SpeechLocale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechResultSnapshotTest {
    @Test
    fun `mutable recognition hypotheses cannot change emitted event`() {
        val hypotheses = mutableListOf("", "xin chào", "alternative")
        val partial = SpeechResultSnapshot.partial(hypotheses)
        val final = SpeechResultSnapshot.final(hypotheses)
        hypotheses[1] = "rewritten"
        hypotheses.clear()
        assertEquals(SpeechEvent.Partial("xin chào"), partial)
        assertEquals(SpeechEvent.Final("xin chào"), final)
    }

    @Test
    fun `empty partial clears preview but empty final is no match`() {
        assertEquals(SpeechEvent.Partial(""), SpeechResultSnapshot.partial(null))
        assertEquals(SpeechEvent.Failure(7), SpeechResultSnapshot.final(listOf("", "   ")))
    }

    @Test
    fun `snapshot retains Unicode and final whitespace for controller normalization`() {
        val text = "  Tiếng Việt 😀  "
        assertEquals(SpeechEvent.Final(text), SpeechResultSnapshot.final(listOf(text)))
    }

    @Test
    fun `both locales request partial on-device results explicitly`() {
        SpeechLocale.entries.forEach { locale ->
            val request = SpeechRequestFactory.create(locale)
            assertEquals(locale.tag, request.localeTag)
            assertTrue(request.partialResults)
            assertTrue(request.preferOffline)
            assertEquals(1, request.maxResults)
        }
        assertEquals("vi-VN", SpeechLocale.VI.tag)
        assertEquals("en-US", SpeechLocale.EN.tag)
    }
}
