#if DEBUG
import FunputShared
import SwiftUI

struct MetricsSection: View {
    let report: KeyboardTouchDiagnosticReport

    var body: some View {
        let value = report.metrics
        Section("Live metrics · #\(report.sequence)") {
            metric("Matched", value.matched)
            metric(
                "Legacy missing / late",
                value.legacyMissing + value.legacyLate
            )
            metric("Shadow missing", value.shadowMissing)
            metric("Reorder", value.orderMismatch)
            metric("Cancellation", value.cancellationDisagreement)
            metric("Cancel system", value.cancelledSystem)
            metric("Cancel slop / duration", value.cancelledTapSlop + value.cancelledDuration)
            metric("Recovered slop", value.recoveredTapSlop)
            metric("Cancel outside", value.cancelledOutside)
            metric(
                "Unknown capture / resolver",
                value.captureUnknownCallback + value.resolverUnknownCallback
            )
            metric("Out of scope", value.outOfScopeCallback)
            metric("Dropped", value.droppedForCapacity)
            metric(
                "Max contacts / arbiter depth",
                value.maximumConcurrentContacts,
                value.maximumArbiterDepth
            )
            metric("Arbiter bypass", value.arbiterBypassCount)
            metric("Max emission delay (ms)", value.maximumEmissionDelayMilliseconds)
            metric(
                "Delay >40 / >120 ms",
                value.emissionDelayedOver40Milliseconds,
                value.emissionDelayedOver120Milliseconds
            )
            metric(
                "Active / pending",
                report.activeContactCount + report.pendingComparisonCount
            )
            metric("Timestamp tie", value.timestampTie)
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
