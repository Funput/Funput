package app.funput.funput.ui.shortcuts

import app.funput.funput.shortcuts.model.ShortcutLibrary
import app.funput.funput.shortcuts.model.TextShortcut
import app.funput.funput.shortcuts.persistence.ShortcutsStoring
import java.util.UUID

internal class ShortcutTestStore(initial: ShortcutLibrary) : ShortcutsStoring {
    @Volatile
    var value = initial
    var saveCount = 0
        private set
    var saveFailure: Throwable? = null

    override fun load() = value

    @Synchronized
    override fun save(library: ShortcutLibrary) {
        saveFailure?.let { throw it }
        value = library
        saveCount += 1
    }
}

internal fun testShortcut(trigger: String, expansion: String) = TextShortcut(
    id = UUID.nameUUIDFromBytes(trigger.toByteArray()),
    trigger = trigger,
    expansion = expansion,
)
