import FunputShared

extension SettingsModel {
    /// What a page link shows beside its title: where that page stands, so the keyboard
    /// tab still answers "what is set?" without opening anything.
    func summary(for destination: SettingsDestination) -> String? {
        switch destination {
        case .typing: "\(inputMethodLabel) · \(toneStyleLabel)"
        case .layout: layoutPresetLabel
        case .smartInput: "\(enabledSmartInputCount)/\(SmartInputToggle.all.count) bật"
        case .feedback: feedbackSummary
        case .clipboard: configuration.clipboardEnabled ? clipboardExpiryLabel : "Tắt"
        case .data: nil
        }
    }

    private var enabledSmartInputCount: Int {
        SmartInputToggle.all.filter { configuration[keyPath: $0.keyPath] }.count
    }

    private var feedbackSummary: String {
        let enabled = [
            configuration.isHapticFeedbackEnabled,
            configuration.isKeySoundEnabled,
            configuration.showsKeyPreviews,
        ].filter { $0 }.count
        return enabled == 0 ? "Tắt" : "\(enabled)/3 bật"
    }
}
