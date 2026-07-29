#if DEBUG
import Foundation

/// A short-lived, separately stored configuration for end-to-end UI tests.
///
/// The real user configuration is never modified. An expiry is required so a
/// killed or interrupted UI-test process cannot leave the keyboard permanently
/// pinned to the test configuration.
public struct FunputUITestConfigurationOverrideStore {
    private struct Envelope: Codable {
        let configuration: FunputConfiguration
        let expiresAt: Date
    }

    private let defaults: UserDefaults
    private let key: String

    public init(
        suiteName: String = FunputAppGroup.identifier,
        key: String = FunputAppGroup.uiTestConfigurationOverrideKey
    ) {
        self.init(defaults: UserDefaults(suiteName: suiteName) ?? .standard, key: key)
    }

    public init(
        defaults: UserDefaults,
        key: String = FunputAppGroup.uiTestConfigurationOverrideKey
    ) {
        self.defaults = defaults
        self.key = key
    }

    public func load(now: Date = Date()) -> FunputConfiguration? {
        guard let data = defaults.data(forKey: key),
              let envelope = try? JSONDecoder().decode(Envelope.self, from: data),
              envelope.expiresAt > now
        else {
            defaults.removeObject(forKey: key)
            return nil
        }
        return envelope.configuration
    }

    @discardableResult
    public func save(_ configuration: FunputConfiguration, expiresAt: Date) -> Bool {
        guard let data = try? JSONEncoder().encode(
            Envelope(configuration: configuration, expiresAt: expiresAt)
        ) else { return false }
        defaults.set(data, forKey: key)
        return true
    }

    public func clear() {
        defaults.removeObject(forKey: key)
    }
}
#endif
