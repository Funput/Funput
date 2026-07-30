#if canImport(UIKit) && DEBUG
import KeyboardTouchUIKit

public extension KeyboardSurfaceView {
    var touchDiagnosticSnapshot: KeyboardTouchDiagnosticSnapshot {
        let state = touchShadow.diagnosticState
        let metrics = state.metrics
        let v2 = v2Touch.metrics
        return KeyboardTouchDiagnosticSnapshot(
            capturedBegan: metrics.capturedBegan,
            shadowResolved: metrics.shadowResolved,
            outputReleased: metrics.outputReleased,
            matched: metrics.matched,
            orderMismatch: metrics.orderMismatch,
            outputMissing: metrics.outputMissing,
            shadowMissing: metrics.shadowMissing,
            outputLate: metrics.outputLate,
            shadowLate: metrics.shadowLate,
            shadowCancelled: metrics.shadowCancelled,
            outputCancelled: metrics.outputCancelled,
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
            v2Committed: v2.v2Committed,
            systemCancelled: v2.systemCancelled,
            ownershipViolation: v2.ownershipViolation,
            maximumCaptureToCommitLatencyMilliseconds:
                v2.maximumCaptureToCommitLatencyMilliseconds,
            releaseCommitted: v2.releaseCommitted,
            repeatEmitted: v2.repeatEmitted,
            alternateCommitted: v2.alternateCommitted,
            swipeCommitted: v2.swipeCommitted,
            controlCommitted: v2.controlCommitted,
            gestureConflict: v2.gestureConflict,
            staleTimerCallback: v2.staleTimerCallback,
            maximumTerminalToEmissionLatencyMilliseconds:
                v2.maximumTerminalToEmissionLatencyMilliseconds,
            activeContactCount: max(
                state.activeContactCount, v2Touch.activeContactCount
            ),
            pendingComparisonCount:
                state.pendingComparisonCount + v2Touch.pendingContactCount,
            isSettled: state.isSettled
                && v2Touch.activeContactCount == 0
                && v2Touch.pendingContactCount == 0
        )
    }

    func observeTouchDiagnostics(
        _ observer: (@MainActor (KeyboardTouchDiagnosticSnapshot) -> Void)?
    ) {
        touchShadow.trace.observe { [weak self] _ in
            guard let self else { return }
            observer?(touchDiagnosticSnapshot)
        }
        v2Touch.observe { [weak self] _ in
            guard let self else { return }
            observer?(touchDiagnosticSnapshot)
        }
    }

    @discardableResult
    func resetTouchDiagnosticsIfIdle() -> Bool {
        guard v2Touch.activeContactCount == 0,
              touchShadow.resetDiagnosticsIfIdle() else { return false }
        v2Touch.reset()
        return true
    }
}
#endif
