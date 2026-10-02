package app.funput.funput.ime.settings.extraonsets

/** Atomic selection edits shared by the app and IME, independent of UI state. */
interface ExtraOnsetsStoring {
    /** Selects every supported letter when enabled, or clears the entire selection. */
    suspend fun setEnabled(enabled: Boolean)

    /** Adds or removes one letter from the latest persisted selection. */
    suspend fun setLetter(letter: ExtraOnsetLetter, enabled: Boolean)
}
