public struct KeyboardTouchMetrics: Equatable, Sendable {
    public internal(set) var capturedContacts = 0
    public internal(set) var committedContacts = 0
    public internal(set) var cancelledContacts = 0
    public internal(set) var systemCancelled = 0
    public internal(set) var captureUnknownCallback = 0
    public internal(set) var resolverUnknownCallback = 0
    public internal(set) var beganOutside = 0
    public internal(set) var endedOutside = 0
    public internal(set) var recoveredTapSlop = 0
    public internal(set) var layoutChangedWhileActive = 0
    public internal(set) var timestampTieContacts = 0
    public internal(set) var maximumConcurrentContacts = 0
    public internal(set) var maximumArbiterDepth = 0
    public internal(set) var arbiterBypassCount = 0
    public internal(set) var ownershipViolation = 0
    public internal(set) var releaseCommitted = 0
    public internal(set) var repeatEmitted = 0
    public internal(set) var repeatClaimedContacts = 0
    public internal(set) var alternateCommitted = 0
    public internal(set) var swipeCommitted = 0
    public internal(set) var controlCommitted = 0
    public internal(set) var gestureConflict = 0
    public internal(set) var staleTimerCallback = 0
    public internal(set) var maximumCaptureToCommitLatencyMilliseconds = 0
    public internal(set) var maximumTerminalToEmissionLatencyMilliseconds = 0
    public internal(set) var emissionDelayedOver40Milliseconds = 0
    public internal(set) var emissionDelayedOver120Milliseconds = 0

    public init() {}

    public var hasRegression: Bool {
        captureUnknownCallback > 0 || resolverUnknownCallback > 0
            || systemCancelled > 0 || ownershipViolation > 0
            || gestureConflict > 0 || staleTimerCallback > 0
            || emissionDelayedOver120Milliseconds > 0
    }
}
