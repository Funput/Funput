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
                bypassedContactCount &+= 1
            }
        }
        blockedDeadline = nil
        return emissions
    }

    var hasResolvedFollower: Bool {
        ordered.dropFirst().contains {
            if case .resolved = $0.state { true } else { false }
        }
    }
}
