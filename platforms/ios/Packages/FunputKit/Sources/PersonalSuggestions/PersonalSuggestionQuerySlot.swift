#if os(iOS)
import os

public struct PersonalSuggestionQueryRequest: Equatable, Sendable {
    public let prefix: String
    public let generation: UInt64

    public init(prefix: String, generation: UInt64) {
        self.prefix = prefix
        self.generation = generation
    }
}

/// A bounded latest-value mailbox; callers schedule at most one serial drain.
public final class PersonalSuggestionQuerySlot: @unchecked Sendable {
    private struct State {
        var latest: PersonalSuggestionQueryRequest?
        var drainScheduled = false
    }

    private let state = OSAllocatedUnfairLock(initialState: State())

    public init() {}

    /// Returns true only when the caller must schedule a new drain.
    public func submit(_ request: PersonalSuggestionQueryRequest) -> Bool {
        state.withLock { state in
            state.latest = request
            guard !state.drainScheduled else { return false }
            state.drainScheduled = true
            return true
        }
    }

    /// Takes the newest request and drops all requests it replaced.
    public func takeLatest() -> PersonalSuggestionQueryRequest? {
        state.withLock { state in
            let value = state.latest
            state.latest = nil
            if value == nil { state.drainScheduled = false }
            return value
        }
    }

    public var hasPending: Bool {
        state.withLock { $0.latest != nil }
    }

    public func hasNewer(than generation: UInt64) -> Bool {
        state.withLock { state in
            state.latest.map { $0.generation != generation } ?? false
        }
    }
}
#endif
