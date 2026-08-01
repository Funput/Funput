#if canImport(UIKit)
import Foundation
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit

@MainActor
final class KeyboardTouchCoordinator {
    typealias Observer = @MainActor (KeyboardTouchMetrics) -> Void

    let clock: @MainActor () -> TimeInterval
    let onEvent: @MainActor (KeyboardKeyEvent) -> Void
    var registry = KeyboardTouchContactRegistry()
    var beganAt: [ContactID: TimeInterval] = [:]
    var terminalAt: [ContactID: TimeInterval] = [:]
    var hits: [ContactID: KeyboardTouchHit] = [:]
    var claims: [ContactID: KeyboardSurfaceInteractionController.GestureClaim] = [:]
    var finishedUIKitContacts: Set<ContactID> = []
    var tiedContacts: Set<ContactID> = []
    private var observer: Observer?
    lazy var pipeline = KeyboardTouchPipeline(
        policy: Self.recoveryPolicy,
        resolverConfiguration: Self.resolverConfiguration,
        arbiterConfiguration: Self.arbiterConfiguration,
        clock: clock,
        onResolved: { [weak self] id, time in self?.terminalAt[id] = time },
        onStaleDeadline: { [weak self] in
            self?.registry.metrics.staleTimerCallback += 1
            self?.notify()
        },
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
    var metrics: KeyboardTouchMetrics { registry.metrics }

    var geometrySnapshot: KeyboardGeometrySnapshot? { pipeline.geometrySnapshot }

    func updateGeometry(_ geometry: ResolvedKeyboard) {
        let wasActive = activeContactCount > 0
        if pipeline.updateGeometry(geometry), wasActive {
            registry.metrics.layoutChangedWhileActive += 1
            notify()
        }
    }

    func observe(_ observer: Observer?) {
        self.observer = observer
        observer?(metrics)
    }

    /// Tears down every contact. Pass `flushingResolvedPresses` when the surface stays alive —
    /// a layout swap must not swallow presses whose finger already lifted. Leave it off when the
    /// keyboard itself is going away, since the document proxy is on its way out too.
    func reset(flushingResolvedPresses: Bool = false) {
        // Flush first: the emissions still need the registry to claim exactly-once ownership.
        // A teardown that does not flush is a session boundary and starts the counter over.
        let carried = flushingResolvedPresses
            ? registry.metrics.flushedOnLayoutChange + pipeline.flushResolvedPresses()
            : 0
        pipeline.reset()
        registry.reset(carryingFlushCount: carried)
        beganAt.removeAll(keepingCapacity: true)
        terminalAt.removeAll(keepingCapacity: true)
        hits.removeAll(keepingCapacity: true)
        claims.removeAll(keepingCapacity: true)
        finishedUIKitContacts.removeAll(keepingCapacity: true)
        tiedContacts.removeAll(keepingCapacity: true)
        notify()
    }

    func recordUnknownCaptureCallback() {
        registry.metrics.captureUnknownCallback += 1
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

    func notify() { observer?(metrics) }

    func clear(_ id: ContactID) {
        registry.finish(id)
        claims.removeValue(forKey: id)
        hits.removeValue(forKey: id)
        beganAt.removeValue(forKey: id)
        terminalAt.removeValue(forKey: id)
        finishedUIKitContacts.remove(id)
        tiedContacts.remove(id)
    }

}
#endif
