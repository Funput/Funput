#if canImport(UIKit)
import Foundation

@MainActor
final class KeyboardScheduledAction {
    private var cancellation: (() -> Void)?

    init(cancellation: @escaping () -> Void) {
        self.cancellation = cancellation
    }

    func cancel() {
        cancellation?()
        cancellation = nil
    }
}

@MainActor
final class BackspaceRepeatController {
    typealias Scheduler = @MainActor (
        _ delay: TimeInterval,
        _ action: @escaping @MainActor () -> Void
    ) -> KeyboardScheduledAction

    private let schedule: Scheduler
    private let onRepeat: () -> Void
    private let initialDelay: TimeInterval
    private let repeatInterval: TimeInterval
    private var scheduledAction: KeyboardScheduledAction?
    private var isActive = false
    private(set) var hasRepeated = false

    init(
        initialDelay: TimeInterval = 0.4,
        repeatInterval: TimeInterval = 0.05,
        schedule: @escaping Scheduler = BackspaceRepeatController.schedule,
        onRepeat: @escaping () -> Void
    ) {
        precondition(initialDelay > 0)
        precondition(repeatInterval > 0)
        self.initialDelay = initialDelay
        self.repeatInterval = repeatInterval
        self.schedule = schedule
        self.onRepeat = onRepeat
    }

    /// - Parameter initialDelay: overrides the configured delay for this press only.
    ///   The spacebar pushes it out so a trackpad drag has room to start before the key
    ///   begins inserting spaces.
    func start(initialDelay override: TimeInterval? = nil) {
        cancel()
        isActive = true
        scheduleNext(after: override ?? initialDelay)
    }

    func finish() -> Bool {
        let suppressRelease = isActive && hasRepeated
        cancel()
        return suppressRelease
    }

    func cancel() {
        scheduledAction?.cancel()
        scheduledAction = nil
        isActive = false
        hasRepeated = false
    }

    private func tick() {
        scheduledAction = nil
        guard isActive else { return }
        hasRepeated = true
        onRepeat()
        scheduleNext(after: repeatInterval)
    }

    private func scheduleNext(after delay: TimeInterval) {
        scheduledAction = schedule(delay) { [weak self] in
            self?.tick()
        }
    }

    static func schedule(
        after delay: TimeInterval,
        action: @escaping @MainActor () -> Void
    ) -> KeyboardScheduledAction {
        let timer = Timer(timeInterval: delay, repeats: false) { _ in
            MainActor.assumeIsolated { action() }
        }
        RunLoop.main.add(timer, forMode: .common)
        return KeyboardScheduledAction { timer.invalidate() }
    }
}
#endif
