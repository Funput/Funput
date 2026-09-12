import Foundation

/// Versioned, standalone App Group document; not part of keyboard bootstrap settings.
public struct ShortcutLibrary: Codable, Hashable, Sendable {
    public static let currentSchemaVersion = 1
    public var schemaVersion: Int
    public var entries: [TextShortcut]
    public var isEnabled: Bool
    public var smartCase: Bool
    public var inEnglish: Bool

    public init(
        entries: [TextShortcut] = [],
        isEnabled: Bool = true,
        smartCase: Bool = true,
        inEnglish: Bool = true,
        schemaVersion: Int = Self.currentSchemaVersion
    ) {
        self.schemaVersion = schemaVersion
        self.entries = entries
        self.isEnabled = isEnabled
        self.smartCase = smartCase
        self.inEnglish = inEnglish
    }

    public func isDuplicate(_ entry: TextShortcut) -> Bool {
        entries.contains { $0.id != entry.id && $0.trigger == entry.trigger }
    }

    /// Also enforced on disk reads, so malformed data cannot silently become editable.
    public func validate() throws {
        guard schemaVersion == Self.currentSchemaVersion else {
            throw ShortcutsStorageError.unsupportedVersion
        }
        var ids = Set<UUID>()
        var triggers = Set<String>()
        for entry in entries {
            guard entry.isValid, ids.insert(entry.id).inserted else {
                throw ShortcutsStorageError.invalidData
            }
            guard triggers.insert(entry.trigger).inserted else {
                throw ShortcutsStorageError.duplicateTrigger
            }
        }
    }
}
