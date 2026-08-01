import Foundation

@MainActor
public final class PressArbiterDriver<Payload: Sendable> {
    public typealias Clock = @MainActor () -> TimeInterval
    public typealias EmissionHandler = @MainActor (PressEmission<Payload>) -> Void

    private var arbiter: PressArbiter<Payload>
    private let clock: Clock
    private let schedule: DeadlineSchedule
    private let onEmit: EmissionHandler
    private let onStaleDeadline: @MainActor () -> Void
    private var scheduledAction: ScheduledDeadline?
    private var scheduledDeadline: TimeInterval?
    private var scheduleGeneration: UInt64 = 0

    public init(
        configuration: PressArbiterConfiguration = .default,
        clock: @escaping Clock = { ProcessInfo.processInfo.systemUptime },
        schedule: @escaping DeadlineSchedule = RunLoopDeadlineScheduler.schedule,
        onStaleDeadline: @escaping @MainActor () -> Void = {},
        onEmit: @escaping EmissionHandler
    ) {
        arbiter = PressArbiter(configuration: configuration)
        self.clock = clock
        self.schedule = schedule
        self.onStaleDeadline = onStaleDeadline
        self.onEmit = onEmit
    }

    @discardableResult
    public func begin(_ contactID: ContactID) -> Bool {
        begin(contactID, at: clock())
    }

    @discardableResult
    public func begin(_ contactID: ContactID, at timestamp: TimeInterval) -> Bool {
        let inserted = arbiter.begin(contactID, at: timestamp)
        refreshSchedule()
        return inserted
    }

    public func resolve(_ contactID: ContactID, payload: Payload) {
        resolve(contactID, payload: payload, at: clock())
    }

    public func resolve(
        _ contactID: ContactID,
        payload: Payload,
        at timestamp: TimeInterval
    ) {
        process(arbiter.resolve(contactID, payload: payload, at: timestamp))
    }

    public func cancel(_ contactID: ContactID) {
        cancel(contactID, at: clock())
    }

    public func cancel(_ contactID: ContactID, at timestamp: TimeInterval) {
        process(arbiter.cancel(contactID, at: timestamp))
    }

    public func reset() {
        scheduleGeneration &+= 1
        scheduledAction?.cancel()
        scheduledAction = nil
        scheduledDeadline = nil
        arbiter.reset()
    }

    /// Commits presses whose finger already lifted, then returns how many were emitted.
    /// Call this before `reset()` so a teardown never swallows a completed press.
    @discardableResult
    public func flushResolved() -> Int {
        let emissions = arbiter.flushResolved()
        process(emissions)
        return emissions.count
    }

    public var orderedContactCount: Int { arbiter.ordered.count }
    public var heldContactCount: Int { arbiter.detached.count }
    public var statistics: PressArbiterStatistics { arbiter.statistics }

    private func process(_ emissions: [PressEmission<Payload>]) {
        emissions.forEach(onEmit)
        refreshSchedule()
    }

    private func refreshSchedule() {
        guard arbiter.nextDeadline != scheduledDeadline else { return }
        scheduleGeneration &+= 1
        scheduledAction?.cancel()
        scheduledAction = nil
        scheduledDeadline = arbiter.nextDeadline
        guard let deadline = scheduledDeadline else { return }
        let generation = scheduleGeneration
        scheduledAction = schedule(max(0, deadline - clock())) { [weak self] in
            self?.deadlineFired(generation: generation)
        }
    }

    private func deadlineFired(generation: UInt64) {
        guard generation == scheduleGeneration else {
            onStaleDeadline()
            return
        }
        scheduledAction = nil
        scheduledDeadline = nil
        process(arbiter.advance(to: clock()))
    }
}
