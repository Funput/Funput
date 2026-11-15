import Foundation
import FunputShared

/// Keeps the app's Full Access state in step with the keyboard, which is the only
/// process that can ask iOS for it.
///
/// Changes made while the app was away are picked up by the reload on activation. A
/// change made while the app is open (the Funput keyboard shown over one of its text
/// fields) arrives as a Darwin signal. The keyboard posts that signal once per change,
/// right after its defaults write, and the app's read can still return the old value,
/// so a signal that finds nothing new is re-checked a few times instead of dropped.
@MainActor
final class FullAccessRefresh {
    typealias Observe =
        @MainActor (@escaping @MainActor () -> Void) -> any ObservedFullAccessObserving

    private let reloadSettings: () -> Void
    private let refreshAccess: () -> Bool
    private let observe: Observe
    private let retryAttempts: Int
    private let retryInterval: Duration
    private var observer: (any ObservedFullAccessObserving)?
    private(set) var retryTask: Task<Void, Never>?

    /// - Parameters:
    ///   - reloadSettings: Re-reads everything the keyboard may have written.
    ///   - refreshAccess: Re-reads only the Full Access flag; returns whether it changed.
    init(
        reloadSettings: @escaping () -> Void,
        refreshAccess: @escaping () -> Bool,
        retryAttempts: Int = 6,
        retryInterval: Duration = .milliseconds(500),
        observe: @escaping Observe = ObservedFullAccessSignal.observe
    ) {
        self.reloadSettings = reloadSettings
        self.refreshAccess = refreshAccess
        self.retryAttempts = retryAttempts
        self.retryInterval = retryInterval
        self.observe = observe
    }

    var isActive: Bool { observer != nil }

    /// Reloads once and listens for the keyboard. Repeated calls are no-ops.
    func start() {
        guard !isActive else { return }
        reloadSettings()
        observer = observe { [weak self] in self?.handleSignal() }
    }

    func stop() {
        observer = nil
        retryTask?.cancel()
        retryTask = nil
    }

    private func handleSignal() {
        guard isActive else { return }
        retryTask?.cancel()
        retryTask = nil
        guard !refreshAccess() else { return }
        retryTask = Task { [weak self, retryAttempts, retryInterval] in
            for _ in 0..<retryAttempts {
                do { try await Task.sleep(for: retryInterval) } catch { return }
                guard let self, self.isActive else { return }
                if self.refreshAccess() { return }
            }
        }
    }
}
