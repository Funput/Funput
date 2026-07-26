#if canImport(UIKit)
import Foundation

@MainActor
final class AlternateHoldController {
    typealias TouchToken = KeyboardPressCommitQueue.TouchToken
    typealias Scheduler = BackspaceRepeatController.Scheduler

    private let delay: TimeInterval
    private let schedule: Scheduler
    private let onActivate: (TouchToken) -> Void
    private var action: KeyboardScheduledAction?
    private var token: TouchToken?

    init(
        delay: TimeInterval = 0.35,
        schedule: @escaping Scheduler = BackspaceRepeatController.schedule,
        onActivate: @escaping (TouchToken) -> Void
    ) {
        self.delay = delay
        self.schedule = schedule
        self.onActivate = onActivate
    }

    func start(for token: TouchToken) {
        guard action == nil else { return }
        self.token = token
        action = schedule(delay) { [weak self] in
            guard let self, let token = self.token else { return }
            self.action = nil
            self.token = nil
            self.onActivate(token)
        }
    }

    func cancel(for token: TouchToken) {
        guard self.token == token else { return }
        cancelAll()
    }

    func cancelAll() {
        action?.cancel()
        action = nil
        token = nil
    }
}
#endif
