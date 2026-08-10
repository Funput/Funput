package app.funput.funput.ime.clipboard.model

import java.time.Duration

/** Retention window for an unpinned clipboard entry. */
enum class ClipboardExpiry(
    internal val storageValue: String,
    /** Duration after which an unpinned entry is no longer returned. */
    val duration: Duration,
) {
    /** Retain unpinned entries for one hour. */
    HOUR("hour", Duration.ofHours(1)),

    /** Retain unpinned entries for one day. */
    DAY("day", Duration.ofDays(1)),

    /** Retain unpinned entries for one week. */
    WEEK("week", Duration.ofDays(7)),
    ;

    /** Defaults shared by Settings and the IME. */
    companion object {
        /** Privacy-first default used when no valid setting exists. */
        val Default: ClipboardExpiry = HOUR

        internal fun fromStorageValue(value: String?): ClipboardExpiry =
            entries.firstOrNull { it.storageValue == value } ?: Default
    }
}
