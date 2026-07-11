public enum FunputInputMethod: UInt8, CaseIterable, Sendable {
    case telex = 0
    case vni = 1
}

public enum FunputToneStyle: UInt8, CaseIterable, Sendable {
    case traditional = 0
    case modern = 1
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
