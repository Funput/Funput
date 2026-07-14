import Foundation

/// A user-authored snapshot derived from one immutable bundled theme.
public struct CustomKeyboardTheme: Codable, Hashable, Sendable, Identifiable {
    public var baseThemeID: String
    public var theme: KeyboardTheme

    public var id: String { theme.id }

    public init(baseThemeID: String, theme: KeyboardTheme) {
        self.baseThemeID = baseThemeID
        self.theme = theme
    }

    public init(baseTheme: KeyboardTheme, name: String? = nil, uuid: UUID = UUID()) {
        var snapshot = baseTheme
        snapshot.id = "app.funput.theme.custom.\(uuid.uuidString.lowercased())"
        snapshot.schemaVersion = KeyboardTheme.currentSchemaVersion
        snapshot.metadata.name = name ?? "\(baseTheme.metadata.name) Custom"
        snapshot.metadata.author = "Bạn"
        self.init(baseThemeID: baseTheme.id, theme: snapshot)
    }
}
