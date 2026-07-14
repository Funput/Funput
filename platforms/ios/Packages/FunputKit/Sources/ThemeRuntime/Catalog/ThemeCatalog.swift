import ThemeSchema

/// Immutable lookup snapshot used by previews and the keyboard hot path.
public struct ThemeCatalog: Hashable, Sendable {
    public let customThemes: [CustomKeyboardTheme]

    public init(customThemes: [CustomKeyboardTheme] = []) {
        self.customThemes = customThemes
    }

    public var all: [KeyboardTheme] {
        BundledThemes.all + customThemes.map(\.theme)
    }

    public func theme(id: String) -> KeyboardTheme? {
        BundledThemes.theme(id: id) ?? customThemes.first { $0.id == id }?.theme
    }

    public func customTheme(id: String) -> CustomKeyboardTheme? {
        customThemes.first { $0.id == id }
    }

    public func baseTheme(for custom: CustomKeyboardTheme) -> KeyboardTheme {
        BundledThemes.theme(id: custom.baseThemeID) ?? BundledThemes.default
    }
}
