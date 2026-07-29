import Foundation
import KeyboardLayout
import KeyboardTouchCore

@MainActor
public final class KeyboardFastTapPipeline {
    public typealias EmissionHandler = @MainActor (
        PressEmission<KeyboardTouchHit>
    ) -> Void
    public typealias ResolutionHandler = @MainActor (
        ContactID, TimeInterval
    ) -> Void

    private let eligibleRoles: Set<KeyRole>
    let recoveringTapSlopRoles: Set<KeyRole>
    private let clock: PressArbiterDriver<KeyboardTouchHit>.Clock
    private let schedule: DeadlineSchedule
    private var resolver: ContactResolver<KeyboardTouchHit>
    private var currentGeometry: KeyboardGeometrySnapshot?
    var geometries: [ContactID: KeyboardGeometrySnapshot] = [:]
    private var nextGeometryRevision: UInt64 = 1
    private let onEmit: EmissionHandler
    let onResolved: ResolutionHandler
    lazy var arbiter = PressArbiterDriver<KeyboardTouchHit>(
        configuration: arbiterConfiguration,
        clock: clock,
        schedule: schedule,
        onEmit: onEmit
    )
    private let arbiterConfiguration: PressArbiterConfiguration

    public init(
        eligibleRoles: Set<KeyRole>,
        recoveringTapSlopRoles: Set<KeyRole>,
        resolverConfiguration: ContactResolverConfiguration = .default,
        arbiterConfiguration: PressArbiterConfiguration = .default,
        clock: @escaping PressArbiterDriver<KeyboardTouchHit>.Clock = {
            ProcessInfo.processInfo.systemUptime
        },
        schedule: @escaping DeadlineSchedule = RunLoopDeadlineScheduler.schedule,
        onResolved: @escaping ResolutionHandler = { _, _ in },
        onEmit: @escaping EmissionHandler
    ) {
        self.eligibleRoles = eligibleRoles
        self.recoveringTapSlopRoles = recoveringTapSlopRoles
        self.arbiterConfiguration = arbiterConfiguration
        self.clock = clock
        self.schedule = schedule
        self.onResolved = onResolved
        self.onEmit = onEmit
        resolver = ContactResolver(configuration: resolverConfiguration)
    }

    public var activeContactCount: Int { resolver.activeContactCount }
    public var orderedContactCount: Int { arbiter.orderedContactCount }
    public var heldContactCount: Int { arbiter.heldContactCount }
    public var bypassedContactCount: UInt64 { arbiter.bypassedContactCount }

    public func identity(for key: KeySpec) -> ShadowKeyIdentity? {
        currentGeometry?.identity(for: key)
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

    public func consume(_ sample: ContactSample) -> KeyboardFastTapDisposition {
        let geometry = sample.phase == .began
            ? currentGeometry : geometries[sample.id]
        let hit = geometry?.touchHit(at: sample.location)
        let eligibleHit = hit.flatMap {
            eligibleRoles.contains($0.key.role) ? $0 : nil
        }
        let resolution = resolver.consume(sample, hit: eligibleHit)
        return handle(resolution, sample: sample, geometry: geometry)
    }

    @discardableResult
    public func promoteToLegacy(
        _ contactID: ContactID,
        at timestamp: TimeInterval
    ) -> Bool {
        guard resolver.discard(contactID) else { return false }
        geometries.removeValue(forKey: contactID)
        arbiter.cancel(contactID, at: timestamp)
        return true
    }

    public func reset() {
        arbiter.reset()
        resolver.reset()
        geometries.removeAll(keepingCapacity: true)
    }
}
