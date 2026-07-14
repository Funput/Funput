import Foundation
import Testing
import ThemeRuntime

struct ThemeAssetStoreTests {
    @Test("Asset bundle saves, loads, deletes, and rejects unsafe identifiers")
    func lifecycle() throws {
        let root = temporaryRoot()
        defer { try? FileManager.default.removeItem(at: root) }
        let store = ThemeAssetStore(root: root)
        let id = UUID(uuidString: "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE")!

        let assetID = try #require(store.save(
            source: Data("source".utf8),
            rendered: Data("rendered".utf8),
            id: id
        ))
        #expect(store.sourceData(for: assetID) == Data("source".utf8))
        #expect(store.renderedData(for: assetID) == Data("rendered".utf8))
        #expect(store.renderedData(for: "../escape") == nil)
        #expect(store.delete(assetID: assetID))
        #expect(store.renderedData(for: assetID) == nil)
    }

    @Test("Cleanup retains referenced assets and removes orphans")
    func cleanup() throws {
        let root = temporaryRoot()
        defer { try? FileManager.default.removeItem(at: root) }
        let store = ThemeAssetStore(root: root)
        let kept = try #require(store.save(source: Data([1]), rendered: Data([2])))
        let orphan = try #require(store.save(source: Data([3]), rendered: Data([4])))

        store.cleanup(referencedAssetIDs: [kept])

        #expect(store.renderedData(for: kept) == Data([2]))
        #expect(store.renderedData(for: orphan) == nil)
    }

    private func temporaryRoot() -> URL {
        FileManager.default.temporaryDirectory
            .appendingPathComponent("ThemeAssetStoreTests-\(UUID().uuidString)")
    }
}
