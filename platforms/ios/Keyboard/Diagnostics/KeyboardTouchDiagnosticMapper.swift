#if DEBUG
import FunputShared
import KeyboardRenderer

extension KeyboardTouchDiagnosticMetrics {
    init(_ snapshot: KeyboardTouchDiagnosticSnapshot) {
        let value = snapshot.metrics
        self.init()
        capturedContacts = value.capturedContacts
        committedContacts = value.committedContacts
        cancelledContacts = value.cancelledContacts
        systemCancelled = value.systemCancelled
        captureUnknownCallback = value.captureUnknownCallback
        resolverUnknownCallback = value.resolverUnknownCallback
        beganOutside = value.beganOutside
        endedOutside = value.endedOutside
        recoveredTapSlop = value.recoveredTapSlop
        layoutChangedWhileActive = value.layoutChangedWhileActive
        timestampTieContacts = value.timestampTieContacts
        maximumConcurrentContacts = value.maximumConcurrentContacts
        maximumArbiterDepth = value.maximumArbiterDepth
        arbiterBypassCount = value.arbiterBypassCount
        ownershipViolation = value.ownershipViolation
        releaseCommitted = value.releaseCommitted
        repeatEmitted = value.repeatEmitted
        repeatClaimedContacts = value.repeatClaimedContacts
        alternateCommitted = value.alternateCommitted
        swipeCommitted = value.swipeCommitted
        controlCommitted = value.controlCommitted
        gestureConflict = value.gestureConflict
        staleTimerCallback = value.staleTimerCallback
        maximumCaptureToCommitLatencyMilliseconds =
            value.maximumCaptureToCommitLatencyMilliseconds
        maximumTerminalToEmissionLatencyMilliseconds =
            value.maximumTerminalToEmissionLatencyMilliseconds
        emissionDelayedOver40Milliseconds =
            value.emissionDelayedOver40Milliseconds
        emissionDelayedOver120Milliseconds =
            value.emissionDelayedOver120Milliseconds
    }
}
#endif
