import Foundation

public enum DocumentMutation: Equatable, Sendable {
    case deleteBackward(count: Int)
    case insert(String)
}

public struct InputTransaction: Equatable, Sendable {
    public let sequence: UInt64
    public let mutations: [DocumentMutation]
    public let resultingState: KeyboardInputState

    public init(
        sequence: UInt64,
        mutations: [DocumentMutation],
        resultingState: KeyboardInputState
    ) {
        self.sequence = sequence
        self.mutations = mutations
        self.resultingState = resultingState
    }
}
