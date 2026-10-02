public enum FunputInputMethod: UInt8, CaseIterable, Sendable {
    case telex = 0
    case vni = 1
    case telexAdvanced = 2
}

public enum FunputToneStyle: UInt8, CaseIterable, Sendable {
    case traditional = 0
    case modern = 1
}

/// Extra initial consonants, using the C ABI's `ONSET_*` bits without importing C.
public struct FunputExtraOnsets: OptionSet, Hashable, Sendable {
    public let rawValue: UInt8

    public init(rawValue: UInt8) { self.rawValue = rawValue }

    public static let f = FunputExtraOnsets(rawValue: 1)
    public static let j = FunputExtraOnsets(rawValue: 2)
    public static let w = FunputExtraOnsets(rawValue: 4)
    public static let z = FunputExtraOnsets(rawValue: 8)
}

/// The engine's durable user options, applied together by
/// ``FunputComposer/configure(_:)``.
///
/// Typed counterpart of the C ABI's `FunputConfig`, so callers outside `FunputEngine`
/// never touch the C layer. The runtime state — whether Vietnamese composition is
/// currently on (``FunputComposer/setEnabled(_:)``) and a mid-session method switch
/// (``FunputComposer/setInputMethod(_:)``) — is deliberately not part of this type.
/// Extra onsets use a separate setter so the by-value C configuration ABI stays stable.
///
/// One `FunputConfig` field has no counterpart here: `auto_capitalize`. Case on iOS
/// follows the Shift state, so the engine's own sentence tracker must never get a
/// say, and leaving the option out means nobody can hand it one by accident. See
/// ``FunputComposer/configure(_:)``.
public struct FunputCompositionOptions: Equatable, Sendable {
    public var inputMethod: FunputInputMethod
    public var toneStyle: FunputToneStyle
    public var smartRestore: Bool
    public var eagerRestore: Bool
    public var spellCheck: Bool
    public var extraOnsets: FunputExtraOnsets

    public init(
        inputMethod: FunputInputMethod,
        toneStyle: FunputToneStyle,
        smartRestore: Bool,
        eagerRestore: Bool,
        spellCheck: Bool,
        extraOnsets: FunputExtraOnsets = []
    ) {
        self.inputMethod = inputMethod
        self.toneStyle = toneStyle
        self.smartRestore = smartRestore
        self.eagerRestore = eagerRestore
        self.spellCheck = spellCheck
        self.extraOnsets = extraOnsets
    }
}

public enum FunputCompositionAction: UInt8, Sendable {
    case none = 0
    case send = 1
    case restore = 2
}

public struct FunputCompositionResult: Equatable, Sendable {
    public let action: FunputCompositionAction
    public let deleteCount: Int
    public let text: String

    public init(
        action: FunputCompositionAction,
        deleteCount: Int,
        text: String
    ) {
        self.action = action
        self.deleteCount = deleteCount
        self.text = text
    }

    public static let none = FunputCompositionResult(
        action: .none,
        deleteCount: 0,
        text: ""
    )
}
