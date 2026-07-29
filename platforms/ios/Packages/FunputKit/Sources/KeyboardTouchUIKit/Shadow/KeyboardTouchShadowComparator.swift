import Foundation
import KeyboardTouchCore

@MainActor
final class KeyboardTouchShadowComparator {
    struct Pending {
        let identity: ShadowKeyIdentity
        let observedAt: TimeInterval
        let hasTimestampTie: Bool
    }

    private let configuration: KeyboardTouchShadowConfiguration
    private let clock: PressArbiterDriver<ShadowKeyIdentity>.Clock
    private let schedule: DeadlineSchedule
    private let trace: KeyboardTouchShadowTrace
    private var legacy: [Pending] = []
    private var shadow: [Pending] = []
    private var awaitingLegacy: [ShadowKeyIdentity] = []
    private var awaitingShadow: [ShadowKeyIdentity] = []
    private var scheduled: ScheduledDeadline?
    private var generation: UInt64 = 0

    var pendingCount: Int { legacy.count + shadow.count }
    var isSettled: Bool { pendingCount == 0 && scheduled == nil }

    init(
        configuration: KeyboardTouchShadowConfiguration,
        clock: @escaping PressArbiterDriver<ShadowKeyIdentity>.Clock,
        schedule: @escaping DeadlineSchedule,
        trace: KeyboardTouchShadowTrace
    ) {
        self.configuration = configuration
        self.clock = clock
        self.schedule = schedule
        self.trace = trace
    }

    func recordLegacy(_ identity: ShadowKeyIdentity) {
        if remove(identity, from: &awaitingLegacy) {
            trace.record(.legacyReleased)
            trace.record(.legacyLate)
            return
        }
        legacy.append(Pending(identity: identity, observedAt: clock(), hasTimestampTie: false))
        trace.record(.legacyReleased)
        reconcile()
    }

    func recordShadow(_ identity: ShadowKeyIdentity, timestampTie: Bool) {
        if remove(identity, from: &awaitingShadow) {
            trace.record(.shadowResolved)
            trace.record(.shadowLate)
            return
        }
        shadow.append(Pending(
            identity: identity,
            observedAt: clock(),
            hasTimestampTie: timestampTie
        ))
        trace.record(.shadowResolved)
        reconcile()
    }

    func reset() {
        generation &+= 1
        scheduled?.cancel()
        scheduled = nil
        legacy.removeAll(keepingCapacity: true)
        shadow.removeAll(keepingCapacity: true)
        awaitingLegacy.removeAll(keepingCapacity: true)
        awaitingShadow.removeAll(keepingCapacity: true)
    }

    private func reconcile() {
        scheduled?.cancel()
        scheduled = nil
        while let old = legacy.first, let new = shadow.first, old.identity == new.identity {
            legacy.removeFirst()
            shadow.removeFirst()
            trace.record(new.hasTimestampTie ? .timestampTie : .matched)
        }
        if legacy.count > 1, shadow.count > 1,
           legacy[0].identity == shadow[1].identity,
           legacy[1].identity == shadow[0].identity {
            let hasTimestampTie = shadow[0].hasTimestampTie || shadow[1].hasTimestampTie
            legacy.removeFirst(2)
            shadow.removeFirst(2)
            trace.record(hasTimestampTie ? .timestampTie : .orderMismatch)
        }
        trimToCapacity()
        scheduleSettlement()
    }

    private func trimToCapacity() {
        let limit = configuration.maximumBufferedActions
        while legacy.count > limit {
            awaitingShadow.append(legacy.removeFirst().identity)
            trace.record(.droppedForCapacity)
        }
        while shadow.count > limit {
            awaitingLegacy.append(shadow.removeFirst().identity)
            trace.record(.droppedForCapacity)
        }
        awaitingLegacy = Array(awaitingLegacy.suffix(limit))
        awaitingShadow = Array(awaitingShadow.suffix(limit))
    }

    private func scheduleSettlement() {
        let firstObserved = [legacy.first?.observedAt, shadow.first?.observedAt]
            .compactMap { $0 }.min()
        guard let firstObserved else { return }
        generation &+= 1
        let currentGeneration = generation
        let delay = max(0, firstObserved + configuration.settlementWindow - clock())
        scheduled = schedule(delay) { [weak self] in
            self?.settle(generation: currentGeneration)
        }
    }

    private func settle(generation: UInt64) {
        guard generation == self.generation else { return }
        scheduled = nil
        let cutoff = clock() - configuration.settlementWindow
        if let old = legacy.first, let new = shadow.first,
           old.observedAt <= cutoff, new.observedAt <= cutoff {
            legacy.removeFirst()
            shadow.removeFirst()
            trace.record(old.hasTimestampTie || new.hasTimestampTie ? .timestampTie : .orderMismatch)
        } else if let old = legacy.first, old.observedAt <= cutoff {
            awaitingShadow.append(legacy.removeFirst().identity)
            trace.record(.shadowMissing)
        } else if let new = shadow.first, new.observedAt <= cutoff {
            awaitingLegacy.append(shadow.removeFirst().identity)
            trace.record(.legacyMissing)
        }
        reconcile()
    }

    private func remove(_ identity: ShadowKeyIdentity, from values: inout [ShadowKeyIdentity]) -> Bool {
        guard let index = values.firstIndex(of: identity) else { return false }
        values.remove(at: index)
        return true
    }
}
