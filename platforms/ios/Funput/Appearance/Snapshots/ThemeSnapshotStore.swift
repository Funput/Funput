import Foundation

/// Where the gallery keeps the pictures captured of custom themes. Only the app reads
/// them, and any of them can be captured again, so they live in the app's caches.
protocol ThemeSnapshotStoring {
    func data(for key: ThemeSnapshotKey) -> Data?
    /// Replaces any older picture of the same theme in the same style.
    func save(_ data: Data, for key: ThemeSnapshotKey) -> Bool
    func cleanup(keeping themeIDs: Set<String>)
}

struct ThemeSnapshotFileStore: ThemeSnapshotStoring {
    private static let separator = "__"
    let directory: URL

    init(directory: URL = Self.defaultDirectory) {
        self.directory = directory
    }

    static var defaultDirectory: URL {
        FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("ThemeSnapshots", isDirectory: true)
    }

    func data(for key: ThemeSnapshotKey) -> Data? {
        try? Data(contentsOf: url(for: key))
    }

    func save(_ data: Data, for key: ThemeSnapshotKey) -> Bool {
        let fileManager = FileManager.default
        do {
            try fileManager.createDirectory(at: directory, withIntermediateDirectories: true)
            try data.write(to: url(for: key), options: .atomic)
        } catch {
            return false
        }
        let prefix = stem(themeID: key.themeID, mode: key.mode)
        let current = url(for: key).lastPathComponent
        for name in fileNames() where name.hasPrefix(prefix) && name != current {
            try? fileManager.removeItem(at: directory.appendingPathComponent(name))
        }
        return true
    }

    /// File names start with the theme ID up to the first separator; custom theme IDs
    /// (`app.funput.theme.custom.<uuid>`) never contain one.
    func cleanup(keeping themeIDs: Set<String>) {
        for name in fileNames() {
            guard let range = name.range(of: Self.separator),
                  !themeIDs.contains(String(name[..<range.lowerBound]))
            else { continue }
            try? FileManager.default.removeItem(at: directory.appendingPathComponent(name))
        }
    }

    private func fileNames() -> [String] {
        (try? FileManager.default.contentsOfDirectory(atPath: directory.path)) ?? []
    }

    private func stem(themeID: String, mode: AppearancePreviewMode) -> String {
        [themeID, mode.rawValue, ""].joined(separator: Self.separator)
    }

    private func url(for key: ThemeSnapshotKey) -> URL {
        let name = stem(themeID: key.themeID, mode: key.mode) + key.fingerprint + ".jpg"
        return directory.appendingPathComponent(name)
    }
}

/// Keeps pictures for the current launch only: SwiftUI previews and tests.
final class PreviewThemeSnapshotStore: ThemeSnapshotStoring {
    private var pictures: [ThemeSnapshotKey: Data] = [:]

    func data(for key: ThemeSnapshotKey) -> Data? { pictures[key] }

    func save(_ data: Data, for key: ThemeSnapshotKey) -> Bool {
        pictures = pictures.filter { $0.key.themeID != key.themeID || $0.key.mode != key.mode }
        pictures[key] = data
        return true
    }

    func cleanup(keeping themeIDs: Set<String>) {
        pictures = pictures.filter { themeIDs.contains($0.key.themeID) }
    }
}
