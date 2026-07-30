#if DEBUG
import FunputShared
import KeyboardRenderer

extension KeyboardTouchDiagnosticMetrics {
    init(_ snapshot: KeyboardRenderer.KeyboardTouchDiagnosticSnapshot) {
        self.init()
        capturedBegan = snapshot.capturedBegan
        shadowResolved = snapshot.shadowResolved
        legacyReleased = snapshot.legacyReleased
        matched = snapshot.matched
        orderMismatch = snapshot.orderMismatch
        legacyMissing = snapshot.legacyMissing
        shadowMissing = snapshot.shadowMissing
        legacyLate = snapshot.legacyLate
        shadowLate = snapshot.shadowLate
        shadowCancelled = snapshot.shadowCancelled
        legacyCancelled = snapshot.legacyCancelled
        cancelledSystem = snapshot.cancelledSystem
        cancelledTapSlop = snapshot.cancelledTapSlop
        recoveredTapSlop = snapshot.recoveredTapSlop
        cancelledDuration = snapshot.cancelledDuration
        cancelledOutside = snapshot.cancelledOutside
        timestampTie = snapshot.timestampTie
        unknownCallback = snapshot.unknownCallback
        captureUnknownCallback = snapshot.captureUnknownCallback
        resolverUnknownCallback = snapshot.resolverUnknownCallback
        outOfScopeCallback = snapshot.outOfScopeCallback
        layoutChangedWhileActive = snapshot.layoutChangedWhileActive
        droppedForCapacity = snapshot.droppedForCapacity
        maximumConcurrentContacts = snapshot.maximumConcurrentContacts
        maximumArbiterDepth = snapshot.maximumArbiterDepth
        arbiterBypassCount = snapshot.arbiterBypassCount
        maximumEmissionDelayMilliseconds =
            snapshot.maximumEmissionDelayMilliseconds
        emissionDelayedOver40Milliseconds =
            snapshot.emissionDelayedOver40Milliseconds
        emissionDelayedOver120Milliseconds =
            snapshot.emissionDelayedOver120Milliseconds
        primaryCommitted = snapshot.primaryCommitted
        legacyFallback = snapshot.legacyFallback
        legacyReleaseSuppressed = snapshot.legacyReleaseSuppressed
        primarySystemCancelled = snapshot.primarySystemCancelled
        commitGateViolation = snapshot.commitGateViolation
        duplicateCommitPrevented = snapshot.duplicateCommitPrevented
        maximumCaptureToCommitLatencyMilliseconds =
            snapshot.maximumCaptureToCommitLatencyMilliseconds
        releaseCommitted = snapshot.releaseCommitted
        repeatEmitted = snapshot.repeatEmitted
        alternateCommitted = snapshot.alternateCommitted
        swipeCommitted = snapshot.swipeCommitted
        controlCommitted = snapshot.controlCommitted
        gestureConflict = snapshot.gestureConflict
        staleTimerCallback = snapshot.staleTimerCallback
        maximumTerminalToEmissionLatencyMilliseconds =
            snapshot.maximumTerminalToEmissionLatencyMilliseconds
    }
}
#endif
