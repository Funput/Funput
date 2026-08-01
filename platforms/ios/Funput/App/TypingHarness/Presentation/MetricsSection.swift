#if DEBUG
import FunputShared
import SwiftUI

struct MetricsSection: View {
    let report: KeyboardTouchDiagnosticReport

    var body: some View {
        let value = report.metrics
        Section("Live metrics · #\(report.sequence)") {
            metric("Captured / committed", value.capturedContacts, value.committedContacts)
            metric("Cancelled / system", value.cancelledContacts, value.systemCancelled)
            metric("Recovered slop", value.recoveredTapSlop)
            metric("Began / ended outside", value.beganOutside, value.endedOutside)
            metric(
                "Unknown capture / resolver",
                value.captureUnknownCallback + value.resolverUnknownCallback
            )
            metric(
                "Max contacts / arbiter depth",
                value.maximumConcurrentContacts,
                value.maximumArbiterDepth
            )
            metric("Arbiter bypass", value.arbiterBypassCount)
            metric("Ownership violation", value.ownershipViolation)
            metric(
                "Release / repeat",
                value.releaseCommitted,
                value.repeatEmitted
            )
            metric(
                "Alternate / swipe",
                value.alternateCommitted,
                value.swipeCommitted
            )
            metric("Control committed", value.controlCommitted)
            metric("Gesture conflict", value.gestureConflict)
            metric("Stale timer", value.staleTimerCallback)
            metric(
                "Max capture → commit (ms)",
                value.maximumCaptureToCommitLatencyMilliseconds
            )
            metric(
                "Max terminal → emission (ms)",
                value.maximumTerminalToEmissionLatencyMilliseconds
            )
            metric(
                "Delay >40 / >120 ms",
                value.emissionDelayedOver40Milliseconds,
                value.emissionDelayedOver120Milliseconds
            )
            metric(
                "Active / pending",
                report.activeContactCount + report.pendingContactCount
            )
            metric("Timestamp tie contacts", value.timestampTieContacts)
        }
    }

    private func metric(_ label: String, _ value: Int) -> some View {
        LabeledContent(label, value: "\(value)")
    }

    private func metric(_ label: String, _ first: Int, _ second: Int) -> some View {
        LabeledContent(label, value: "\(first) / \(second)")
    }
}
#endif
