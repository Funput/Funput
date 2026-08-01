#if DEBUG
import Foundation
import FunputShared
import KeyboardLayout
import Testing

@MainActor
struct KeyboardTouchDiagnosticPublisherTests {
    @Test("One to one thousand updates are coalesced")
    func coalesces() {
        let clock = PublisherTestClock()
        var reports: [KeyboardTouchDiagnosticReport] = []
        let publisher = makePublisher(clock: clock) { reports.append($0) }

        for value in 1 ... 1_000 {
            var metrics = KeyboardTouchDiagnosticMetrics()
            metrics.capturedContacts = value
            publisher.submit(
                metrics: metrics,
                activeContactCount: 0,
                pendingContactCount: 0,
                isSettled: true
            )
        }
        #expect(reports.isEmpty)
        clock.advance(by: 0)
        #expect(reports.count == 1)
        #expect(reports[0].metrics.capturedContacts == 1_000)
    }

    @Test("Final flush writes immediately and cancels stale timer")
    func finalFlush() {
        let clock = PublisherTestClock()
        var reports: [KeyboardTouchDiagnosticReport] = []
        let publisher = makePublisher(clock: clock) { reports.append($0) }
        publisher.submit(
            metrics: .init(),
            activeContactCount: 1,
            pendingContactCount: 2,
            isSettled: false
        )
        publisher.finalFlush()
        clock.advance(by: 1)

        #expect(reports.count == 1)
        #expect(reports[0].activeContactCount == 1)
        #expect(reports[0].pendingContactCount == 2)
    }

    @Test("Invalidation drops pending callback")
    func invalidation() {
        let clock = PublisherTestClock()
        var reports: [KeyboardTouchDiagnosticReport] = []
        let publisher = makePublisher(clock: clock) { reports.append($0) }
        publisher.submit(
            metrics: .init(),
            activeContactCount: 0,
            pendingContactCount: 0,
            isSettled: true
        )
        publisher.invalidate()
        clock.advance(by: 1)
        #expect(reports.isEmpty)
    }

    private func makePublisher(
        clock: PublisherTestClock,
        writer: @escaping @MainActor (KeyboardTouchDiagnosticReport) -> Void
    ) -> KeyboardTouchDiagnosticPublisher {
        let now = clock.now
        let session = KeyboardTouchDiagnosticSession(
            inputMethod: .vni,
            phase: .guided,
            generation: 1,
            startedAt: now
        )
        return KeyboardTouchDiagnosticPublisher(
            session: session,
            device: .init(model: "test", operatingSystem: "test", maximumFramesPerSecond: 60),
            clock: { clock.now },
            schedule: { delay, action in clock.schedule(delay: delay, action: action) },
            writer: writer
        )
    }
}

@MainActor
private final class PublisherTestClock {
    struct Job {
        let id: UInt64
        let date: Date
        let action: @MainActor () -> Void
    }

    var now = Date(timeIntervalSince1970: 1_000)
    private var nextID: UInt64 = 0
    private var jobs: [Job] = []

    func schedule(
        delay: TimeInterval,
        action: @escaping @MainActor () -> Void
    ) -> KeyboardTouchDiagnosticDeadline {
        nextID &+= 1
        let id = nextID
        jobs.append(Job(id: id, date: now.addingTimeInterval(delay), action: action))
        return KeyboardTouchDiagnosticDeadline { [weak self] in
            self?.jobs.removeAll { $0.id == id }
        }
    }

    func advance(by interval: TimeInterval) {
        now.addTimeInterval(interval)
        while let index = jobs.firstIndex(where: { $0.date <= now }) {
            jobs.remove(at: index).action()
        }
    }
}
#endif
