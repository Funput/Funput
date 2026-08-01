#if canImport(UIKit)
import Foundation
import KeyboardTouchCore

extension KeyboardTouchCoordinator {
    func recordLatency(_ id: ContactID) {
        if let began = beganAt[id] {
            registry.metrics.maximumCaptureToCommitLatencyMilliseconds = max(
                registry.metrics.maximumCaptureToCommitLatencyMilliseconds,
                milliseconds(since: began)
            )
        }
        guard let terminal = terminalAt[id] else { return }
        let delay = milliseconds(since: terminal)
        registry.metrics.maximumTerminalToEmissionLatencyMilliseconds = max(
            registry.metrics.maximumTerminalToEmissionLatencyMilliseconds,
            delay
        )
        if delay > 40 { registry.metrics.emissionDelayedOver40Milliseconds += 1 }
        if delay > 120 { registry.metrics.emissionDelayedOver120Milliseconds += 1 }
    }

    func observePipelineState() {
        registry.metrics.maximumConcurrentContacts = max(
            registry.metrics.maximumConcurrentContacts, activeContactCount
        )
        registry.metrics.maximumArbiterDepth = max(
            registry.metrics.maximumArbiterDepth,
            pipeline.orderedContactCount + pipeline.heldContactCount
        )
        let statistics = pipeline.statistics
        registry.metrics.arbiterBypassCount = Int(
            clamping: statistics.arbiter.bypassedContacts
        )
        registry.metrics.maximumBypassHoldMilliseconds = max(
            registry.metrics.maximumBypassHoldMilliseconds,
            Int((statistics.arbiter.maximumBypassHoldSeconds * 1_000).rounded())
        )
        registry.metrics.endedOutside = statistics.releasesOutside
        registry.metrics.recoveredReleaseOutside = statistics.recoveredReleasesOutside
    }

    func recordTimestampTie(_ id: ContactID, at timestamp: TimeInterval) {
        let peers = beganAt.filter { $0.value == timestamp }.map(\.key)
        // `peers + [id]` always contains `id`, so without this guard every contact counted and
        // the metric merely mirrored `capturedContacts`. It has to answer the open question in
        // the architecture document (§9.5): how often UIKit really reports equal timestamps.
        guard !peers.isEmpty else { return }
        for contact in peers + [id] where tiedContacts.insert(contact).inserted {
            registry.metrics.timestampTieContacts += 1
        }
    }

    private func milliseconds(since time: TimeInterval) -> Int {
        max(0, Int(((clock() - time) * 1_000).rounded()))
    }
}
#endif
