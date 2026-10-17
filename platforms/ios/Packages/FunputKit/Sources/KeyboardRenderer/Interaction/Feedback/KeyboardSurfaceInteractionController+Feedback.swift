#if canImport(UIKit)
extension KeyboardSurfaceInteractionController {
    func updateHapticFeedback(_ presentation: KeyboardPresentation) {
        hapticStrength = presentation.isHapticFeedbackEnabled ? presentation.hapticFeedbackStrength : nil
        modeChangeStrength = presentation.hapticFeedbackStrength
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
}
#endif
