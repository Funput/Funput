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
    private var output: [Pending] = []
    private var shadow: [Pending] = []
    private var awaitingOutput: [ShadowKeyIdentity] = []
    private var awaitingShadow: [ShadowKeyIdentity] = []
    private var scheduled: ScheduledDeadline?
    private var generation: UInt64 = 0

    var pendingCount: Int { output.count + shadow.count }
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

    func recordActual(_ identity: ShadowKeyIdentity) {
        if remove(identity, from: &awaitingOutput) {
            trace.record(.outputReleased)
            trace.record(.outputLate)
            return
        }
        output.append(Pending(identity: identity, observedAt: clock(), hasTimestampTie: false))
        trace.record(.outputReleased)
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
        output.removeAll(keepingCapacity: true)
        shadow.removeAll(keepingCapacity: true)
        awaitingOutput.removeAll(keepingCapacity: true)
        awaitingShadow.removeAll(keepingCapacity: true)
    }

    private func reconcile() {
        scheduled?.cancel()
        scheduled = nil
        while let old = output.first, let new = shadow.first, old.identity == new.identity {
            output.removeFirst()
            shadow.removeFirst()
            trace.record(new.hasTimestampTie ? .timestampTie : .matched)
        }
        if output.count > 1, shadow.count > 1,
           output[0].identity == shadow[1].identity,
           output[1].identity == shadow[0].identity {
            let hasTimestampTie = shadow[0].hasTimestampTie || shadow[1].hasTimestampTie
            output.removeFirst(2)
            shadow.removeFirst(2)
            trace.record(hasTimestampTie ? .timestampTie : .orderMismatch)
        }
        trimToCapacity()
        scheduleSettlement()
    }

    private func trimToCapacity() {
        let limit = configuration.maximumBufferedActions
        while output.count > limit {
            awaitingShadow.append(output.removeFirst().identity)
            trace.record(.droppedForCapacity)
        }
        while shadow.count > limit {
            awaitingOutput.append(shadow.removeFirst().identity)
            trace.record(.droppedForCapacity)
        }
        awaitingOutput = Array(awaitingOutput.suffix(limit))
        awaitingShadow = Array(awaitingShadow.suffix(limit))
    }

    private func scheduleSettlement() {
        let firstObserved = [output.first?.observedAt, shadow.first?.observedAt]
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
        if let old = output.first, let new = shadow.first,
           old.observedAt <= cutoff, new.observedAt <= cutoff {
            output.removeFirst()
            shadow.removeFirst()
            trace.record(old.hasTimestampTie || new.hasTimestampTie ? .timestampTie : .orderMismatch)
        } else if let old = output.first, old.observedAt <= cutoff {
            awaitingShadow.append(output.removeFirst().identity)
            trace.record(.shadowMissing)
        } else if let new = shadow.first, new.observedAt <= cutoff {
            awaitingOutput.append(shadow.removeFirst().identity)
            trace.record(.outputMissing)
        }
        reconcile()
    }

    private func remove(_ identity: ShadowKeyIdentity, from values: inout [ShadowKeyIdentity]) -> Bool {
        guard let index = values.firstIndex(of: identity) else { return false }
        values.remove(at: index)
        return true
    }
}
