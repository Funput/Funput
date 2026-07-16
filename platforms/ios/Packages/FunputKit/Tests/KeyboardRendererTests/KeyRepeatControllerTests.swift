#if canImport(UIKit)
@testable import KeyboardRenderer
import Foundation
import Testing

@MainActor
struct KeyRepeatControllerTests {
    @Test("Quick tap does not enter repeat mode")
    func quickTap() {
        let scheduler = TestRepeatScheduler()
        var repeats = 0
        let controller = makeController(scheduler: scheduler) { repeats += 1 }

        controller.start()

        #expect(scheduler.delay == 0.4)
        #expect(controller.finish() == false)
        #expect(repeats == 0)
        #expect(scheduler.action == nil)
    }

    @Test("Hold repeats at interval and suppresses release")
    func holdRepeat() {
        let scheduler = TestRepeatScheduler()
        var repeats = 0
        let controller = makeController(scheduler: scheduler) { repeats += 1 }

        controller.start()
        scheduler.runNext()
        #expect(scheduler.delay == 0.05)
        scheduler.runNext()

        #expect(repeats == 2)
        #expect(controller.finish())
        #expect(scheduler.action == nil)
    }

    @Test("Cancelling stops a pending repeat")
    func cancel() {
        let scheduler = TestRepeatScheduler()
        var repeats = 0
        let controller = makeController(scheduler: scheduler) { repeats += 1 }

        controller.start()
        controller.cancel()
        scheduler.runNext()

        #expect(repeats == 0)
        #expect(controller.finish() == false)
    }

    private func makeController(
        scheduler: TestRepeatScheduler,
        onRepeat: @escaping () -> Void
    ) -> KeyRepeatController {
        KeyRepeatController(
            initialDelay: 0.4,
            repeatInterval: 0.05,
            schedule: scheduler.schedule,
            onRepeat: onRepeat
        )
    }
}

@MainActor
final class TestRepeatScheduler {
    var action: (@MainActor () -> Void)?
    var delay: TimeInterval?

    func schedule(
        delay: TimeInterval,
        action: @escaping @MainActor () -> Void
    ) -> KeyboardScheduledAction {
        self.delay = delay
        self.action = action
        return KeyboardScheduledAction { [weak self] in
            self?.action = nil
        }
    }

    func runNext() {
        let next = action
        action = nil
        next?()
    }
}
#endif
