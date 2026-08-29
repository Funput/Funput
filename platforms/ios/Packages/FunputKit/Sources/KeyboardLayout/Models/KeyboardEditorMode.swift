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

    /// Whether this page can hide its number row and carry the digits as long-press
    /// alternates on the top character row instead. See `usesCompactLetterRows`.
    public var supportsCompactLetterRows: Bool {
        switch self {
        case .text, .search, .email, .url: true
        default: false
        }
    }

    /// Whether `KeyboardLayoutPreset.system` describes this mode's keyboard.
    ///
    /// The stock keyboard renders text and search identically — the magnifying glass on
    /// the return key comes from the enter action, not the layout. Every other mode is
    /// left to the Funput preset. Deliberately not folded into
    /// `supportsVietnameseComposition`, which happens to cover the same two cases today
    /// but answers a different question and may not track this one.
    public var usesSystemPreset: Bool { self == .text || self == .search }
}
