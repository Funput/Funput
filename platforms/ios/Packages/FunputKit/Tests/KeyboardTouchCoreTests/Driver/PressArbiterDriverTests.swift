import KeyboardTouchCore
import Testing

@MainActor
struct PressArbiterDriverTests {
    @Test("The injected scheduler advances a blocked follower")
    func scheduledProgress() {
        let scheduler = TestDeadlineScheduler()
        var output: [String] = []
        let driver = makeDriver(scheduler) { output.append($0.payload) }
        driver.begin(contact(1))
        driver.begin(contact(2))
        driver.resolve(contact(2), payload: "B")

        #expect(output.isEmpty)
        #expect(scheduler.pendingCount == 1)
        scheduler.runNext()
        driver.resolve(contact(1), payload: "A")

        #expect(output == ["B", "A"])
    }

    @Test("A cancelled timer callback cannot duplicate output")
    func staleTimerDoesNotEmit() {
        let scheduler = TestDeadlineScheduler()
        var output: [String] = []
        let driver = makeDriver(scheduler) { output.append($0.payload) }
        driver.begin(contact(1))
        driver.begin(contact(2))
        driver.resolve(contact(2), payload: "B")
        scheduler.now = 0.02
        driver.resolve(contact(1), payload: "A")

        #expect(output == ["A", "B"])
        scheduler.fireIncludingCancelled(at: 0)
        #expect(output == ["A", "B"])
    }

    @Test("Reset cancels scheduled work and ignores its stale callback")
    func resetCancelsDeadline() {
        let scheduler = TestDeadlineScheduler()
        var output: [String] = []
        let driver = makeDriver(scheduler) { output.append($0.payload) }
        driver.begin(contact(1))
        driver.begin(contact(2))
        driver.resolve(contact(2), payload: "B")

        driver.reset()
        #expect(scheduler.pendingCount == 0)
        scheduler.fireIncludingCancelled(at: 0)

        #expect(output.isEmpty)
    }

    private func makeDriver(
        _ scheduler: TestDeadlineScheduler,
        onEmit: @escaping @MainActor (PressEmission<String>) -> Void
    ) -> PressArbiterDriver<String> {
        PressArbiterDriver(
            configuration: PressArbiterConfiguration(rolloverWindow: 0.04),
            clock: { scheduler.now },
            schedule: scheduler.schedule,
            onEmit: onEmit
        )
    }
}
