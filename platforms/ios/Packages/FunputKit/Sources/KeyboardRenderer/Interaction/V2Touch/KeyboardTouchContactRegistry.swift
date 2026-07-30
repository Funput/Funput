#if canImport(UIKit)
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit

struct KeyboardTouchContactRegistry {
    private struct State {
        let key: KeySpec
        var terminal = false
    }

    private var states: [ContactID: State] = [:]
    var metrics = KeyboardV2TouchMetrics()
    var pendingCount: Int { states.count }

    func isPending(_ id: ContactID) -> Bool {
        states[id]?.terminal == false
    }

    mutating func begin(_ id: ContactID, key: KeySpec) {
        guard states[id] == nil else {
            metrics.ownershipViolation += 1
            return
        }
        states[id] = State(key: key)
    }

    mutating func commit(
        _ id: ContactID,
        action: KeyboardTouchAction
    ) -> Bool {
        guard var state = states[id], !state.terminal else {
            metrics.ownershipViolation += 1
            return false
        }
        state.terminal = true
        states[id] = state
        metrics.v2Committed += 1
        switch action {
        case .released:
            metrics.releaseCommitted += 1
            if state.key.role.isControl { metrics.controlCommitted += 1 }
        case .alternate: metrics.alternateCommitted += 1
        case .swiped: metrics.swipeCommitted += 1
        case .repeated, .cancelled: break
        }
        return true
    }

    mutating func emitRepeat(_ id: ContactID) -> Bool {
        guard isPending(id) else {
            metrics.gestureConflict += 1
            return false
        }
        metrics.repeatEmitted += 1
        return true
    }

    mutating func cancel(_ id: ContactID, system: Bool) -> KeySpec? {
        guard var state = states[id], !state.terminal else {
            metrics.ownershipViolation += 1
            return nil
        }
        state.terminal = true
        states[id] = state
        if system { metrics.systemCancelled += 1 }
        return state.key
    }

    mutating func finish(_ id: ContactID) {
        states.removeValue(forKey: id)
    }

    mutating func reset() {
        states.removeAll(keepingCapacity: true)
        metrics = KeyboardV2TouchMetrics()
    }
}
#endif
