import Foundation
import KeyboardLayout
import KeyboardTouchCore

@MainActor
public final class KeyboardTouchShadowPipeline {
    public let trace: KeyboardTouchShadowTrace

    private let configuration: KeyboardTouchShadowConfiguration
    let clock: PressArbiterDriver<ShadowKeyIdentity>.Clock
    private let schedule: DeadlineSchedule
    var comparator: KeyboardTouchShadowComparator
    var beganAt: [ContactID: TimeInterval] = [:]
    var resolvedAt: [ContactID: TimeInterval] = [:]
    var tiedContacts: Set<ContactID> = []
    private var ignoredContacts: Set<ContactID> = []
    lazy var fastTap = KeyboardFastTapPipeline(
        eligibleRoles: [.character, .vniModifier, .punctuation, .space],
        recoveringTapSlopRoles: [.character, .vniModifier, .punctuation],
        resolverConfiguration: configuration.resolver,
        arbiterConfiguration: configuration.arbiter,
        clock: clock,
        schedule: schedule,
        onResolved: { [weak self] id, timestamp in
            self?.resolvedAt[id] = timestamp
        }
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
        comparator = KeyboardTouchShadowComparator(
            configuration: configuration,
            clock: clock,
            schedule: schedule,
            trace: trace
        )
    }

    public var activeContactCount: Int { fastTap.activeContactCount }

    public func updateGeometry(_ geometry: ResolvedKeyboard) {
        let wasActive = activeContactCount > 0
        if fastTap.updateGeometry(geometry), wasActive {
            trace.record(.layoutChangedWhileActive)
        }
    }

    public func consume(_ sample: ContactSample) {
        if sample.phase != .began, ignoredContacts.contains(sample.id) {
            trace.record(.outOfScope)
            if sample.phase == .ended || sample.phase == .cancelled {
                ignoredContacts.remove(sample.id)
            }
            return
        }
        switch fastTap.consume(sample) {
        case .began:
            recordTimestamp(sample)
            trace.record(.capturedBegan)
            observeArbiterState()
        case let .resolved(id, metadata):
            finishTimestamp(id)
            if metadata.exceededTapSlop { trace.record(.recoveredTapSlop) }
            observeArbiterState()
        case let .fallback(id, reason):
            finishTimestamp(id)
            resolvedAt.removeValue(forKey: id)
            trace.recordCancellation(reason.contactReason)
            observeArbiterState()
        case let .cancelled(id):
            finishTimestamp(id)
            trace.recordCancellation(.system)
            observeArbiterState()
        case let .ignored(reason):
            if reason == .beganOutside {
                ignoredContacts.insert(sample.id)
            } else if reason == .unknownContact {
                trace.record(.resolverUnknown)
            }
        case .tracking:
            break
        }
    }

    public func recordLegacyRelease(_ key: KeySpec) {
        guard Self.isEligible(key.role),
              let identity = fastTap.identity(for: key) else { return }
        comparator.recordLegacy(identity)
    }

    public func recordLegacyCancellation(_ key: KeySpec) {
        guard Self.isEligible(key.role) else { return }
        trace.record(.legacyCancelled)
    }

    public func recordUnknownCaptureCallback() {
        trace.record(.captureUnknown)
    }

    public func promoteToLegacy(_ rawContactID: UInt64) {
        let id = ContactID(rawValue: rawContactID)
        guard fastTap.promoteToLegacy(id, at: clock()) else { return }
        finishTimestamp(id)
        resolvedAt.removeValue(forKey: id)
        ignoredContacts.insert(id)
        observeArbiterState()
    }

    public func reset() {
        fastTap.reset()
        comparator.reset()
        beganAt.removeAll(keepingCapacity: true)
        resolvedAt.removeAll(keepingCapacity: true)
        tiedContacts.removeAll(keepingCapacity: true)
        ignoredContacts.removeAll(keepingCapacity: true)
    }

    static func isEligible(_ role: KeyRole) -> Bool {
        switch role {
        case .character, .vniModifier, .punctuation, .space: true
        default: false
        }
    }
}

private extension KeyboardFastTapFallback {
    var contactReason: ContactCancellationReason {
        switch self {
        case .exceededDuration: .exceededDuration
        case .endedOutside: .endedOutside
        case .exceededTapSlop: .exceededTapSlop
        }
    }
}
