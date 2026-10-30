package app.funput.funput.ime.editing.entersend

import app.funput.funput.ime.collectIn
import app.funput.funput.ime.settings.entersend.EnterToSendStoring
import kotlinx.coroutines.CoroutineScope

/**
 * The IME's copy of the apps whose Enter key sends. Read when an editor starts, so a change made
 * in Settings applies from the next field the keyboard opens on.
 */
internal class EnterToSendHosts {
    @Volatile
    var packages: Set<String> = emptySet()
        private set

    fun observe(settings: EnterToSendStoring, scope: CoroutineScope) {
        settings.packages.collectIn(scope) { packages = it }
    }
}
