package app.funput.funput.ime.settings

import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetters

internal object SmartCompositionSettingCodec {
    fun decode(
        spellCheckEnabled: Boolean?,
        smartRestoreEnabled: Boolean?,
        autoCapitalizeEnabled: Boolean?,
        extraOnsets: String? = null,
        typoCorrectionEnabled: Boolean? = null,
    ) = SmartCompositionPreferences(
        spellCheckEnabled = spellCheckEnabled ?: SmartCompositionPreferences.Default.spellCheckEnabled,
        smartRestoreEnabled = smartRestoreEnabled ?: SmartCompositionPreferences.Default.smartRestoreEnabled,
        autoCapitalizeEnabled = autoCapitalizeEnabled
            ?: SmartCompositionPreferences.Default.autoCapitalizeEnabled,
        extraOnsets = ExtraOnsetLetters.parse(extraOnsets),
        typoCorrectionEnabled = typoCorrectionEnabled ?: SmartCompositionPreferences.Default.typoCorrectionEnabled,
    )
}
