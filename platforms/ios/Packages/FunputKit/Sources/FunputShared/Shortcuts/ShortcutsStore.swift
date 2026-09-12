import Foundation

/// Serializes file access away from the main actor. Only the containing app writes.
public actor ShortcutsStore: ShortcutsStoring {
    public static let shared = ShortcutsStore()
    private let directory: URL?
    private let write: @Sendable (Data, URL) throws -> Void

    public init(directory: URL? = AppGroupDirectory.containerURL()?.appendingPathComponent("Shortcuts")) {
        self.directory = directory
        self.write = Self.atomicWrite
    }

    /// Injects a deterministic write failure without depending on sandbox permissions.
    init(directory: URL?, write: @escaping @Sendable (Data, URL) throws -> Void) {
        self.directory = directory
        self.write = write
    }

    public func load() throws -> ShortcutLibrary {
        guard let directory else { throw ShortcutsStorageError.unavailable }
        let data: Data
        do {
            data = try Data(contentsOf: directory.appendingPathComponent("shortcuts.json"))
        } catch CocoaError.fileReadNoSuchFile {
            return ShortcutLibrary()
        } catch {
            throw ShortcutsStorageError.readFailed
        }
        do {
            let version = try JSONDecoder().decode(Version.self, from: data)
            guard version.schemaVersion == ShortcutLibrary.currentSchemaVersion else {
                throw ShortcutsStorageError.unsupportedVersion
            }
            let library = try JSONDecoder().decode(ShortcutLibrary.self, from: data)
            try library.validate()
            return library
        } catch let error as ShortcutsStorageError {
            throw error == .duplicateTrigger ? ShortcutsStorageError.invalidData : error
        } catch {
            throw ShortcutsStorageError.invalidData
        }
    }

    public func save(_ library: ShortcutLibrary) throws {
        try library.validate()
        // Recheck before replacing a file that may have become unreadable since load.
        _ = try load()
        guard let directory,
              AppGroupDirectory.prepare(
                named: directory.lastPathComponent,
                in: directory.deletingLastPathComponent(),
                excludedFromBackup: false
              ) != nil else {
            throw ShortcutsStorageError.writeFailed
        }
        do {
            try write(JSONEncoder().encode(library), directory.appendingPathComponent("shortcuts.json"))
        } catch {
            throw ShortcutsStorageError.writeFailed
        }
    }

    private struct Version: Decodable {
        let schemaVersion: Int
    }

    private static func atomicWrite(_ data: Data, to url: URL) throws {
        try data.write(to: url, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
    }
}
