public enum KeyboardEditorMode: String, CaseIterable, Hashable, Sendable {
    case text
    case search
    case email
    case url
    case phone
    case password
    case pin
    case number
    case numberDecimal
    case numberSigned
    case numberSignedDecimal

    public var supportsVietnameseComposition: Bool {
        self == .text || self == .search
    }

    public var isNumber: Bool {
        switch self {
        case .number, .numberDecimal, .numberSigned, .numberSignedDecimal: true
        default: false
        }
    }

    public var isPassword: Bool { self == .password || self == .pin }
    public var allowsDecimal: Bool { self == .numberDecimal || self == .numberSignedDecimal }
    public var allowsSigned: Bool { self == .numberSigned || self == .numberSignedDecimal }
    public var usesKeypad: Bool { isNumber || self == .phone || self == .pin }
}
