public struct KeyboardToolbarSpec: Hashable, Sendable {
    public let inputMethodKey: KeySpec
    public let systemInputModeKey: KeySpec?
    public let settingsKey: KeySpec
    public let emojiKey: KeySpec

    public var keys: [KeySpec] {
        [inputMethodKey, systemInputModeKey, settingsKey, emojiKey].compactMap { $0 }
    }

    public init(
        inputMethod: KeyboardInputMethod,
        systemInputModeKey: KeySpec? = nil
    ) {
        let nextMethod = inputMethod == .vni ? "Telex" : "VNI"
        inputMethodKey = KeySpec(
            id: "toolbar-input-method",
            label: inputMethod == .vni ? "V" : "T",
            role: .inputMethod,
            accessibilityLabel: "\(inputMethod.displayName). Chuyển sang \(nextMethod)"
        )
        self.systemInputModeKey = systemInputModeKey
        settingsKey = KeySpec(
            id: "toolbar-settings",
            label: "",
            role: .settings,
            accessibilityLabel: "Cài đặt"
        )
        emojiKey = KeySpec(
            id: "toolbar-emoji",
            label: "",
            role: .emoji,
            accessibilityLabel: "Biểu tượng cảm xúc"
        )
    }

    public static func standard(for inputMethod: KeyboardInputMethod) -> Self {
        KeyboardToolbarSpec(inputMethod: inputMethod)
    }

    public static func withSystemInputMode(for inputMethod: KeyboardInputMethod) -> Self {
        KeyboardToolbarSpec(
            inputMethod: inputMethod,
            systemInputModeKey: KeySpec(
                id: "toolbar-system-input-mode",
                label: "",
                role: .systemInputMode,
                accessibilityLabel: "Chuyển bàn phím"
            )
        )
    }
}

private extension KeyboardInputMethod {
    var displayName: String {
        switch self {
        case .telex: "Telex"
        case .vni: "VNI"
        }
    }
}
