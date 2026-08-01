import Foundation

/// What the arbiter observed while ordering presses.
///
/// Counters live here rather than as loose properties on `PressArbiter`, so adding one is a
/// field and a `record…` call instead of a new property threaded through every layer above.
public struct PressArbiterStatistics: Equatable, Sendable {
    /// Contacts detached because the rollover window expired while they were still down.
    public internal(set) var bypassedContacts: UInt64 = 0

    /// Longest a bypassed head had been held when its window expired. This is the measurement
    /// that has to settle `rolloverWindow` (architecture document, §19.1).
    public internal(set) var maximumBypassHoldSeconds: TimeInterval = 0

    public init() {}

    mutating func recordBypass(heldFor duration: TimeInterval) {
        bypassedContacts &+= 1
        maximumBypassHoldSeconds = max(maximumBypassHoldSeconds, duration)
    }
}
