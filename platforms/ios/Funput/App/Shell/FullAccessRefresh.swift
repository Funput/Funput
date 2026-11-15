import FunputShared
import Foundation

/// Reloads settings when Full Access may have been confirmed while the app was away
/// or while the Funput keyboard was shown over the app.
@MainActor
final class FullAccessRefresh {
    private let reload: () -> Void
    private let hasFullAccess: () -> Bool
    private let observe: (@escaping () -> Void) -> any ObservedFullAccessObserving
    private let pollAttempts: Int
    private let pollInterval: Duration
    private var pollTask: Task<Void, Never>?
    private var observer: (any ObservedFullAccessObserving)?
    private var isActive = false

    init(
        reload: @escaping () -> Void,
        hasFullAccess: @escaping () -> Bool,
        pollAttempts: Int = 6,
        pollInterval: Duration = .milliseconds(500),
        observe: @escaping (@escaping () -> Void) -> any ObservedFullAccessObserving =
            ObservedFullAccessSignal.observe
    ) {
        self.reload = reload
        self.hasFullAccess = hasFullAccess
        self.pollAttempts = pollAttempts
        self.pollInterval = pollInterval
        self.observe = observe
    }

    func start() {
        cancelPoll()
        isActive = true
        reload()
        if observer == nil {
            observer = observe { [weak self] in
                Task { @MainActor in self?.handleSignal() }
            }
        }
        guard !hasFullAccess() else { return }
        pollTask = Task { [weak self] in
            guard let self else { return }
            for _ in 0..<self.pollAttempts {
                try? await Task.sleep(for: self.pollInterval)
                guard !Task.isCancelled, self.isActive else { return }
                self.reload()
                if self.hasFullAccess() { return }
            }
        }
    }

    func stop() {
        isActive = false
        cancelPoll()
    }

    private func handleSignal() {
        guard isActive else { return }
        reload()
        if hasFullAccess() { cancelPoll() }
    }

    private func cancelPoll() {
        pollTask?.cancel()
        pollTask = nil
    }
}
