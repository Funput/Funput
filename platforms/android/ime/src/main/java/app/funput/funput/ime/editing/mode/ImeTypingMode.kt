package app.funput.funput.ime.editing.mode

import app.funput.funput.keyboard.model.KeyboardLanguage

/** Editor policy and selected language; independent of composition and editor writes. */
internal class ImeTypingMode {
    var compositionAllowed = true
        private set
    var suggestionsAllowed = true
        private set
    var shortcutsAllowed = true
        private set
    var language = KeyboardLanguage.VIETNAMESE
        private set

    val usesVietnameseComposition: Boolean
        get() = compositionAllowed && language == KeyboardLanguage.VIETNAMESE

    fun usesEnglishShortcuts(runsInEnglish: Boolean): Boolean =
        shortcutsAllowed && compositionAllowed && language == KeyboardLanguage.ENGLISH && runsInEnglish

    fun configure(composition: Boolean, suggestions: Boolean, shortcuts: Boolean) {
        compositionAllowed = composition
        suggestionsAllowed = suggestions
        shortcutsAllowed = shortcuts
    }

    fun selectLanguage(value: KeyboardLanguage) {
        if (compositionAllowed) language = value
    }
}
