import FunputShared

protocol FunputConfigurationStoring {
    func load() -> FunputConfiguration
    func save(_ configuration: FunputConfiguration) -> Bool
}

extension FunputConfigurationStore: FunputConfigurationStoring {}

protocol KeyboardAccessStateReading {
    var hasObservedFullAccess: Bool { get }
}

extension KeyboardAccessStateStore: KeyboardAccessStateReading {}

struct PreviewConfigurationStore: FunputConfigurationStoring {
    var configuration = FunputConfiguration.default

    func load() -> FunputConfiguration { configuration }
    func save(_ configuration: FunputConfiguration) -> Bool { true }
}
