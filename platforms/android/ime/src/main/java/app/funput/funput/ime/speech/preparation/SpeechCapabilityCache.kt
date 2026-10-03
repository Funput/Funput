package app.funput.funput.ime.speech.preparation

import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.platform.PreparationTime

/** RAM only; transient failures are not cached. Access is serialized by the owner. */
internal class SpeechCapabilityCache(private val time: PreparationTime) {
    private data class Entry(val value: SpeechCapability, val expiresAt: Long)
    private val revisions = mutableMapOf<SpeechLocale, Long>()
    private val entries = mutableMapOf<SpeechLocale, Entry>()

    fun get(locale: SpeechLocale): SpeechCapability? {
        val entry = entries[locale] ?: return null
        if (time.nowMillis() >= entry.expiresAt) {
            entries.remove(locale)
            return null
        }
        return entry.value
    }

    fun revision(locale: SpeechLocale): Long = revisions[locale] ?: 0

    fun put(locale: SpeechLocale, value: SpeechCapability, expectedRevision: Long) {
        if (expectedRevision != revision(locale)) return
        if (value != SpeechCapability.UNKNOWN) entries[locale] = Entry(value, time.nowMillis() + 300_000)
    }

    fun invalidate(locale: SpeechLocale?) {
        val affected = if (locale == null) SpeechLocale.entries else listOf(locale)
        affected.forEach {
            revisions[it] = revision(it) + 1
            entries.remove(it)
        }
    }
}
