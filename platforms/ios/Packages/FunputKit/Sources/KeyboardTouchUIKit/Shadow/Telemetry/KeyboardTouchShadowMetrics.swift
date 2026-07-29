public struct KeyboardTouchShadowMetrics: Equatable, Sendable {
    public internal(set) var capturedBegan = 0
    public internal(set) var shadowResolved = 0
    public internal(set) var legacyReleased = 0
    public internal(set) var matched = 0
    public internal(set) var orderMismatch = 0
    public internal(set) var legacyMissing = 0
    public internal(set) var shadowMissing = 0
    public internal(set) var legacyLate = 0
    public internal(set) var shadowLate = 0
    public internal(set) var shadowCancelled = 0
    public internal(set) var legacyCancelled = 0
    public internal(set) var cancelledSystem = 0
    public internal(set) var cancelledTapSlop = 0
    public internal(set) var recoveredTapSlop = 0
    public internal(set) var cancelledDuration = 0
    public internal(set) var cancelledOutside = 0
    public internal(set) var captureUnknownCallback = 0
    public internal(set) var resolverUnknownCallback = 0
    public internal(set) var outOfScopeCallback = 0
    public internal(set) var timestampTie = 0
    public internal(set) var layoutChangedWhileActive = 0
    public internal(set) var droppedForCapacity = 0
    public internal(set) var maximumConcurrentContacts = 0
    public internal(set) var maximumArbiterDepth = 0
    public internal(set) var arbiterBypassCount = 0
    public internal(set) var maximumEmissionDelayMilliseconds = 0
    public internal(set) var emissionDelayedOver40Milliseconds = 0
    public internal(set) var emissionDelayedOver120Milliseconds = 0

    public init() {}

    public var unknownCallback: Int {
        captureUnknownCallback + resolverUnknownCallback
    }

    public var cancellationDisagreement: Int {
        abs(shadowCancelled - legacyCancelled)
    }
}
