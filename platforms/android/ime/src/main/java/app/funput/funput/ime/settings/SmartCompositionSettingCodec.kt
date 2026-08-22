package app.funput.funput.ime.settings

internal object SmartCompositionSettingCodec {
    fun decode(
        spellCheckEnabled: Boolean?,
        smartRestoreEnabled: Boolean?,
        autoCapitalizeEnabled: Boolean?,
    ) = SmartCompositionPreferences(
        spellCheckEnabled = spellCheckEnabled ?: SmartCompositionPreferences.Default.spellCheckEnabled,
        smartRestoreEnabled = smartRestoreEnabled ?: SmartCompositionPreferences.Default.smartRestoreEnabled,
        autoCapitalizeEnabled = autoCapitalizeEnabled
            ?: SmartCompositionPreferences.Default.autoCapitalizeEnabled,
    )
}
