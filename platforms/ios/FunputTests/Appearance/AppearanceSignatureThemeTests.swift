import FunputShared
import Testing
import ThemeRuntime
import ThemeSchema
@testable import Funput

@MainActor
struct AppearanceSignatureThemeTests {
    @Test(
        "Signature themes preview, apply, customize, save, and reset",
        arguments: [KeyboardTheme.lotusSilk, .jadeCurrent]
    )
    func fullLifecycle(_ base: KeyboardTheme) {
        let settings = SettingsTestStore(configuration: .default)
        let customThemes = ThemeTestStore()
        let model = AppearanceModel(store: settings, customStore: customThemes)

        model.selectTheme(base.id)
        #expect(model.previewTheme == base)
        model.applyPreview()
        #expect(settings.configuration.selectedThemeID == base.id)

        var draft = model.makeDraft(for: .create(baseThemeID: base.id))
        assertDraft(draft, matches: base)
        draft.customTheme.theme.palette.label = base.palette.accent
        draft.customTheme.theme.metrics.cornerRadius = 20
        draft.customTheme.theme.gradientDirection = .vertical
        draft.resetToBase()
        assertDraft(draft, matches: base)

        draft.customTheme.theme.metadata.name = "\(base.metadata.name) Personal"
        #expect(model.save(draft))
        #expect(model.previewThemeID == draft.customTheme.id)
        #expect(customThemes.load() == [draft.customTheme])
        #expect(model.catalog.baseTheme(for: draft.customTheme) == base)
    }

    private func assertDraft(_ draft: ThemeEditorDraft, matches base: KeyboardTheme) {
        #expect(draft.isNew)
        #expect(draft.baseTheme == base)
        #expect(draft.customTheme.baseThemeID == base.id)
        #expect(draft.customTheme.theme.palette == base.palette)
        #expect(draft.customTheme.theme.metrics == base.metrics)
        #expect(draft.customTheme.theme.geometry == base.geometry)
        #expect(draft.customTheme.theme.colorEffects == base.colorEffects)
        #expect(draft.customTheme.theme.surfaceEffects == base.surfaceEffects)
        #expect(draft.customTheme.theme.gradientDirection == base.gradientDirection)
        #expect(draft.customTheme.theme.backgroundEffects == base.backgroundEffects)
    }
}
