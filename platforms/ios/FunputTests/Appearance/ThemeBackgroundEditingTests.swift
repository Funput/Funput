import FunputShared
import Testing
import ThemeRuntime
import ThemeSchema
@testable import Funput

@MainActor
struct ThemeBackgroundEditingTests {
    @Test("Background opacity edits only the active appearance and stop")
    func opacityIsAdaptive() {
        var draft = makeDraft()
        let originalDark = draft.customTheme.theme.palette.backgroundStart.dark
        let originalEnd = draft.customTheme.theme.palette.backgroundEnd
        draft.previewMode = .light

        draft.setBackgroundOpacity(0.35, for: .start)

        #expect(draft.customTheme.theme.palette.backgroundStart.light.alpha == 0.35)
        #expect(draft.customTheme.theme.palette.backgroundStart.dark == originalDark)
        #expect(draft.customTheme.theme.palette.backgroundEnd == originalEnd)
        #expect(draft.customTheme.theme.colorEffects.glassBackgroundTintEnabled)
    }

    @Test("Changing Glass gradient direction enables its background tint")
    func directionEnablesGlassTint() {
        var draft = makeDraft()
        #expect(!draft.customTheme.theme.colorEffects.glassBackgroundTintEnabled)

        draft.setGradientDirection(.vertical)

        #expect(draft.customTheme.theme.gradientDirection == .vertical)
        #expect(draft.customTheme.theme.colorEffects.glassBackgroundTintEnabled)
    }

    @Test("Changing material preserves gradient values")
    func materialPreservesGradient() {
        var draft = makeDraft()
        draft.setGradientDirection(.horizontal)
        draft.setBackgroundOpacity(0.4, for: .end)
        draft.customTheme.theme.material = .solid
        draft.customTheme.theme.material = .glass

        #expect(draft.customTheme.theme.gradientDirection == .horizontal)
        #expect(draft.backgroundOpacity(for: .end) == 0.4)
    }

    private func makeDraft() -> ThemeEditorDraft {
        let model = AppearanceModel(
            store: SettingsTestStore(configuration: .default),
            customStore: ThemeTestStore()
        )
        return model.makeDraft(for: .create(baseThemeID: KeyboardTheme.funputGlass.id))
    }
}
