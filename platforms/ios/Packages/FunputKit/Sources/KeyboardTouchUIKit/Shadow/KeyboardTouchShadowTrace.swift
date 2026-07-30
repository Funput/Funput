import Foundation
import KeyboardTouchCore
import os

@MainActor
public final class KeyboardTouchShadowTrace {
    public typealias Observer = @MainActor (KeyboardTouchShadowMetrics) -> Void

    public private(set) var metrics = KeyboardTouchShadowMetrics()
    private var observer: Observer?

    public init() {}

    func record(_ event: KeyboardTouchShadowEvent) {
        switch event {
        case .capturedBegan: metrics.capturedBegan += 1
        case .shadowResolved: metrics.shadowResolved += 1
        case .outputReleased: metrics.outputReleased += 1
        case .matched: metrics.matched += 1
        case .orderMismatch: metrics.orderMismatch += 1
        case .outputMissing: metrics.outputMissing += 1
        case .shadowMissing: metrics.shadowMissing += 1
        case .outputLate: metrics.outputLate += 1
        case .shadowLate: metrics.shadowLate += 1
        case .cancelledSystem:
            metrics.shadowCancelled += 1
            metrics.cancelledSystem += 1
        case .cancelledTapSlop:
            metrics.shadowCancelled += 1
            metrics.cancelledTapSlop += 1
        case .recoveredTapSlop: metrics.recoveredTapSlop += 1
        case .cancelledDuration:
            metrics.shadowCancelled += 1
            metrics.cancelledDuration += 1
        case .cancelledOutside:
            metrics.shadowCancelled += 1
            metrics.cancelledOutside += 1
        case .outputCancelled: metrics.outputCancelled += 1
        case .timestampTie: metrics.timestampTie += 1
        case .captureUnknown: metrics.captureUnknownCallback += 1
        case .resolverUnknown: metrics.resolverUnknownCallback += 1
        case .outOfScope: metrics.outOfScopeCallback += 1
        case .layoutChangedWhileActive: metrics.layoutChangedWhileActive += 1
        case .droppedForCapacity: metrics.droppedForCapacity += 1
        }
        KeyboardTouchShadowSignpost.emit(event, total: total(for: event))
        observer?(metrics)
    }

    public func observe(_ observer: Observer?) {
        self.observer = observer
        observer?(metrics)
    }

    public func resetMetrics() {
        metrics = KeyboardTouchShadowMetrics()
        observer?(metrics)
    }

    func recordCancellation(_ reason: ContactCancellationReason) {
        switch reason {
        case .system: record(.cancelledSystem)
        case .exceededTapSlop: record(.cancelledTapSlop)
        case .exceededDuration: record(.cancelledDuration)
        case .endedOutside: record(.cancelledOutside)
        }
    }

    func observePipeline(
        activeContacts: Int,
        arbiterDepth: Int,
        bypassCount: UInt64
    ) {
        let bypass = Int(clamping: bypassCount)
        let changed = activeContacts > metrics.maximumConcurrentContacts
            || arbiterDepth > metrics.maximumArbiterDepth
            || bypass != metrics.arbiterBypassCount
        metrics.maximumConcurrentContacts = max(
            metrics.maximumConcurrentContacts, activeContacts
        )
        metrics.maximumArbiterDepth = max(metrics.maximumArbiterDepth, arbiterDepth)
        metrics.arbiterBypassCount = bypass
        if changed { observer?(metrics) }
    }

    func recordEmissionDelay(_ delay: TimeInterval) {
        let milliseconds = max(0, Int((delay * 1_000).rounded()))
        metrics.maximumEmissionDelayMilliseconds = max(
            metrics.maximumEmissionDelayMilliseconds, milliseconds
        )
        if milliseconds > 40 {
            metrics.emissionDelayedOver40Milliseconds += 1
        }
        if milliseconds > 120 {
            metrics.emissionDelayedOver120Milliseconds += 1
        }
        observer?(metrics)
    }

    private func total(for event: KeyboardTouchShadowEvent) -> Int {
        switch event {
        case .matched: metrics.matched
        case .orderMismatch: metrics.orderMismatch
        case .outputMissing: metrics.outputMissing
        case .shadowMissing: metrics.shadowMissing
        default: 1
        }
    }
}

private enum KeyboardTouchShadowSignpost {
    private static let log = OSLog(
        subsystem: "app.funput.keyboard",
        category: "TouchShadow"
    )

    static func emit(_ event: KeyboardTouchShadowEvent, total: Int) {
        os_signpost(
            .event,
            log: log,
            name: "ShadowComparison",
            "code=%{public}d total=%{public}d",
            event.rawValue,
            total
        )
    }
}
