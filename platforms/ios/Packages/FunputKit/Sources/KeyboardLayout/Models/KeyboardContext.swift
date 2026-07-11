public enum KeyboardInputMethod: String, CaseIterable, Hashable, Sendable {
    case telex
    case vni
}

public enum KeyboardLayoutMode: String, CaseIterable, Hashable, Sendable {
    case letters
    case symbolsPrimary
    case symbolsSecondary
}

public enum KeyboardLanguage: String, CaseIterable, Hashable, Sendable {
    case vietnamese
    case english

    public var displayLabel: String {
        switch self {
        case .vietnamese: "Tiếng Việt"
        case .english: "Tiếng Anh"
        }
    }
}

public enum KeySwipeAction: String, Hashable, Sendable {
    case toggleLanguage
}
