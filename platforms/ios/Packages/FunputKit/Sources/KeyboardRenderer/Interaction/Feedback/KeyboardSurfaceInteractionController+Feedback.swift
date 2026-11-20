#if canImport(UIKit)
extension KeyboardSurfaceInteractionController {
    /// Records the feedback settings and wakes nothing. This runs on every presentation
    /// change, and the first one lands while the keyboard is still loading: the Taptic
    /// Engine and the audio server are not part of the first frame.
    func updateFeedback(_ presentation: KeyboardPresentation) {
        hapticStrength = presentation.isHapticFeedbackEnabled ? presentation.hapticFeedbackStrength : nil
        modeChangeStrength = presentation.hapticFeedbackStrength
    }

    /// The settings a touch runs under, with its sound loading as the finger lands.
    func armFeedback(_ presentation: KeyboardPresentation) {
        updateFeedback(presentation)
        guard presentation.isKeySoundEnabled else { return }
        sounds.prepare(
            style: presentation.keySoundStyle,
            volume: presentation.keySoundVolume,
            source: presentation.keySoundVolumeSource
        )
    }

    /// Readies haptics and the key sound ahead of the first press, once the keyboard
    /// is on screen.
    func prepareFeedback(_ presentation: KeyboardPresentation) {
        armFeedback(presentation)
        haptics.prepare()
    }

    /// Lets go of what `prepareFeedback` loaded while the keyboard is off screen.
    func releaseFeedback() {
        sounds.release()
    }

    func performHapticFeedback() {
        guard let hapticStrength else { return }
        haptics.perform(strength: hapticStrength)
    }

    /// Repeated steps (caret pan, Backspace repeat) tick instead of striking.
    func performHapticTick() {
        guard hapticStrength != nil else { return }
        haptics.performTick()
    }

    /// The spacebar turned into a trackpad. Deliberately outside the on/off switch: that
    /// switch silences the buzz of ordinary typing, which the user sees landing anyway,
    /// while this reports a mode the keyboard shows no other way, during a hold that looks
    /// identical to a thumb resting on space. It uses the remembered strength.
    func performModeChangeFeedback() {
        haptics.perform(strength: modeChangeStrength)
    }

    func performKeySound(_ kind: KeySoundKind?, presentation: KeyboardPresentation) {
        guard let kind, presentation.isKeySoundEnabled else { return }
        sounds.play(
            kind,
            style: presentation.keySoundStyle,
            volume: presentation.keySoundVolume,
            source: presentation.keySoundVolumeSource
        )
    }
}
#endif
