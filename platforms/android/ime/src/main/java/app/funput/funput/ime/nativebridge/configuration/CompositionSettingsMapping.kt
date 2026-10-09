package app.funput.funput.ime.nativebridge.configuration

import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetter
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetters
import app.funput.funput.keyboard.model.KeyboardInputMethod

/** Builds a complete snapshot for both the document engine and local text fields. */
internal fun SmartCompositionPreferences.engineConfiguration(
    inputMethod: KeyboardInputMethod,
    toneStyle: ToneStyle,
) = EngineConfiguration(
    inputMethod = inputMethod,
    toneStyle = toneStyle,
    // Android's single restore switch drives both Rust restore behaviors.
    smartRestore = smartRestoreEnabled,
    eagerRestore = smartRestoreEnabled,
    spellCheck = spellCheckEnabled,
    autoCapitalize = false,
    extraOnsets = extraOnsets,
)

/** JNI wire contract: F=1, J=2, W=4, Z=8; independent of model storage and Rust layout. */
internal val ExtraOnsetLetters.nativeMask: Int
    get() {
        var mask = 0
        if (ExtraOnsetLetter.F in this) mask = mask or 1
        if (ExtraOnsetLetter.J in this) mask = mask or 2
        if (ExtraOnsetLetter.W in this) mask = mask or 4
        if (ExtraOnsetLetter.Z in this) mask = mask or 8
        return mask
    }
