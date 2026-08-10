package app.funput.funput.ime.clipboard.model

import java.time.Instant
import java.util.UUID

/**
 * One exact text value explicitly pasted through Funput.
 *
 * @property id Stable identity used by pin and delete operations.
 * @property text Unmodified clipboard text, including whitespace and Unicode.
 * @property capturedAt Time at which Funput accepted the paste.
 * @property isPinned Whether the entry is exempt from expiry and normal eviction.
 * @property sourceToken Opaque identity of the Android clipboard generation.
 */
data class ClipboardEntry(
    val id: UUID = UUID.randomUUID(),
    val text: String,
    val capturedAt: Instant = Instant.now(),
    val isPinned: Boolean = false,
    val sourceToken: String,
) {
    init {
        require(text.isNotEmpty()) { "Clipboard text must not be empty" }
        require(sourceToken.isNotBlank()) { "Clipboard source token must not be blank" }
    }
}
