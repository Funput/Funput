package app.funput.funput.ime.suggestions

import app.funput.funput.ime.settings.PersonalSuggestionPreferences
import app.funput.funput.ime.settings.pendingReset

/**
 * Carries a "Xoá từ đã học" request from settings to the store, once.
 *
 * The request is a token in the shared settings; it is applied when the worker has a
 * store that lasts, and acknowledged only after the wipe took, so a request made while
 * the store was unavailable is carried out the next time it is.
 */
internal class PersonalSuggestionResetGate(
    private val worker: PersonalSuggestionWorker?,
    private val onApplied: (String) -> Unit,
) {
    private var inFlight: String? = null

    fun apply(preferences: PersonalSuggestionPreferences) {
        val reset = preferences.pendingReset(inFlight) ?: return
        val worker = worker ?: return
        inFlight = reset
        worker.reset {
            inFlight = null
            onApplied(reset)
        }
    }
}
