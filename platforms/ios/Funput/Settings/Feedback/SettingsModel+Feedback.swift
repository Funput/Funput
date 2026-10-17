import FunputShared

extension SettingsModel {
    var selectedHapticStrength: HapticFeedbackStrength? {
        configuration.isHapticFeedbackEnabled ? configuration.hapticFeedbackStrength : nil
    }

    /// Returns true only when the choice is already selected or was saved successfully.
    @discardableResult
    func selectHapticStrength(_ strength: HapticFeedbackStrength?) -> Bool {
        guard strength == nil || hasFullAccess else { return false }
        return saveHapticStrength(strength)
    }

    /// Confirmation saves the requested setting before the user leaves for Settings.
    @discardableResult
    func enableFullAccessFeature(_ feature: FullAccessFeature) -> Bool {
        switch feature {
        case .hapticStrength(let strength):
            return saveHapticStrength(strength)
        case .haptics:
            return saveHapticStrength(configuration.hapticFeedbackStrength)
        case .sound:
            if configuration.isKeySoundEnabled { return true }
            update(\.isKeySoundEnabled, to: true)
            return configuration.isKeySoundEnabled
        }
    }

    private func saveHapticStrength(_ strength: HapticFeedbackStrength?) -> Bool {
        if selectedHapticStrength == strength { return true }
        update {
            $0.isHapticFeedbackEnabled = strength != nil
            if let strength { $0.hapticFeedbackStrength = strength }
        }
        return selectedHapticStrength == strength
    }
}
