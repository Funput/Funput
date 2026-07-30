public struct KeyboardV2TouchMetrics: Equatable, Sendable {
    public internal(set) var v2Committed = 0
    public internal(set) var systemCancelled = 0
    public internal(set) var ownershipViolation = 0
    public internal(set) var maximumCaptureToCommitLatencyMilliseconds = 0
    public internal(set) var releaseCommitted = 0
    public internal(set) var repeatEmitted = 0
    public internal(set) var alternateCommitted = 0
    public internal(set) var swipeCommitted = 0
    public internal(set) var controlCommitted = 0
    public internal(set) var gestureConflict = 0
    public internal(set) var staleTimerCallback = 0
    public internal(set) var maximumTerminalToEmissionLatencyMilliseconds = 0

    public init() {}
}
