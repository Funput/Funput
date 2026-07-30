#if canImport(UIKit)
import Foundation
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit

@MainActor
final class KeyboardV2TouchCoordinator {
    typealias Observer = @MainActor (KeyboardV2TouchMetrics) -> Void

    let clock: @MainActor () -> TimeInterval
    let onEvent: @MainActor (KeyboardKeyEvent) -> Void
    var registry = KeyboardTouchContactRegistry()
    var beganAt: [ContactID: TimeInterval] = [:]
    var terminalAt: [ContactID: TimeInterval] = [:]
    var hits: [ContactID: KeyboardTouchHit] = [:]
    var claims: [ContactID: KeyboardSurfaceInteractionController.GestureClaim] = [:]
    var finishedUIKitContacts: Set<ContactID> = []
    private var observer: Observer?
    lazy var pipeline = KeyboardTouchPipeline(
        eligibleRoles: Self.touchRoles,
        recoveringTapSlopRoles: Self.touchRoles,
        resolverConfiguration: .init(
            maximumTapDuration: .greatestFiniteMagnitude
        ),
        clock: clock,
        onResolved: { [weak self] id, time in self?.terminalAt[id] = time },
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

    var activeContactCount: Int { pipeline.activeContactCount }
    var pendingContactCount: Int { registry.pendingCount }
    var metrics: KeyboardV2TouchMetrics { registry.metrics }

    func updateGeometry(_ geometry: ResolvedKeyboard) {
        pipeline.updateGeometry(geometry)
    }

    func observe(_ observer: Observer?) {
        self.observer = observer
        observer?(metrics)
    }

    func reset() {
        pipeline.reset()
        registry.reset()
        beganAt.removeAll(keepingCapacity: true)
        terminalAt.removeAll(keepingCapacity: true)
        hits.removeAll(keepingCapacity: true)
        claims.removeAll(keepingCapacity: true)
        finishedUIKitContacts.removeAll(keepingCapacity: true)
        notify()
    }

    func cancel(_ id: ContactID, emit: Bool, system: Bool = true) {
        if let key = registry.cancel(id, system: system), emit {
            onEvent(KeyboardKeyEvent(key: key, phase: .cancelled))
        }
    }

    private func commit(_ emission: PressEmission<KeyboardTouchAction>) {
        guard registry.commit(
            emission.contactID,
            action: emission.payload
        ) else { return }
        recordLatency(emission.contactID)
        onEvent(emission.payload.keyEvent)
        if finishedUIKitContacts.contains(emission.contactID) {
            clear(emission.contactID)
        }
        notify()
    }

    private func recordLatency(_ id: ContactID) {
        if let began = beganAt[id] {
            registry.metrics.maximumCaptureToCommitLatencyMilliseconds = max(
                registry.metrics.maximumCaptureToCommitLatencyMilliseconds,
                milliseconds(since: began)
            )
        }
        if let terminal = terminalAt[id] {
            registry.metrics.maximumTerminalToEmissionLatencyMilliseconds = max(
                registry.metrics.maximumTerminalToEmissionLatencyMilliseconds,
                milliseconds(since: terminal)
            )
        }
    }

    private func milliseconds(since time: TimeInterval) -> Int {
        max(0, Int(((clock() - time) * 1_000).rounded()))
    }

    func notify() { observer?(metrics) }

    func clear(_ id: ContactID) {
        registry.finish(id)
        claims.removeValue(forKey: id)
        hits.removeValue(forKey: id)
        beganAt.removeValue(forKey: id)
        terminalAt.removeValue(forKey: id)
        finishedUIKitContacts.remove(id)
    }
}
#endif
