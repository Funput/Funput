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
        registry.metrics.arbiterBypassCount = Int(
            clamping: pipeline.bypassedContactCount
        )
    }

    func recordTimestampTie(_ id: ContactID, at timestamp: TimeInterval) {
        let peers = beganAt.filter { $0.value == timestamp }.map(\.key)
        for contact in peers + [id] where tiedContacts.insert(contact).inserted {
            registry.metrics.timestampTieContacts += 1
        }
    }

    private func milliseconds(since time: TimeInterval) -> Int {
        max(0, Int(((clock() - time) * 1_000).rounded()))
    }
}
#endif
