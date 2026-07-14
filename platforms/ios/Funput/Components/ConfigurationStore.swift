import FunputShared
import ThemeRuntime
import ThemeSchema

protocol FunputConfigurationStoring {
    func load() -> FunputConfiguration
    func save(_ configuration: FunputConfiguration) -> Bool
}

extension FunputConfigurationStore: FunputConfigurationStoring {}

protocol KeyboardAccessStateReading {
    var hasObservedFullAccess: Bool { get }
}

extension KeyboardAccessStateStore: KeyboardAccessStateReading {}

protocol CustomThemeStoring {
    func load() -> [CustomKeyboardTheme]
    func upsert(_ theme: CustomKeyboardTheme) -> Bool
    func delete(id: String) -> Bool
}

extension CustomThemeStore: CustomThemeStoring {}

struct PreviewConfigurationStore: FunputConfigurationStoring {
    var configuration = FunputConfiguration.default

    func load() -> FunputConfiguration { configuration }
    func save(_ configuration: FunputConfiguration) -> Bool { true }
}

final class PreviewCustomThemeStore: CustomThemeStoring {
    var themes: [CustomKeyboardTheme]

    init(themes: [CustomKeyboardTheme] = []) {
        self.themes = themes
    }

    func load() -> [CustomKeyboardTheme] { themes }

    func upsert(_ theme: CustomKeyboardTheme) -> Bool {
        themes.removeAll { $0.id == theme.id }
        themes.append(theme)
        return true
    }

    func delete(id: String) -> Bool {
        themes.removeAll { $0.id == id }
        return true
    }
}
