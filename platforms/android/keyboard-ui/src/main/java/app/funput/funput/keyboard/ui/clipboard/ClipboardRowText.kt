package app.funput.funput.keyboard.ui.clipboard

import java.time.Duration
import java.time.Instant

internal object ClipboardRowText {
    const val PreviewLimit = 120

    fun preview(text: String): String {
        val collapsed = text.trim().replace(Whitespace, " ")
        val count = collapsed.codePointCount(0, collapsed.length)
        if (count <= PreviewLimit) return collapsed
        val end = collapsed.offsetByCodePoints(0, PreviewLimit)
        return collapsed.substring(0, end) + "…"
    }

    fun relativeTime(
        capturedAt: Instant,
        now: Instant,
        strings: ClipboardTimeStrings,
    ): String {
        val seconds = Duration.between(capturedAt, now).seconds.coerceAtLeast(0)
        if (seconds < 60) return strings.justNow
        val minutes = seconds / 60
        if (minutes < 60) return strings.minutesAgo(minutes)
        val hours = minutes / 60
        return if (hours < 24) strings.hoursAgo(hours) else strings.daysAgo(hours / 24)
    }

    private val Whitespace = Regex("\\s+")
}

internal data class ClipboardTimeStrings(
    val justNow: String,
    val minutesAgo: (Long) -> String,
    val hoursAgo: (Long) -> String,
    val daysAgo: (Long) -> String,
)
