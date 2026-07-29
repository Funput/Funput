#if canImport(UIKit)
import Foundation
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit

@MainActor
final class KeyboardPrimaryTouchCoordinator {
    typealias Observer = @MainActor (KeyboardPrimaryTouchMetrics) -> Void

    private let clock: @MainActor () -> TimeInterval
    private let onEvent: @MainActor (KeyboardKeyEvent) -> Void
    private var gate = KeyboardTouchCommitGate()
    private var beganAt: [ContactID: TimeInterval] = [:]
    private var observer: Observer?
    private lazy var fastTap = KeyboardFastTapPipeline(
        eligibleRoles: [.character, .vniModifier, .punctuation],
        recoveringTapSlopRoles: [.character, .vniModifier, .punctuation],
        clock: clock,
        onEmit: { [weak self] in self?.commit($0) }
    )

    init(
        clock: @escaping @MainActor () -> TimeInterval = {
            ProcessInfo.processInfo.systemUptime
        },
        onEvent: @escaping @MainActor (KeyboardKeyEvent) -> Void
    ) {
        self.clock = clock
        self.onEvent = onEvent
    }

    var activeContactCount: Int { fastTap.activeContactCount }
    var pendingContactCount: Int { gate.pendingCount }
    var metrics: KeyboardPrimaryTouchMetrics { gate.metrics }

    func updateGeometry(_ geometry: ResolvedKeyboard) {
        fastTap.updateGeometry(geometry)
    }

    func consume(_ sample: ContactSample) {
        switch fastTap.consume(sample) {
        case let .began(id, hit):
            beganAt[id] = sample.timestamp
            gate.begin(id, key: hit.key, primary: true)
        case let .fallback(id, _):
            gate.promote(id)
        case let .cancelled(id):
            if let key = gate.cancelPrimary(id) {
                onEvent(KeyboardKeyEvent(key: key, phase: .cancelled))
            }
        case .ignored:
            break
        case .tracking, .resolved:
            break
        }
        notify()
    }

    func handleLegacy(
        token: UInt64,
        event: KeyboardKeyEvent
    ) -> KeyboardKeyEvent? {
        let id = ContactID(rawValue: token)
        if event.phase.isPromotion {
            promote(token: token)
        }
        let decision = gate.legacy(id, event: event)
        if event.phase.isTerminalEvent { beganAt.removeValue(forKey: id) }
        notify()
        return decision == .emit ? event : nil
    }

    func promote(token: UInt64) {
        let id = ContactID(rawValue: token)
        guard fastTap.promoteToLegacy(id, at: clock()) else { return }
        gate.promote(id)
        notify()
    }

    func observe(_ observer: Observer?) {
        self.observer = observer
        observer?(metrics)
    }

    func reset() {
        fastTap.reset()
        gate.reset()
        beganAt.removeAll(keepingCapacity: true)
        notify()
    }

    private func commit(_ emission: PressEmission<KeyboardTouchHit>) {
        guard gate.primaryCommit(emission.contactID) else {
            notify()
            return
        }
        if let began = beganAt[emission.contactID] {
            let delay = max(0, Int(((clock() - began) * 1_000).rounded()))
            gate.metrics.maximumCaptureToCommitLatencyMilliseconds = max(
                gate.metrics.maximumCaptureToCommitLatencyMilliseconds, delay
            )
        }
        onEvent(KeyboardKeyEvent(key: emission.payload.key, phase: .released))
        notify()
    }

    private func notify() { observer?(metrics) }

}

private extension KeyboardKeyEvent.Phase {
    var isPromotion: Bool {
        switch self {
        case .repeated, .swiped, .alternateSelected: true
        default: false
        }
    }

    var isTerminalEvent: Bool {
        switch self {
        case .released, .cancelled, .swiped, .alternateSelected: true
        case .pressed, .repeated: false
        }
    }
}
#endif
