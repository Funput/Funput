import Foundation
import FunputShared
import KeyboardConfiguration
import Testing
import ThemeRuntime
import ThemeSchema

@MainActor
struct KeyboardActivationTests {
    @Test("Missing selected theme falls back to bundled default")
    func missingThemeFallback() {
        var configuration = FunputConfiguration.default
        configuration.selectedThemeID = "missing"

        let resolved = KeyboardActivationThemeResolver.resolve(
            configuration: configuration,
            customThemes: []
        )

        #expect(resolved.selectedTheme == BundledThemes.default)
    }

    @Test("Activation generation rejects inactive and stale work")
    func generationGuard() {
        let state = KeyboardActivationState()
        let first = state.begin()
        #expect(state.accepts(first))
        state.end()
        #expect(!state.accepts(first))

        let second = state.begin()
        #expect(!state.accepts(first))
        #expect(state.accepts(second))
    }

    @Test("Failed assets retry and never leak the previous image")
    func assetRetry() {
        let cache = KeyboardBackgroundAssetCache<String>()
        var data: [String: Data] = ["old": Data("old".utf8)]
        let decode: (Data) -> String? = { String(data: $0, encoding: .utf8) }

        #expect(cache.resolve(assetID: "old", load: { data[$0] }, decode: decode) == "old")
        #expect(cache.resolve(assetID: "new", load: { data[$0] }, decode: decode) == nil)
        #expect(cache.assetID == nil)
        #expect(cache.value == nil)

        data["new"] = Data("new".utf8)
        #expect(cache.resolve(assetID: "new", load: { data[$0] }, decode: decode) == "new")
        #expect(cache.assetID == "new")
    }

    @Test("Corrupt assets are not cached")
    func corruptAsset() {
        let cache = KeyboardBackgroundAssetCache<String>()
        let corrupt = Data([0xFF])
        let decode: (Data) -> String? = { String(data: $0, encoding: .utf8) }

        #expect(cache.resolve(assetID: "asset", load: { _ in corrupt }, decode: decode) == nil)
        #expect(cache.assetID == nil)
    }
}
