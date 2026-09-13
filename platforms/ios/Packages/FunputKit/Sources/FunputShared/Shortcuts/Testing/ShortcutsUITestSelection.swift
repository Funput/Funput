#if DEBUG
import Foundation

/// Debug-only cross-process routing. Fixtures never share the production document path.
public enum ShortcutsUITestSelection {
    private static var root: URL? {
        AppGroupDirectory.containerURL()?.appendingPathComponent("UITests/Shortcuts")
    }
    private static var marker: URL? { root?.appendingPathComponent("active.json") }

    public static func directory(id: String) -> URL? {
        guard UUID(uuidString: id) != nil else { return nil }
        return root?.appendingPathComponent(id)
    }

    public static func activate(id: String?) throws {
        guard let root, let marker else { throw ShortcutsStorageError.unavailable }
        guard let id else {
            if FileManager.default.fileExists(atPath: marker.path) {
                try FileManager.default.removeItem(at: marker)
            }
            return
        }
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        try JSONEncoder().encode(id).write(to: marker, options: .atomic)
    }

    /// Malformed selectors fail closed; they must never fall through to real data.
    public static func selectedStore() -> ShortcutsStore? {
        guard let marker else { return ShortcutsStore(directory: nil) }
        do {
            let data = try Data(contentsOf: marker)
            let id = try JSONDecoder().decode(String.self, from: data)
            return ShortcutsStore(directory: directory(id: id))
        } catch CocoaError.fileReadNoSuchFile {
            return nil
        } catch {
            return ShortcutsStore(directory: nil)
        }
    }
}
#endif
