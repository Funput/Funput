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

    @Test("Missing assets and unavailable storage fail closed")
    func unavailableStorage() {
        let missingStore = ThemeAssetStore(root: temporaryRoot())
        #expect(missingStore.renderedData(for: "missing") == nil)
        #expect(missingStore.sourceData(for: "missing") == nil)

        // A bogus App Group identifier used to stand in for an unavailable container,
        // but a simulator returns a usable container for any identifier, so the store
        // happily saved and the expectation failed. `root: nil` is the same state
        // reached deterministically on both simulator and device.
        let unavailableStore = ThemeAssetStore(root: nil)
        #expect(unavailableStore.renderedData(for: "asset") == nil)
        #expect(unavailableStore.sourceData(for: "asset") == nil)
        #expect(unavailableStore.save(source: Data([1]), rendered: Data([2])) == nil)
        #expect(!unavailableStore.delete(assetID: "asset"))
    }

    private func temporaryRoot() -> URL {
        FileManager.default.temporaryDirectory
            .appendingPathComponent("ThemeAssetStoreTests-\(UUID().uuidString)")
    }
}
