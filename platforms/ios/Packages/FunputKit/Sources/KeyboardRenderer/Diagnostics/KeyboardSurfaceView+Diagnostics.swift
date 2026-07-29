#if canImport(UIKit) && DEBUG
import KeyboardTouchUIKit

public extension KeyboardSurfaceView {
    var touchDiagnosticSnapshot: KeyboardTouchDiagnosticSnapshot {
        let state = touchShadow.diagnosticState
        let metrics = state.metrics
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
            activeContactCount: state.activeContactCount,
            pendingComparisonCount: state.pendingComparisonCount,
            isSettled: state.isSettled
        )
    }

    func observeTouchDiagnostics(
        _ observer: (@MainActor (KeyboardTouchDiagnosticSnapshot) -> Void)?
    ) {
        touchShadow.trace.observe { [weak self] _ in
            guard let self else { return }
            observer?(touchDiagnosticSnapshot)
        }
    }

    @discardableResult
    func resetTouchDiagnosticsIfIdle() -> Bool {
        touchShadow.resetDiagnosticsIfIdle()
    }
}
#endif
