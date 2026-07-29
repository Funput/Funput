import Foundation

/// Reads and writes ``FunputConfiguration`` in the shared App Group defaults.
///
/// Reads never throw: a missing, unreadable, or partially corrupt value falls
/// back to ``FunputConfiguration/default`` so the keyboard always has usable
/// settings — including when Full Access (and thus shared storage) is absent.
public struct FunputConfigurationStore {
    private let defaults: UserDefaults
    private let key: String

    /// Creates a store backed by the App Group suite, falling back to
    /// `.standard` if the suite is unavailable at runtime.
    public init(
        suiteName: String = FunputAppGroup.identifier,
        key: String = FunputAppGroup.configurationKey
    ) {
        self.init(defaults: UserDefaults(suiteName: suiteName) ?? .standard, key: key)
    }

    /// Creates a store backed by an explicit defaults instance (used in tests).
    public init(defaults: UserDefaults, key: String = FunputAppGroup.configurationKey) {
        self.defaults = defaults
        self.key = key
    }

    public func load() -> FunputConfiguration {
        guard let data = defaults.data(forKey: key),
              let configuration = try? JSONDecoder().decode(FunputConfiguration.self, from: data)
        else { return .default }
        return configuration
    }

    @discardableResult
    public func save(_ configuration: FunputConfiguration) -> Bool {
        guard let data = try? JSONEncoder().encode(configuration) else { return false }
        defaults.set(data, forKey: key)
        return true
    }
}
