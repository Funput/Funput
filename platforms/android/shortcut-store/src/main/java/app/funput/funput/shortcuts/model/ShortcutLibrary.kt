package app.funput.funput.shortcuts.model

import app.funput.funput.shortcuts.persistence.ShortcutsStorageError

/** Versioned standalone document shared by the app and input method. */
data class ShortcutLibrary(
    val entries: List<TextShortcut> = emptyList(),
    val isEnabled: Boolean = true,
    val smartCase: Boolean = true,
    val inEnglish: Boolean = true,
    val schemaVersion: Int = CurrentSchemaVersion,
) {
    fun isDuplicate(entry: TextShortcut): Boolean =
        entries.any { it.id != entry.id && it.trigger == entry.trigger }

    fun validate() {
        if (schemaVersion != CurrentSchemaVersion) throw ShortcutsStorageError.UnsupportedVersion
        val ids = HashSet<java.util.UUID>()
        val triggers = HashSet<String>()
        entries.forEach { entry ->
            if (!entry.isValid || !ids.add(entry.id)) throw ShortcutsStorageError.InvalidData
            if (!triggers.add(entry.trigger)) throw ShortcutsStorageError.DuplicateTrigger
        }
    }

    companion object {
        const val CurrentSchemaVersion = 1
    }
}
