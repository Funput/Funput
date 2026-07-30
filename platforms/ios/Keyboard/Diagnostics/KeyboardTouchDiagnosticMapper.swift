#if DEBUG
import FunputShared
import KeyboardRenderer

extension KeyboardTouchDiagnosticMetrics {
    init(_ snapshot: KeyboardRenderer.KeyboardTouchDiagnosticSnapshot) {
        self.init()
        capturedBegan = snapshot.capturedBegan
        shadowResolved = snapshot.shadowResolved
        outputReleased = snapshot.outputReleased
        matched = snapshot.matched
        orderMismatch = snapshot.orderMismatch
        outputMissing = snapshot.outputMissing
        shadowMissing = snapshot.shadowMissing
        outputLate = snapshot.outputLate
        shadowLate = snapshot.shadowLate
        shadowCancelled = snapshot.shadowCancelled
        outputCancelled = snapshot.outputCancelled
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
        v2Committed = snapshot.v2Committed
        systemCancelled = snapshot.systemCancelled
        ownershipViolation = snapshot.ownershipViolation
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
