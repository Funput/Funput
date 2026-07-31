import KeyboardTouchCore

/// What the pipeline accumulated over a session.
///
/// Only counters belong here. Gauges such as `activeContactCount` describe the current moment
/// rather than the session, so they stay computed on the pipeline itself.
///
/// Adding a measurement means a field here and the line that increments it — not another
/// property threaded through the driver, the pipeline and the coordinator.
public struct KeyboardTouchPipelineStatistics: Equatable, Sendable {
    /// Releases that landed outside the tracked geometry, recovered or not.
    public internal(set) var releasesOutside = 0

    /// The subset of `releasesOutside` that the recovery policy kept as real presses.
    public internal(set) var recoveredReleasesOutside = 0

    /// Filled from the arbiter on read; it owns its own counters.
    public internal(set) var arbiter = PressArbiterStatistics()

    public init() {}

    mutating func recordReleaseOutside(recovered: Bool) {
        releasesOutside += 1
        if recovered { recoveredReleasesOutside += 1 }
    }
}
