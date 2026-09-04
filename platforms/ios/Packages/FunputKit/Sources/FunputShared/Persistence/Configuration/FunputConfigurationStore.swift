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

    /// Reads the stored configuration, or works out what an install with nothing
    /// stored should start on.
    ///
    /// That second case is the one write this read can perform: it settles the
    /// install's ``ToneStyleInstallCohort``, and here is the earliest point that
    /// still knows whether the App Group was empty before Funput wrote to it.
    public func load() -> FunputConfiguration {
        guard let data = defaults.data(forKey: key) else { return unstoredConfiguration() }
        guard let configuration = try? JSONDecoder().decode(FunputConfiguration.self, from: data)
        else {
            // Data that will not decode still belongs to someone who has been
            // typing here, so their settings are gone but their tone placement is
            // not something to guess at.
            return defaultConfiguration(toneStyle: ToneStyleInstallCohort.legacy.toneStyle)
        }
        return configuration
    }

    /// Nothing stored: either a new install, or someone who has typed here for
    /// releases without ever opening Settings. Only the cohort can tell them apart.
    private func unstoredConfiguration() -> FunputConfiguration {
        let cohort = ToneStyleInstallCohortStore(defaults: defaults).cohort()
        return defaultConfiguration(toneStyle: cohort.toneStyle)
    }

    private func defaultConfiguration(toneStyle: ToneStyleOption) -> FunputConfiguration {
        var configuration = FunputConfiguration.default
        configuration.toneStyle = toneStyle
        return configuration
    }

    @discardableResult
    public func save(_ configuration: FunputConfiguration) -> Bool {
        guard let data = try? JSONEncoder().encode(configuration) else { return false }
        defaults.set(data, forKey: key)
        return true
    }
}
