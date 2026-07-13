import Foundation

/// Shares the keyboard extension's last confirmed Full Access state with the app.
public struct KeyboardAccessStateStore {
    private let defaults: UserDefaults
    private let key: String

    public init(
        suiteName: String = FunputAppGroup.identifier,
        key: String = FunputAppGroup.observedFullAccessKey
    ) {
        defaults = UserDefaults(suiteName: suiteName) ?? .standard
        self.key = key
    }

    public var hasObservedFullAccess: Bool {
        defaults.bool(forKey: key)
    }

    public func recordFullAccess() {
        defaults.set(true, forKey: key)
    }
}
