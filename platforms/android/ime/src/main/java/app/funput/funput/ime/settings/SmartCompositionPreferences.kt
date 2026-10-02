package app.funput.funput.ime.settings

import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetters

/** Durable composition preferences shared by Settings, the document and local text fields. */
data class SmartCompositionPreferences(
    /** Only accept diacritics that can form a valid Vietnamese syllable. */
    val spellCheckEnabled: Boolean,
    /** Restore English words, including eager restore while typing. */
    val smartRestoreEnabled: Boolean,
    /** Raise Android's visible Shift key when the document starts a sentence. */
    val autoCapitalizeEnabled: Boolean,
    /** Extra initial consonants; absent settings preserve standard Vietnamese spelling. */
    val extraOnsets: ExtraOnsetLetters = ExtraOnsetLetters.None,
) {
    /** Defaults for absent Preferences DataStore keys. */
    companion object {
        /** New and existing installations keep extra onsets disabled. */
        val Default = SmartCompositionPreferences(
            spellCheckEnabled = false,
            smartRestoreEnabled = true,
            autoCapitalizeEnabled = true,
        )
    }
}
