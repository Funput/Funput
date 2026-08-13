package app.funput.funput.ime.clipboard.persistence

import app.funput.funput.ime.clipboard.model.ClipboardEntry
import java.nio.file.Files
import java.time.Instant
import java.util.UUID

internal val ClipboardEpoch: Instant = Instant.ofEpochMilli(1_800_000_000_000)

internal fun clipboardEntry(
    text: String,
    capturedAt: Instant = ClipboardEpoch,
    pinned: Boolean = false,
    sourceToken: String = "source-${UUID.randomUUID()}",
) = ClipboardEntry(
    text = text,
    capturedAt = capturedAt,
    isPinned = pinned,
    sourceToken = sourceToken,
)

internal fun temporaryClipboardDirectory() = Files.createTempDirectory("clipboard-test-").toFile()
