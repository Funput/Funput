#if DEBUG
public struct KeyboardTouchDiagnosticSnapshot: Equatable, Sendable {
    public let capturedBegan: Int
    public let shadowResolved: Int
    public let outputReleased: Int
    public let matched: Int
    public let orderMismatch: Int
    public let outputMissing: Int
    public let shadowMissing: Int
    public let outputLate: Int
    public let shadowLate: Int
    public let shadowCancelled: Int
    public let outputCancelled: Int
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
    public let v2Committed: Int
    public let systemCancelled: Int
    public let ownershipViolation: Int
    public let maximumCaptureToCommitLatencyMilliseconds: Int
    public let releaseCommitted: Int
    public let repeatEmitted: Int
    public let alternateCommitted: Int
    public let swipeCommitted: Int
    public let controlCommitted: Int
    public let gestureConflict: Int
    public let staleTimerCallback: Int
    public let maximumTerminalToEmissionLatencyMilliseconds: Int
    public let activeContactCount: Int
    public let pendingComparisonCount: Int
    public let isSettled: Bool

    public var cancellationDisagreement: Int {
        abs(shadowCancelled - outputCancelled)
    }
}
#endif
