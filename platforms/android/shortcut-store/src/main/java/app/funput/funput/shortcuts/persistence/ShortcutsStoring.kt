package app.funput.funput.shortcuts.persistence

import app.funput.funput.shortcuts.model.ShortcutLibrary

/** Injectable persistence boundary. Callers keep these blocking operations off the main thread. */
interface ShortcutsStoring {
    fun load(): ShortcutLibrary
    fun save(library: ShortcutLibrary)
}
