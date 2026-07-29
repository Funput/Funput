#if DEBUG
public struct KeyboardTouchDiagnosticMetrics: Codable, Equatable, Sendable {
    public var capturedBegan = 0
    public var shadowResolved = 0
    public var legacyReleased = 0
    public var matched = 0
    public var orderMismatch = 0
    public var legacyMissing = 0
    public var shadowMissing = 0
    public var legacyLate = 0
    public var shadowLate = 0
    public var shadowCancelled = 0
    public var legacyCancelled = 0
    public var cancelledSystem = 0
    public var cancelledTapSlop = 0
    public var recoveredTapSlop = 0
    public var cancelledDuration = 0
    public var cancelledOutside = 0
    public var timestampTie = 0
    public var unknownCallback = 0
    public var captureUnknownCallback = 0
    public var resolverUnknownCallback = 0
    public var outOfScopeCallback = 0
    public var layoutChangedWhileActive = 0
    public var droppedForCapacity = 0
    public var maximumConcurrentContacts = 0
    public var maximumArbiterDepth = 0
    public var arbiterBypassCount = 0
    public var maximumEmissionDelayMilliseconds = 0
    public var emissionDelayedOver40Milliseconds = 0
    public var emissionDelayedOver120Milliseconds = 0
    public var primaryCommitted = 0
    public var legacyFallback = 0
    public var legacyReleaseSuppressed = 0
    public var primarySystemCancelled = 0
    public var commitGateViolation = 0
    public var duplicateCommitPrevented = 0
    public var maximumCaptureToCommitLatencyMilliseconds = 0

    public init() {}

    public var cancellationDisagreement: Int {
        abs(shadowCancelled - legacyCancelled)
    }

    public var hasShadowRegression: Bool {
        shadowMissing > 0 || orderMismatch > 0 || unknownCallback > 0
            || droppedForCapacity > 0 || cancellationDisagreement > 0
            || emissionDelayedOver120Milliseconds > 0
            || primarySystemCancelled > 0 || commitGateViolation > 0
    }
}
#endif
