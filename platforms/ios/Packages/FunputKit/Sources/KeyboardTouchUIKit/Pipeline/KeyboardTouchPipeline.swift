import Foundation
import KeyboardLayout
import KeyboardTouchCore

@MainActor
public final class KeyboardTouchPipeline {
    public typealias EmissionHandler = @MainActor (
        PressEmission<KeyboardTouchAction>
    ) -> Void
    public typealias ResolutionHandler = @MainActor (
        ContactID, TimeInterval
    ) -> Void

    let policy: KeyboardTouchRecoveryPolicy
    var resolver: ContactResolver<KeyboardTouchHit>
    var currentGeometry: KeyboardGeometrySnapshot?
    var geometries: [ContactID: KeyboardGeometrySnapshot] = [:]
    var initialHits: [ContactID: KeyboardTouchHit] = [:]
    var counters = KeyboardTouchPipelineStatistics()
    let onResolved: ResolutionHandler
    private let clock: PressArbiterDriver<KeyboardTouchAction>.Clock
    private let schedule: DeadlineSchedule
    private var nextGeometryRevision: UInt64 = 1
    private let onEmit: EmissionHandler
    private let onStaleDeadline: @MainActor () -> Void
    private let arbiterConfiguration: PressArbiterConfiguration
    lazy var arbiter = PressArbiterDriver<KeyboardTouchAction>(
        configuration: arbiterConfiguration,
        clock: clock,
        schedule: schedule,
        onStaleDeadline: onStaleDeadline,
        onEmit: onEmit
    )

    public init(
        policy: KeyboardTouchRecoveryPolicy,
        resolverConfiguration: ContactResolverConfiguration = .default,
        arbiterConfiguration: PressArbiterConfiguration = .default,
        clock: @escaping PressArbiterDriver<KeyboardTouchAction>.Clock = {
            ProcessInfo.processInfo.systemUptime
        },
        schedule: @escaping DeadlineSchedule = RunLoopDeadlineScheduler.schedule,
        onResolved: @escaping ResolutionHandler = { _, _ in },
        onStaleDeadline: @escaping @MainActor () -> Void = {},
        onEmit: @escaping EmissionHandler
    ) {
        self.policy = policy
        self.arbiterConfiguration = arbiterConfiguration
        self.clock = clock
        self.schedule = schedule
        self.onResolved = onResolved
        self.onStaleDeadline = onStaleDeadline
        self.onEmit = onEmit
        resolver = ContactResolver(configuration: resolverConfiguration)
    }

    // Gauges: what is happening right now, so they are read through rather than accumulated.
    public var activeContactCount: Int { resolver.activeContactCount }
    public var orderedContactCount: Int { arbiter.orderedContactCount }
    public var heldContactCount: Int { arbiter.heldContactCount }

    /// Everything accumulated this session, including the arbiter's own counters.
    public var statistics: KeyboardTouchPipelineStatistics {
        var value = counters
        value.arbiter = arbiter.statistics
        return value
    }

    /// The snapshot new contacts hit-test against. The capture view borrows it so both sides
    /// share one revision instead of building a second copy of the same geometry.
    public var geometrySnapshot: KeyboardGeometrySnapshot? { currentGeometry }

    public func initialHit(for contactID: ContactID) -> KeyboardTouchHit? {
        initialHits[contactID]
    }

    @discardableResult
    public func updateGeometry(_ geometry: ResolvedKeyboard) -> Bool {
        guard currentGeometry?.geometry != geometry else { return false }
        precondition(nextGeometryRevision < UInt64.max)
        currentGeometry = KeyboardGeometrySnapshot(
            revision: nextGeometryRevision,
            geometry: geometry
        )
        nextGeometryRevision += 1
        return true
    }

}
