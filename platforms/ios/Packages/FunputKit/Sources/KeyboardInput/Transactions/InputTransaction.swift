import Foundation

public enum DocumentMutation: Equatable, Sendable {
    case deleteBackward(count: Int)
    case insert(String)
    /// Moves the caret without changing the text. Kept in the same channel as the text
    /// mutations so the document shadow is never advanced behind the synchronizer's back.
    case moveCursor(offset: Int)
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
