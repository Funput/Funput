package app.funput.funput.ime.settings

data class SmartCompositionPreferences(
    val spellCheckEnabled: Boolean,
    val smartRestoreEnabled: Boolean,
) {
    companion object {
        val Default = SmartCompositionPreferences(
            spellCheckEnabled = false,
            smartRestoreEnabled = true,
        )
    }
}
