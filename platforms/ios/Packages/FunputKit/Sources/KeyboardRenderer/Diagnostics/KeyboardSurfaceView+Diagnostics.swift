#if canImport(UIKit) && DEBUG
import KeyboardTouchUIKit

public extension KeyboardSurfaceView {
    var touchDiagnosticSnapshot: KeyboardTouchDiagnosticSnapshot {
        let state = touchShadow.diagnosticState
        let metrics = state.metrics
        let primary = primaryTouch.metrics
        return KeyboardTouchDiagnosticSnapshot(
            capturedBegan: metrics.capturedBegan,
            shadowResolved: metrics.shadowResolved,
            legacyReleased: metrics.legacyReleased,
            matched: metrics.matched,
            orderMismatch: metrics.orderMismatch,
            legacyMissing: metrics.legacyMissing,
            shadowMissing: metrics.shadowMissing,
            legacyLate: metrics.legacyLate,
            shadowLate: metrics.shadowLate,
            shadowCancelled: metrics.shadowCancelled,
            legacyCancelled: metrics.legacyCancelled,
            cancelledSystem: metrics.cancelledSystem,
            cancelledTapSlop: metrics.cancelledTapSlop,
            recoveredTapSlop: metrics.recoveredTapSlop,
            cancelledDuration: metrics.cancelledDuration,
            cancelledOutside: metrics.cancelledOutside,
            timestampTie: metrics.timestampTie,
            unknownCallback: metrics.unknownCallback,
            captureUnknownCallback: metrics.captureUnknownCallback,
            resolverUnknownCallback: metrics.resolverUnknownCallback,
            outOfScopeCallback: metrics.outOfScopeCallback,
            layoutChangedWhileActive: metrics.layoutChangedWhileActive,
            droppedForCapacity: metrics.droppedForCapacity,
            maximumConcurrentContacts: metrics.maximumConcurrentContacts,
            maximumArbiterDepth: metrics.maximumArbiterDepth,
            arbiterBypassCount: metrics.arbiterBypassCount,
            maximumEmissionDelayMilliseconds:
                metrics.maximumEmissionDelayMilliseconds,
            emissionDelayedOver40Milliseconds:
                metrics.emissionDelayedOver40Milliseconds,
            emissionDelayedOver120Milliseconds:
                metrics.emissionDelayedOver120Milliseconds,
            primaryCommitted: primary.primaryCommitted,
            legacyFallback: primary.legacyFallback,
            legacyReleaseSuppressed: primary.legacyReleaseSuppressed,
            primarySystemCancelled: primary.primarySystemCancelled,
            commitGateViolation: primary.commitGateViolation,
            duplicateCommitPrevented: primary.duplicateCommitPrevented,
            maximumCaptureToCommitLatencyMilliseconds:
                primary.maximumCaptureToCommitLatencyMilliseconds,
            activeContactCount: max(
                state.activeContactCount, primaryTouch.activeContactCount
            ),
            pendingComparisonCount:
                state.pendingComparisonCount + primaryTouch.pendingContactCount,
            isSettled: state.isSettled
                && primaryTouch.activeContactCount == 0
                && primaryTouch.pendingContactCount == 0
        )
    }

    func observeTouchDiagnostics(
        _ observer: (@MainActor (KeyboardTouchDiagnosticSnapshot) -> Void)?
    ) {
        touchShadow.trace.observe { [weak self] _ in
            guard let self else { return }
            observer?(touchDiagnosticSnapshot)
        }
        primaryTouch.observe { [weak self] _ in
            guard let self else { return }
            observer?(touchDiagnosticSnapshot)
        }
    }

    @discardableResult
    func resetTouchDiagnosticsIfIdle() -> Bool {
        guard primaryTouch.activeContactCount == 0,
              touchShadow.resetDiagnosticsIfIdle() else { return false }
        primaryTouch.reset()
        return true
    }
}
#endif
