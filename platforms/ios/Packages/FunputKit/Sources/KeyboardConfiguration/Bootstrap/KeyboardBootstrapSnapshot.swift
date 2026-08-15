import FunputShared
import ThemeRuntime
import ThemeSchema

public struct KeyboardBootstrapSnapshot: Codable, Hashable, Sendable {
    public static let currentSchemaVersion = 1

    public let schemaVersion: Int
    public let configuration: FunputConfiguration
    public let selectedTheme: KeyboardTheme

    public init(
        configuration: FunputConfiguration,
        selectedTheme: KeyboardTheme
    ) {
        var normalized = configuration
        normalized.schemaVersion = FunputConfiguration.currentSchemaVersion
        normalized.heightScale = min(max(normalized.heightScale, 0.85), 1.2)
        normalized.selectedThemeID = selectedTheme.id
        schemaVersion = Self.currentSchemaVersion
        self.configuration = normalized
        self.selectedTheme = selectedTheme
    }

    public static func make(
        configuration: FunputConfiguration,
        customThemes: [CustomKeyboardTheme]
    ) -> KeyboardBootstrapSnapshot {
        let catalog = ThemeCatalog(customThemes: customThemes)
        let theme = catalog.theme(id: configuration.selectedThemeID)
            ?? BundledThemes.default
        return KeyboardBootstrapSnapshot(
            configuration: configuration,
            selectedTheme: theme
        )
    }

    private enum CodingKeys: String, CodingKey {
        case schemaVersion, configuration, selectedTheme
    }

    public init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        let version = try values.decode(Int.self, forKey: .schemaVersion)
        guard version == Self.currentSchemaVersion else {
            throw KeyboardBootstrapSnapshotError.unsupportedSchema(version)
        }
        let configuration = try values.decode(
            FunputConfiguration.self,
            forKey: .configuration
        )
        let selectedTheme = try values.decode(
            KeyboardTheme.self,
            forKey: .selectedTheme
        )
        guard configuration.selectedThemeID == selectedTheme.id else {
            throw KeyboardBootstrapSnapshotError.inconsistentTheme
        }
        let normalized = Self(
            configuration: configuration,
            selectedTheme: selectedTheme
        )
        guard normalized.configuration == configuration else {
            throw KeyboardBootstrapSnapshotError.invalidConfiguration
        }
        self.schemaVersion = version
        self.configuration = configuration
        self.selectedTheme = selectedTheme
    }
}

public enum KeyboardBootstrapSnapshotError: Error, Equatable {
    case unavailableContainer
    case unsupportedSchema(Int)
    case inconsistentTheme
    case invalidConfiguration
}
