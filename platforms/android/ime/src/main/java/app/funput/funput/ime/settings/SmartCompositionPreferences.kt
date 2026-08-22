package app.funput.funput.ime.settings

data class SmartCompositionPreferences(
    val spellCheckEnabled: Boolean,
    val smartRestoreEnabled: Boolean,
    val autoCapitalizeEnabled: Boolean,
) {
    companion object {
        val Default = SmartCompositionPreferences(
            spellCheckEnabled = false,
            smartRestoreEnabled = true,
            autoCapitalizeEnabled = true,
        )
    }
}
