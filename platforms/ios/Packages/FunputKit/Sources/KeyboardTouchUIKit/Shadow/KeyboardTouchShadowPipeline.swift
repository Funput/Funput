import Foundation
import KeyboardLayout
import KeyboardTouchCore

@MainActor
public final class KeyboardTouchShadowPipeline {
    public let trace: KeyboardTouchShadowTrace

    private let configuration: KeyboardTouchShadowConfiguration
    private let clock: PressArbiterDriver<ShadowKeyIdentity>.Clock
    private let schedule: DeadlineSchedule
    private var resolver: ContactResolver<ShadowKeyIdentity>
    private var comparator: KeyboardTouchShadowComparator
    private var currentGeometry: KeyboardGeometrySnapshot?
    private var geometries: [ContactID: KeyboardGeometrySnapshot] = [:]
    private var beganAt: [ContactID: TimeInterval] = [:]
    private var tiedContacts: Set<ContactID> = []
    private var nextGeometryRevision: UInt64 = 1
    private lazy var arbiter = PressArbiterDriver<ShadowKeyIdentity>(
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
        case let .resolved(id, payload):
            finish(id)
            arbiter.resolve(id, payload: payload, at: sample.timestamp)
        case let .cancelled(id):
            finish(id)
            trace.record(.shadowCancelled)
            arbiter.cancel(id, at: sample.timestamp)
        case let .noOp(reason):
            if reason == .unknownContact { trace.record(.unknownCallback) }
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
        trace.record(.unknownCallback)
    }

    public func reset() {
        arbiter.reset()
        comparator.reset()
        resolver.reset()
        geometries.removeAll(keepingCapacity: true)
        beganAt.removeAll(keepingCapacity: true)
        tiedContacts.removeAll(keepingCapacity: true)
        currentGeometry = nil
    }

    private func recordTimestamp(_ sample: ContactSample) {
        for (otherID, timestamp) in beganAt where timestamp == sample.timestamp {
            tiedContacts.insert(otherID)
            tiedContacts.insert(sample.id)
        }
        beganAt[sample.id] = sample.timestamp
    }

    private func finish(_ id: ContactID) {
        geometries.removeValue(forKey: id)
        beganAt.removeValue(forKey: id)
    }

    private func recordShadow(_ emission: PressEmission<ShadowKeyIdentity>) {
        comparator.recordShadow(
            emission.payload,
            timestampTie: tiedContacts.remove(emission.contactID) != nil
        )
    }

    private static func isEligible(_ role: KeyRole) -> Bool {
        switch role {
        case .character, .vniModifier, .punctuation, .space: true
        default: false
        }
    }
}
