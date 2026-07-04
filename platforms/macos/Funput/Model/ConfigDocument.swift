import Foundation

/// The Funput configuration interchange format — see `platforms/CONFIG_FORMAT.md`.
///
/// A versioned JSON document for Export/Import (and, later, sync). Portable fields
/// (`preferences`, `shortcuts`) apply on every platform; `platform.<name>` holds
/// OS-specific data (hotkeys, excluded apps) applied only on that platform. All
/// import-time fields are optional so a partial or future file still decodes and we
/// apply only what we recognise.
struct ConfigDocument: Codable {
    static let schemaID = "app.funput.config"
    static let currentVersion = 1

    var schema = ConfigDocument.schemaID
    var version = ConfigDocument.currentVersion
    var exportedAt: Date?
    var source: Source?
    var preferences: Preferences?
    var shortcuts: [PortableShortcut]?
    var platform: Platform?

    /// Which app/OS produced the file. Metadata only.
    struct Source: Codable {
        var platform: String   // "macos" | "windows" | "linux"
        var appVersion: String
    }

    /// Portable typing preferences. Enums are strings (see `InputMethod`/`ToneStyle`
    /// config-key helpers) so the format is stable across platforms. Every field is
    /// optional: on import we overwrite only the ones present.
    struct Preferences: Codable {
        var inputMethod: String?     // "telex" | "vni"
        var toneStyle: String?       // "traditional" | "modern"
        var smartEnglishRestore: Bool?
        var eagerRestore: Bool?
        var spellCheck: Bool?
        var autoCapitalize: Bool?
    }

    /// A text-expansion shortcut without the local UUID (recreated on import).
    struct PortableShortcut: Codable {
        var trigger: String
        var expansion: String
    }

    /// Platform-specific blocks. Only the block matching the running OS is applied on
    /// import; the others are carried through untouched.
    struct Platform: Codable {
        var macos: MacOS?

        struct MacOS: Codable {
            var toggleShortcut: KeyCombo?
            var flipShortcut: KeyCombo?
            var excludedApps: [ExcludedApp]?
        }
    }
}

// MARK: - JSON coding

extension ConfigDocument {
    private static func makeEncoder() -> JSONEncoder {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys, .withoutEscapingSlashes]
        encoder.dateEncodingStrategy = .iso8601
        return encoder
    }

    private static func makeDecoder() -> JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }

    func encoded() throws -> Data { try Self.makeEncoder().encode(self) }

    static func decoded(from data: Data) throws -> ConfigDocument {
        try makeDecoder().decode(ConfigDocument.self, from: data)
    }
}

// MARK: - Enum ↔ stable config-key mapping

extension InputMethod {
    var configKey: String {
        switch self {
        case .telex: "telex"
        case .vni: "vni"
        }
    }

    init?(configKey: String) {
        switch configKey {
        case "telex": self = .telex
        case "vni": self = .vni
        default: return nil
        }
    }
}

extension ToneStyle {
    var configKey: String {
        switch self {
        case .traditional: "traditional"
        case .modern: "modern"
        }
    }

    init?(configKey: String) {
        switch configKey {
        case "traditional": self = .traditional
        case "modern": self = .modern
        default: return nil
        }
    }
}
