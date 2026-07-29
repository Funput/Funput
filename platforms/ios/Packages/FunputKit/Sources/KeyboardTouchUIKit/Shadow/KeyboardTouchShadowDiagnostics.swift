import Foundation

public struct KeyboardTouchShadowDiagnosticState: Equatable, Sendable {
    public let metrics: KeyboardTouchShadowMetrics
    public let activeContactCount: Int
    public let pendingComparisonCount: Int
    public let isSettled: Bool
}

@MainActor
public extension KeyboardTouchShadowPipeline {
    var diagnosticState: KeyboardTouchShadowDiagnosticState {
        KeyboardTouchShadowDiagnosticState(
            metrics: trace.metrics,
            activeContactCount: activeContactCount,
            pendingComparisonCount: comparator.pendingCount,
            isSettled: activeContactCount == 0 && comparator.isSettled
        )
    }

    @discardableResult
    func resetDiagnosticsIfIdle() -> Bool {
        guard activeContactCount == 0 else { return false }
        comparator.reset()
        trace.resetMetrics()
        return true
    }
}
