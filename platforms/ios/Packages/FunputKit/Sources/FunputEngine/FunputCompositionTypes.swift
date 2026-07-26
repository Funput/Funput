public enum FunputInputMethod: UInt8, CaseIterable, Sendable {
    case telex = 0
    case vni = 1
    case telexAdvanced = 2
}

public enum FunputToneStyle: UInt8, CaseIterable, Sendable {
    case traditional = 0
    case modern = 1
}

/// The engine's durable user options, applied in one call by
/// ``FunputComposer/configure(_:)``.
///
/// Typed counterpart of the C ABI's `FunputConfig`, so callers outside `FunputEngine`
/// never touch the C layer. The runtime state — whether Vietnamese composition is
/// currently on (``FunputComposer/setEnabled(_:)``) and a mid-session method switch
/// (``FunputComposer/setInputMethod(_:)``) — is deliberately not part of this type.
public struct FunputCompositionOptions: Equatable, Sendable {
    public var inputMethod: FunputInputMethod
    public var toneStyle: FunputToneStyle
    public var smartRestore: Bool
    public var eagerRestore: Bool
    public var spellCheck: Bool
    public var autoCapitalize: Bool

    public init(
        inputMethod: FunputInputMethod,
        toneStyle: FunputToneStyle,
        smartRestore: Bool,
        eagerRestore: Bool,
        spellCheck: Bool,
        autoCapitalize: Bool
    ) {
        self.inputMethod = inputMethod
        self.toneStyle = toneStyle
        self.smartRestore = smartRestore
        self.eagerRestore = eagerRestore
        self.spellCheck = spellCheck
        self.autoCapitalize = autoCapitalize
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
