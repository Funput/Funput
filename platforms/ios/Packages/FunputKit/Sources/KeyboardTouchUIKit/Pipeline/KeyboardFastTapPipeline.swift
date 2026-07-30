import Foundation
import KeyboardLayout
import KeyboardTouchCore

@MainActor
public final class KeyboardFastTapPipeline {
    public typealias EmissionHandler = @MainActor (
        PressEmission<KeyboardTouchAction>
    ) -> Void
    public typealias ResolutionHandler = @MainActor (
        ContactID, TimeInterval
    ) -> Void

    private let eligibleRoles: Set<KeyRole>
    let recoveringTapSlopRoles: Set<KeyRole>
    private let clock: PressArbiterDriver<KeyboardTouchAction>.Clock
    private let schedule: DeadlineSchedule
    private var resolver: ContactResolver<KeyboardTouchHit>
    private var currentGeometry: KeyboardGeometrySnapshot?
    var geometries: [ContactID: KeyboardGeometrySnapshot] = [:]
    var initialHits: [ContactID: KeyboardTouchHit] = [:]
    private var nextGeometryRevision: UInt64 = 1
    private let onEmit: EmissionHandler
    let onResolved: ResolutionHandler
    lazy var arbiter = PressArbiterDriver<KeyboardTouchAction>(
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
        clock: @escaping PressArbiterDriver<KeyboardTouchAction>.Clock = {
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

    public func consume(_ sample: ContactSample) -> KeyboardFastTapDisposition {
        let geometry = sample.phase == .began
            ? currentGeometry : geometries[sample.id]
        let currentHit = geometry?.touchHit(at: sample.location)
        let hit = lockedHit(for: sample.id, current: currentHit)
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
        detach(contactID, at: timestamp)
    }

    @discardableResult
    public func claimForGesture(_ contactID: ContactID) -> Bool {
        resolver.discard(contactID)
    }

    @discardableResult
    public func resolveGesture(
        _ contactID: ContactID,
        action: KeyboardTouchAction,
        at timestamp: TimeInterval
    ) -> Bool {
        guard geometries.removeValue(forKey: contactID) != nil else { return false }
        initialHits.removeValue(forKey: contactID)
        arbiter.resolve(contactID, payload: action, at: timestamp)
        return true
    }

    @discardableResult
    public func detach(
        _ contactID: ContactID,
        at timestamp: TimeInterval
    ) -> Bool {
        let existed = resolver.discard(contactID)
            || geometries[contactID] != nil
        guard existed else { return false }
        geometries.removeValue(forKey: contactID)
        initialHits.removeValue(forKey: contactID)
        arbiter.cancel(contactID, at: timestamp)
        return true
    }

    public func reset() {
        arbiter.reset()
        resolver.reset()
        geometries.removeAll(keepingCapacity: true)
        initialHits.removeAll(keepingCapacity: true)
    }

    private func lockedHit(
        for id: ContactID,
        current: KeyboardTouchHit?
    ) -> KeyboardTouchHit? {
        guard let initial = initialHits[id],
              initial.key.horizontalSwipeAction != nil,
              current != nil else { return current }
        return initial
    }
}
