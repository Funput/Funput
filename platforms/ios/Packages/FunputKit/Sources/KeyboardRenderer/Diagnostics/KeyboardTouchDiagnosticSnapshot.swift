#if DEBUG
public struct KeyboardTouchDiagnosticSnapshot: Equatable, Sendable {
    public let capturedBegan: Int
    public let shadowResolved: Int
    public let legacyReleased: Int
    public let matched: Int
    public let orderMismatch: Int
    public let legacyMissing: Int
    public let shadowMissing: Int
    public let legacyLate: Int
    public let shadowLate: Int
    public let shadowCancelled: Int
    public let legacyCancelled: Int
    public let cancelledSystem: Int
    public let cancelledTapSlop: Int
    public let recoveredTapSlop: Int
    public let cancelledDuration: Int
    public let cancelledOutside: Int
    public let timestampTie: Int
    public let unknownCallback: Int
    public let captureUnknownCallback: Int
    public let resolverUnknownCallback: Int
    public let outOfScopeCallback: Int
    public let layoutChangedWhileActive: Int
    public let droppedForCapacity: Int
    public let maximumConcurrentContacts: Int
    public let maximumArbiterDepth: Int
    public let arbiterBypassCount: Int
    public let maximumEmissionDelayMilliseconds: Int
    public let emissionDelayedOver40Milliseconds: Int
    public let emissionDelayedOver120Milliseconds: Int
    public let activeContactCount: Int
    public let pendingComparisonCount: Int
    public let isSettled: Bool

    public var cancellationDisagreement: Int {
        abs(shadowCancelled - legacyCancelled)
    }
}
#endif
