import Foundation

/// Shares the keyboard extension's last observed Full Access state with the app.
///
/// Only the keyboard can ask iOS whether Full Access is on, so it records every state it
/// sees, including a revoked one, and signals the app whenever the stored value changes.
public struct KeyboardAccessStateStore {
    private let defaults: UserDefaults
    private let key: String
    private let notify: () -> Void

    public init(
        suiteName: String = FunputAppGroup.identifier,
        key: String = FunputAppGroup.observedFullAccessKey
    ) {
        self.init(defaults: UserDefaults(suiteName: suiteName) ?? .standard, key: key)
    }

    public init(
        defaults: UserDefaults,
        key: String = FunputAppGroup.observedFullAccessKey,
        notify: @escaping () -> Void = ObservedFullAccessSignal.post
    ) {
        self.defaults = defaults
        self.key = key
        self.notify = notify
    }

    public var hasObservedFullAccess: Bool {
        defaults.bool(forKey: key)
    }

    /// Stores what the keyboard just observed. Writes and signals only on a change, so
    /// showing the keyboard repeatedly costs one defaults read.
    public func record(hasFullAccess: Bool) {
        guard hasFullAccess != hasObservedFullAccess else { return }
        defaults.set(hasFullAccess, forKey: key)
        notify()
    }
}
