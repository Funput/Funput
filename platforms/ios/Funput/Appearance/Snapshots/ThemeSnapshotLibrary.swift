import Combine
import UIKit

/// The gallery's pictures of custom themes, captured from the docked keyboard preview
/// while it is on screen at full size (glass only composites there). Cards without one
/// fall back to the drawn sketch.
@MainActor
final class ThemeSnapshotLibrary: ObservableObject {
    private let storage: any ThemeSnapshotStoring
    private var cache: [ThemeSnapshotKey: UIImage] = [:]
    /// Keys already looked up and not found, so a card redraw does not hit the disk.
    private var missing: Set<ThemeSnapshotKey> = []

    init(storage: any ThemeSnapshotStoring) {
        self.storage = storage
    }

    func image(for key: ThemeSnapshotKey) -> UIImage? {
        if let cached = cache[key] { return cached }
        if missing.contains(key) { return nil }
        guard let data = storage.data(for: key), let image = UIImage(data: data) else {
            missing.insert(key)
            return nil
        }
        cache[key] = image
        return image
    }

    func contains(_ key: ThemeSnapshotKey) -> Bool { image(for: key) != nil }

    func save(_ image: UIImage, for key: ThemeSnapshotKey) {
        guard let data = image.jpegData(compressionQuality: 0.85),
              storage.save(data, for: key)
        else { return }
        objectWillChange.send()
        forget { $0.themeID == key.themeID && $0.mode == key.mode }
        cache[key] = image
    }

    func cleanup(keeping themeIDs: Set<String>) {
        storage.cleanup(keeping: themeIDs)
        forget { !themeIDs.contains($0.themeID) }
    }

    private func forget(where matches: (ThemeSnapshotKey) -> Bool) {
        cache = cache.filter { !matches($0.key) }
        missing = missing.filter { !matches($0) }
    }
}
