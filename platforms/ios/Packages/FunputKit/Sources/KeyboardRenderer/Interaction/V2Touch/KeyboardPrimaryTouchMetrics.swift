public struct KeyboardPrimaryTouchMetrics: Equatable, Sendable {
    public internal(set) var primaryCommitted = 0
    public internal(set) var legacyFallback = 0
    public internal(set) var legacyReleaseSuppressed = 0
    public internal(set) var primarySystemCancelled = 0
    public internal(set) var commitGateViolation = 0
    public internal(set) var duplicateCommitPrevented = 0
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
