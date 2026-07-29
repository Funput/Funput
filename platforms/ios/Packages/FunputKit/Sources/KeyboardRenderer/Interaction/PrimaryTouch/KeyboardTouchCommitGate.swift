#if canImport(UIKit)
import KeyboardLayout
import KeyboardTouchCore

struct KeyboardTouchCommitGate {
    enum Ownership {
        case primaryPending
        case primaryCommitted
        case primaryCancelled
        case legacy
    }

    struct State {
        let key: KeySpec
        var ownership: Ownership
    }

    enum Decision: Equatable {
        case emit
        case suppress
    }

    private var states: [ContactID: State] = [:]
    var metrics = KeyboardPrimaryTouchMetrics()
    var pendingCount: Int { states.count }

    mutating func begin(_ id: ContactID, key: KeySpec, primary: Bool) {
        guard states[id] == nil else {
            metrics.commitGateViolation += 1
            return
        }
        states[id] = State(
            key: key,
            ownership: primary ? .primaryPending : .legacy
        )
    }

    mutating func promote(_ id: ContactID) {
        guard var state = states[id] else {
            metrics.commitGateViolation += 1
            return
        }
        guard state.ownership == .primaryPending else { return }
        state.ownership = .legacy
        states[id] = state
        metrics.legacyFallback += 1
    }

    mutating func primaryCommit(_ id: ContactID) -> Bool {
        guard var state = states[id] else {
            metrics.commitGateViolation += 1
            return false
        }
        guard state.ownership == .primaryPending else {
            metrics.duplicateCommitPrevented += 1
            return false
        }
        state.ownership = .primaryCommitted
        states[id] = state
        metrics.primaryCommitted += 1
        return true
    }

    mutating func cancelPrimary(_ id: ContactID) -> KeySpec? {
        guard var state = states[id],
              state.ownership == .primaryPending else {
            metrics.commitGateViolation += 1
            return nil
        }
        state.ownership = .primaryCancelled
        states[id] = state
        metrics.primarySystemCancelled += 1
        return state.key
    }

    mutating func legacy(
        _ id: ContactID,
        event: KeyboardKeyEvent
    ) -> Decision {
        guard let state = states[id] else { return .emit }
        if event.phase.isGesture, state.ownership == .primaryPending {
            promote(id)
            return .emit
        }
        switch state.ownership {
        case .legacy:
            if event.phase.isTerminal { states.removeValue(forKey: id) }
            return .emit
        case .primaryPending:
            if event.phase.isTerminal { metrics.commitGateViolation += 1 }
            return event.phase == .pressed ? .emit : .suppress
        case .primaryCommitted, .primaryCancelled:
            guard event.phase.isTerminal else { return .suppress }
            if event.phase == .released {
                metrics.legacyReleaseSuppressed += 1
                metrics.duplicateCommitPrevented += 1
            }
            states.removeValue(forKey: id)
            return .suppress
        }
    }

    mutating func reset() {
        states.removeAll(keepingCapacity: true)
        metrics = KeyboardPrimaryTouchMetrics()
    }
}

private extension KeyboardKeyEvent.Phase {
    var isGesture: Bool {
        switch self {
        case .repeated, .swiped, .alternateSelected: true
        default: false
        }
    }

    var isTerminal: Bool {
        switch self {
        case .released, .cancelled, .swiped, .alternateSelected: true
        case .pressed, .repeated: false
        }
    }
}
#endif
