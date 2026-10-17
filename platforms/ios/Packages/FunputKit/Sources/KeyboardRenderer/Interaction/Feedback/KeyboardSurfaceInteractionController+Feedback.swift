#if canImport(UIKit)
extension KeyboardSurfaceInteractionController {
    func updateHapticFeedback(_ presentation: KeyboardPresentation) {
        hapticStrength = presentation.isHapticFeedbackEnabled ? presentation.hapticFeedbackStrength : nil
    }

    func performHapticFeedback() {
        guard let hapticStrength else { return }
        haptics.perform(strength: hapticStrength)
    }
}
#endif
