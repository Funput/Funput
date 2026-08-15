import Foundation
import FunputShared

public struct ThemeAssetStore: Sendable {
    private let root: URL?

    public init(suiteName: String = FunputAppGroup.identifier) {
        let container = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: suiteName
        )
        root = container?.appendingPathComponent("ThemeAssets", isDirectory: true)
    }

    /// - Parameter root: `nil` models an unavailable container, which every operation
    ///   has to fail closed on. Constructing that state directly is the only reliable
    ///   way to reach it from a test: a simulator hands out a usable container for any
    ///   App Group identifier, so going through `init(suiteName:)` with a bogus one
    ///   exercises the happy path instead.
    public init(root: URL?) { self.root = root }

    public func save(source: Data, rendered: Data, id: UUID = UUID()) -> String? {
        guard let root else { return nil }
        let identifier = id.uuidString.lowercased()
        let final = root.appendingPathComponent(identifier, isDirectory: true)
        let temporary = root.appendingPathComponent(".tmp-\(identifier)", isDirectory: true)
        do {
            try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
            try? FileManager.default.removeItem(at: temporary)
            try FileManager.default.createDirectory(at: temporary, withIntermediateDirectories: true)
            try source.write(to: temporary.appendingPathComponent("source.jpg"), options: .atomic)
            try rendered.write(to: temporary.appendingPathComponent("rendered.jpg"), options: .atomic)
            try FileManager.default.moveItem(at: temporary, to: final)
            return identifier
        } catch {
            try? FileManager.default.removeItem(at: temporary)
            return nil
        }
    }

    public func sourceData(for assetID: String) -> Data? {
        data(named: "source.jpg", assetID: assetID)
    }

    public func renderedData(for assetID: String) -> Data? {
        data(named: "rendered.jpg", assetID: assetID)
    }

    @discardableResult public func delete(assetID: String) -> Bool {
        guard let url = assetURL(assetID) else { return false }
        do {
            if FileManager.default.fileExists(atPath: url.path) {
                try FileManager.default.removeItem(at: url)
            }
            return true
        } catch { return false }
    }

    public func cleanup(referencedAssetIDs: Set<String>) {
        guard let root,
              let urls = try? FileManager.default.contentsOfDirectory(
                at: root,
                includingPropertiesForKeys: nil
              ) else { return }
        for url in urls where !referencedAssetIDs.contains(url.lastPathComponent) {
            try? FileManager.default.removeItem(at: url)
        }
    }

    private func data(named name: String, assetID: String) -> Data? {
        guard let url = assetURL(assetID)?.appendingPathComponent(name) else { return nil }
        return try? Data(contentsOf: url, options: .mappedIfSafe)
    }

    private func assetURL(_ id: String) -> URL? {
        guard let root, !id.isEmpty, !id.contains("/") else { return nil }
        return root.appendingPathComponent(id, isDirectory: true)
    }
}
