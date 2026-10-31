#if os(iOS)
import os

/// A latest-value mailbox between threads: each value submitted replaces the one
/// waiting, and taking it leaves the slot empty. For settings a worker queue must
/// apply before its next piece of work; `PersonalSuggestionQuerySlot` is the same idea
/// with drain scheduling on top.
public final class LatestValueSlot<Value: Sendable>: @unchecked Sendable {
    private let state = OSAllocatedUnfairLock<Value?>(initialState: nil)

    public init() {}

    public func submit(_ value: Value) {
        state.withLock { $0 = value }
    }

    public func take() -> Value? {
        state.withLock { state in
            defer { state = nil }
            return state
        }
    }
}
#endif
