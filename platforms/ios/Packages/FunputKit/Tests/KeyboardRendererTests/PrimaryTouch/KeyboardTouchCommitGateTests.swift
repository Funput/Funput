#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import KeyboardTouchCore
import Testing

struct KeyboardTouchCommitGateTests {
    @Test("Primary release emits once and suppresses legacy duplicate")
    func primaryExactlyOnce() {
        var gate = KeyboardTouchCommitGate()
        let id = ContactID(rawValue: 1)
        let key = key("a")
        gate.begin(id, key: key, primary: true)

        let committed = gate.primaryCommit(id)
        let legacy = gate.legacy(id, event: event(key, .released))
        #expect(committed)
        #expect(legacy == .suppress)
        #expect(gate.metrics.primaryCommitted == 1)
        #expect(gate.metrics.legacyReleaseSuppressed == 1)
        #expect(gate.metrics.duplicateCommitPrevented == 1)
        #expect(gate.pendingCount == 0)
    }

    @Test("Gesture promotion gives terminal ownership to legacy")
    func promotion() {
        var gate = KeyboardTouchCommitGate()
        let id = ContactID(rawValue: 2)
        let key = key("a")
        gate.begin(id, key: key, primary: true)
        gate.promote(id)

        let decision = gate.legacy(
            id,
            event: event(key, .alternateSelected(.init(text: "á")))
        )
        #expect(decision == .emit)
        #expect(gate.metrics.legacyFallback == 1)
        #expect(gate.pendingCount == 0)
    }

    @Test("System cancellation suppresses legacy recovery release")
    func cancellation() {
        var gate = KeyboardTouchCommitGate()
        let id = ContactID(rawValue: 3)
        let key = key("a")
        gate.begin(id, key: key, primary: true)

        let cancelled = gate.cancelPrimary(id)
        let decision = gate.legacy(id, event: event(key, .released))
        #expect(cancelled?.id == key.id)
        #expect(decision == .suppress)
        #expect(gate.metrics.primarySystemCancelled == 1)
        #expect(gate.pendingCount == 0)
    }

    @Test("Unknown legacy contacts remain compatible")
    func unknownLegacy() {
        var gate = KeyboardTouchCommitGate()
        let key = key("space", role: .space)
        let decision = gate.legacy(
            ContactID(rawValue: 99),
            event: event(key, .released)
        )
        #expect(decision == .emit)
        #expect(gate.metrics.commitGateViolation == 0)
    }

    @Test("One hundred thousand primary transitions remain exactly once")
    func deterministicStress() {
        var gate = KeyboardTouchCommitGate()
        let key = key("a")
        for rawID in 1...100_000 {
            let id = ContactID(rawValue: UInt64(rawID))
            gate.begin(id, key: key, primary: true)
            _ = gate.primaryCommit(id)
            _ = gate.legacy(id, event: event(key, .released))
        }
        #expect(gate.pendingCount == 0)
        #expect(gate.metrics.primaryCommitted == 100_000)
        #expect(gate.metrics.legacyReleaseSuppressed == 100_000)
        #expect(gate.metrics.commitGateViolation == 0)
    }

    private func key(
        _ id: String,
        role: KeyRole = .character
    ) -> KeySpec {
        KeySpec(id: id, label: id, role: role)
    }

    private func event(
        _ key: KeySpec,
        _ phase: KeyboardKeyEvent.Phase
    ) -> KeyboardKeyEvent {
        KeyboardKeyEvent(key: key, phase: phase)
    }
}
#endif
