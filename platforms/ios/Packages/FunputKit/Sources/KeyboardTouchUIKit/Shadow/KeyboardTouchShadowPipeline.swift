import Foundation
import KeyboardLayout
import KeyboardTouchCore

@MainActor
public final class KeyboardTouchShadowPipeline {
    public let trace: KeyboardTouchShadowTrace

    private let configuration: KeyboardTouchShadowConfiguration
    let clock: PressArbiterDriver<ShadowKeyIdentity>.Clock
    private let schedule: DeadlineSchedule
    var resolver: ContactResolver<ShadowKeyIdentity>
    var comparator: KeyboardTouchShadowComparator
    private var currentGeometry: KeyboardGeometrySnapshot?
    var geometries: [ContactID: KeyboardGeometrySnapshot] = [:]
    var beganAt: [ContactID: TimeInterval] = [:]
    var resolvedAt: [ContactID: TimeInterval] = [:]
    var tiedContacts: Set<ContactID> = []
    private var ignoredContacts: Set<ContactID> = []
    private var nextGeometryRevision: UInt64 = 1
    lazy var arbiter = PressArbiterDriver<ShadowKeyIdentity>(
        configuration: configuration.arbiter,
        clock: clock,
        schedule: schedule
    ) { [weak self] emission in
        self?.recordShadow(emission)
    }

    public init(
        configuration: KeyboardTouchShadowConfiguration = .default,
        clock: @escaping PressArbiterDriver<ShadowKeyIdentity>.Clock = {
            ProcessInfo.processInfo.systemUptime
        },
        schedule: @escaping DeadlineSchedule = RunLoopDeadlineScheduler.schedule
    ) {
        self.configuration = configuration
        self.clock = clock
        self.schedule = schedule
        trace = KeyboardTouchShadowTrace()
        resolver = ContactResolver(configuration: configuration.resolver)
        comparator = KeyboardTouchShadowComparator(
            configuration: configuration,
            clock: clock,
            schedule: schedule,
            trace: trace
        )
    }

    public var activeContactCount: Int { resolver.activeContactCount }

    public func updateGeometry(_ geometry: ResolvedKeyboard) {
        guard currentGeometry?.geometry != geometry else { return }
        if activeContactCount > 0 { trace.record(.layoutChangedWhileActive) }
        precondition(nextGeometryRevision < UInt64.max)
        currentGeometry = KeyboardGeometrySnapshot(
            revision: nextGeometryRevision,
            geometry: geometry
        )
        nextGeometryRevision += 1
    }

    public func consume(_ sample: ContactSample) {
        if sample.phase != .began, ignoredContacts.contains(sample.id) {
            trace.record(.outOfScope)
            if sample.phase == .ended || sample.phase == .cancelled {
                ignoredContacts.remove(sample.id)
            }
            return
        }
        let geometry = sample.phase == .began
            ? currentGeometry : geometries[sample.id]
        let identity = geometry?.hit(at: sample.location)
        let eligibleHit = identity.flatMap { Self.isEligible($0.role) ? $0 : nil }
        let resolution = resolver.consume(sample, hit: eligibleHit)
        switch resolution {
        case .began:
            guard let geometry else { return }
            geometries[sample.id] = geometry
            recordTimestamp(sample)
            trace.record(.capturedBegan)
            _ = arbiter.begin(sample.id, at: sample.timestamp)
            observeArbiterState()
        case let .resolved(id, payload, metadata):
            finish(id)
            if metadata.exceededTapSlop, payload.role == .space {
                trace.recordCancellation(.exceededTapSlop)
                arbiter.cancel(id, at: sample.timestamp)
                observeArbiterState()
                return
            }
            if metadata.exceededTapSlop { trace.record(.recoveredTapSlop) }
            resolvedAt[id] = sample.timestamp
            arbiter.resolve(id, payload: payload, at: sample.timestamp)
            observeArbiterState()
        case let .cancelled(id, reason):
            finish(id)
            resolvedAt.removeValue(forKey: id)
            trace.recordCancellation(reason)
            arbiter.cancel(id, at: sample.timestamp)
            observeArbiterState()
        case let .noOp(reason):
            if reason == .beganOutside {
                ignoredContacts.insert(sample.id)
            } else if reason == .unknownContact {
                trace.record(.resolverUnknown)
            }
        }
    }

    public func recordLegacyRelease(_ key: KeySpec) {
        guard Self.isEligible(key.role),
              let identity = currentGeometry?.identity(for: key) else { return }
        comparator.recordLegacy(identity)
    }

    public func recordLegacyCancellation(_ key: KeySpec) {
        guard Self.isEligible(key.role) else { return }
        trace.record(.legacyCancelled)
    }

    public func recordUnknownCaptureCallback() {
        trace.record(.captureUnknown)
    }

    public func reset() {
        arbiter.reset()
        comparator.reset()
        resolver.reset()
        geometries.removeAll(keepingCapacity: true)
        beganAt.removeAll(keepingCapacity: true)
        resolvedAt.removeAll(keepingCapacity: true)
        tiedContacts.removeAll(keepingCapacity: true)
        ignoredContacts.removeAll(keepingCapacity: true)
        currentGeometry = nil
    }

    static func isEligible(_ role: KeyRole) -> Bool {
        switch role {
        case .character, .vniModifier, .punctuation, .space: true
        default: false
        }
    }
}
