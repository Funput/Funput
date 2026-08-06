#if DEBUG
public struct KeyboardTouchDiagnosticMetrics: Codable, Equatable, Sendable {
    public var capturedContacts = 0
    public var committedContacts = 0
    public var cancelledContacts = 0
    public var systemCancelled = 0
    public var captureUnknownCallback = 0
    public var captureStaleIdentity = 0
    public var contactsAbandoned = 0
    public var resolverUnknownCallback = 0
    public var beganOutside = 0
    public var endedOutside = 0
    public var recoveredReleaseOutside = 0
    public var recoveredTapSlop = 0
    public var layoutChangedWhileActive = 0
    public var timestampTieContacts = 0
    public var maximumConcurrentContacts = 0
    public var maximumArbiterDepth = 0
    public var arbiterBypassCount = 0
    public var maximumBypassHoldMilliseconds = 0
    public var ownershipViolation = 0
    public var releaseCommitted = 0
    public var repeatEmitted = 0
    public var repeatClaimedContacts = 0
    public var alternateCommitted = 0
    public var swipeCommitted = 0
    public var controlCommitted = 0
    public var gestureConflict = 0
    public var staleTimerCallback = 0
    public var maximumCaptureToCommitLatencyMilliseconds = 0
    public var maximumTerminalToEmissionLatencyMilliseconds = 0
    public var emissionDelayedOver40Milliseconds = 0
    public var emissionDelayedOver120Milliseconds = 0

    public init() {}

    public var hasRegression: Bool {
        captureUnknownCallback > 0 || resolverUnknownCallback > 0
            || systemCancelled > 0 || ownershipViolation > 0
            || gestureConflict > 0 || staleTimerCallback > 0
            || emissionDelayedOver120Milliseconds > 0
            || contactsAbandoned > 0
    }
}
#endif
