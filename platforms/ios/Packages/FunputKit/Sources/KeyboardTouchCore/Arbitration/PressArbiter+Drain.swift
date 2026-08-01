import Foundation

extension PressArbiter {
    mutating func drain(at timestamp: TimeInterval) -> [PressEmission<Payload>] {
        var emissions: [PressEmission<Payload>] = []
        while let first = ordered.first {
            switch first.state {
            case let .resolved(payload):
                ordered.removeFirst()
                blockedDeadline = nil
                emissions.append(first.emission(payload))
            case .active:
                guard hasResolvedFollower else {
                    blockedDeadline = nil
                    return emissions
                }
                if blockedDeadline == nil {
                    blockedDeadline = timestamp + configuration.rolloverWindow
                }
                guard let deadline = blockedDeadline, timestamp >= deadline else {
                    return emissions
                }
                ordered.removeFirst()
                detached[first.contactID] = first
                statistics.recordBypass(heldFor: timestamp - first.beganAt)
            }
        }
        blockedDeadline = nil
        return emissions
    }

    /// Emits every press whose finger already lifted, ignoring the rollover ordering window.
    ///
    /// Teardown paths call this before `reset()`: a resolved press is a completed user intent,
    /// so dropping it silently loses a keystroke. Contacts still active stay untouched, because
    /// an unfinished touch must not commit (see the architecture document, §8.3).
    mutating func flushResolved() -> [PressEmission<Payload>] {
        var emissions: [PressEmission<Payload>] = []
        var remaining: [Entry] = []
        remaining.reserveCapacity(ordered.count)
        // `ordered` is kept sorted by intent, so emissions stay in press order.
        for entry in ordered {
            if case let .resolved(payload) = entry.state {
                emissions.append(entry.emission(payload))
            } else {
                remaining.append(entry)
            }
        }
        ordered = remaining
        blockedDeadline = nil
        return emissions
    }

    var hasResolvedFollower: Bool {
        ordered.dropFirst().contains {
            if case .resolved = $0.state { true } else { false }
        }
    }
}
