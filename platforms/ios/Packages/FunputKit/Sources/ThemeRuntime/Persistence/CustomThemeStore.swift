import Foundation
import FunputShared
import ThemeSchema

/// Persists custom themes in the App Group shared by the app and keyboard.
public struct CustomThemeStore {
    private let defaults: UserDefaults
    private let key: String

    public init(
        suiteName: String = FunputAppGroup.identifier,
        key: String = FunputAppGroup.customThemesKey
    ) {
        self.init(defaults: UserDefaults(suiteName: suiteName) ?? .standard, key: key)
    }

    public init(defaults: UserDefaults, key: String = FunputAppGroup.customThemesKey) {
        self.defaults = defaults
        self.key = key
    }

    public func load() -> [CustomKeyboardTheme] {
        guard let data = defaults.data(forKey: key),
              let themes = try? JSONDecoder().decode([CustomKeyboardTheme].self, from: data)
        else { return [] }
        return themes
    }

    @discardableResult
    public func upsert(_ theme: CustomKeyboardTheme) -> Bool {
        var themes = load()
        if let index = themes.firstIndex(where: { $0.id == theme.id }) {
            themes[index] = theme
        } else {
            themes.append(theme)
        }
        return save(themes)
    }

    @discardableResult
    public func delete(id: String) -> Bool {
        save(load().filter { $0.id != id })
    }

    private func save(_ themes: [CustomKeyboardTheme]) -> Bool {
        guard let data = try? JSONEncoder().encode(themes) else { return false }
        defaults.set(data, forKey: key)
        return defaults.data(forKey: key) == data
    }
}
