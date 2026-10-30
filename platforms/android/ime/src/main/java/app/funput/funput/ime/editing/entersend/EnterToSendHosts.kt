package app.funput.funput.ime.editing.entersend

import app.funput.funput.ime.collectIn
import app.funput.funput.ime.settings.entersend.EnterToSendStoring
import kotlinx.coroutines.CoroutineScope

/**
 * The IME's copy of the apps whose Enter key sends. Read when an editor starts and re-applied to
 * the open field whenever it changes, so the first field after a cold start is not left behind.
 */
internal class EnterToSendHosts {
    @Volatile
    var packages: Set<String> = emptySet()
        private set

    /** Keeps [packages] current and calls [onChanged] after each change, including the first read. */
    fun observe(settings: EnterToSendStoring, scope: CoroutineScope, onChanged: () -> Unit) {
        settings.packages.collectIn(scope) {
            packages = it
            onChanged()
        }
    }
}
