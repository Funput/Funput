package app.funput.funput.shortcuts.model

import java.util.UUID

/** A user-authored text replacement whose identity survives edits and reloads. */
data class TextShortcut(
    val id: UUID = UUID.randomUUID(),
    val trigger: String = "",
    val expansion: String = "",
) {
    val isValid: Boolean
        get() = trigger.isNotBlank() && expansion.isNotBlank()
}
