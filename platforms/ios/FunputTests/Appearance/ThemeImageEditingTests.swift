import Foundation
import FunputShared
import Testing
import ThemeRuntime
import ThemeSchema
@testable import Funput

@MainActor
struct ThemeImageEditingTests {
    @Test("Installing an image selects image mode and creates adaptive overlay defaults")
    func installDefaults() {
        var draft = makeDraft()
        draft.installImage(ProcessedThemeImage(source: Data([1]), rendered: Data([2])))

        #expect(draft.customTheme.theme.backgroundEffects.mode == .image)
        #expect(draft.customTheme.theme.backgroundEffects.overlay.light.alpha == 0.15)
        #expect(draft.customTheme.theme.backgroundEffects.overlay.dark.alpha == 0.25)
        #expect(draft.needsAssetSave)
        #expect(draft.canSave)
    }

    @Test("Overlay opacity edits only the preview appearance")
    func adaptiveOverlay() {
        var draft = makeDraft()
        draft.installImage(ProcessedThemeImage(source: Data([1]), rendered: Data([2])))
        draft.previewMode = .light
        let dark = draft.customTheme.theme.backgroundEffects.overlay.dark

        draft.setOverlayOpacity(0.55)

        #expect(draft.customTheme.theme.backgroundEffects.overlay.light.alpha == 0.55)
        #expect(draft.customTheme.theme.backgroundEffects.overlay.dark == dark)
    }

    @Test("Image mode without a valid asset cannot be saved")
    func invalidImageMode() {
        var draft = makeDraft()
        draft.setBackgroundMode(.image)
        #expect(!draft.canSave)
        draft.setBackgroundMode(.gradient)
        #expect(draft.canSave)
    }

    @Test("Image asset save rolls back when theme persistence fails")
    func saveRollback() {
        let themes = ThemeTestStore(acceptsMutations: false)
        let assets = PreviewThemeAssetStore()
        let model = AppearanceModel(
            store: SettingsTestStore(configuration: .default),
            customStore: themes,
            assetStore: assets
        )
        var draft = model.makeDraft(for: .create(baseThemeID: KeyboardTheme.funputGlass.id))
        draft.installImage(ProcessedThemeImage(source: Data([1]), rendered: Data([2])))

        #expect(!model.save(draft))
        #expect(assets.assetCount == 0)
        #expect(themes.load().isEmpty)
    }

    @Test("Saving an image theme persists a real asset id but only changes preview")
    func saveImageTheme() throws {
        let themes = ThemeTestStore()
        let assets = PreviewThemeAssetStore()
        let model = AppearanceModel(
            store: SettingsTestStore(configuration: .default),
            customStore: themes,
            assetStore: assets
        )
        var draft = model.makeDraft(for: .create(baseThemeID: KeyboardTheme.funputGlass.id))
        draft.installImage(ProcessedThemeImage(source: Data([1]), rendered: Data([2])))

        #expect(model.save(draft))
        let saved = try #require(themes.load().first)
        #expect(saved.theme.backgroundEffects.image?.assetID != "pending")
        #expect(assets.assetCount == 1)
        #expect(model.previewThemeID == saved.id)
        #expect(model.appliedThemeID != saved.id)
    }

    private func makeDraft() -> ThemeEditorDraft {
        AppearanceModel(
            store: SettingsTestStore(configuration: .default),
            customStore: ThemeTestStore(),
            assetStore: PreviewThemeAssetStore()
        ).makeDraft(for: .create(baseThemeID: KeyboardTheme.funputGlass.id))
    }
}
