#if DEBUG
import Foundation

@MainActor
public final class KeyboardTouchDiagnosticDeadline {
    private var cancellation: (@MainActor () -> Void)?

    public init(_ cancellation: @escaping @MainActor () -> Void) {
        self.cancellation = cancellation
    }

    public func cancel() {
        cancellation?()
        cancellation = nil
    }
}

@MainActor
public final class KeyboardTouchDiagnosticPublisher {
    public typealias Clock = @MainActor () -> Date
    public typealias Schedule = @MainActor (
        TimeInterval,
        @escaping @MainActor () -> Void
    ) -> KeyboardTouchDiagnosticDeadline
    public typealias Writer = @MainActor (KeyboardTouchDiagnosticReport) -> Void

    private struct State {
        let metrics: KeyboardTouchDiagnosticMetrics
        let active: Int
        let pending: Int
        let settled: Bool
    }

    private let session: KeyboardTouchDiagnosticSession
    private let device: KeyboardTouchDiagnosticDevice
    private let interval: TimeInterval
    private let clock: Clock
    private let schedule: Schedule
    private let writer: Writer
    private var latest: State?
    private var deadline: KeyboardTouchDiagnosticDeadline?
    private var lastWrite: Date?
    private var sequence: UInt64 = 0
    private var generation: UInt64 = 0

    public init(
        session: KeyboardTouchDiagnosticSession,
        device: KeyboardTouchDiagnosticDevice,
        interval: TimeInterval = 0.250,
        clock: @escaping Clock = Date.init,
        schedule: @escaping Schedule = KeyboardTouchDiagnosticPublisher.scheduleOnMain,
        writer: @escaping Writer
    ) {
        self.session = session
        self.device = device
        self.interval = interval
        self.clock = clock
        self.schedule = schedule
        self.writer = writer
    }

    public func submit(
        metrics: KeyboardTouchDiagnosticMetrics,
        activeContactCount: Int,
        pendingContactCount: Int,
        isSettled: Bool
    ) {
        latest = State(
            metrics: metrics,
            active: activeContactCount,
            pending: pendingContactCount,
            settled: isSettled
        )
        guard deadline == nil else { return }
        let elapsed = lastWrite.map { clock().timeIntervalSince($0) } ?? interval
        arm(after: max(0, interval - elapsed))
    }

    public func finalFlush() {
        deadline?.cancel()
        deadline = nil
        publishLatest()
    }

    public func invalidate() {
        generation &+= 1
        deadline?.cancel()
        deadline = nil
        latest = nil
    }

    private func arm(after delay: TimeInterval) {
        generation &+= 1
        let expected = generation
        deadline = schedule(delay) { [weak self] in
            guard let self, generation == expected else { return }
            deadline = nil
            publishLatest()
        }
    }

    private func publishLatest() {
        guard let state = latest else { return }
        latest = nil
        sequence &+= 1
        let now = clock()
        lastWrite = now
        writer(KeyboardTouchDiagnosticReport(
            sessionID: session.id,
            generation: session.generation,
            sequence: sequence,
            observedAt: now,
            metrics: state.metrics,
            activeContactCount: state.active,
            pendingContactCount: state.pending,
            isSettled: state.settled,
            device: device
        ))
    }

    public static func scheduleOnMain(
        delay: TimeInterval,
        action: @escaping @MainActor () -> Void
    ) -> KeyboardTouchDiagnosticDeadline {
        let work = DispatchWorkItem { MainActor.assumeIsolated(action) }
        DispatchQueue.main.asyncAfter(deadline: .now() + delay, execute: work)
        return KeyboardTouchDiagnosticDeadline { work.cancel() }
    }
}
#endif
