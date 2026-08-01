import FunputShared
import Testing
import ThemeRuntime
import ThemeSchema
@testable import Funput

@MainActor
struct ThemeSurfaceEditingTests {
    @Test("Border opacity edits only the active appearance")
    func borderOpacityIsAdaptive() {
        var draft = makeDraft()
        let originalDark = draft.customTheme.theme.palette.border.dark
        draft.previewMode = .light

        draft.setBorderOpacity(0.65)

        #expect(draft.customTheme.theme.palette.border.light.alpha == 0.65)
        #expect(draft.customTheme.theme.palette.border.dark == originalDark)
        #expect(draft.customTheme.theme.surfaceEffects.glassBorderOverrideEnabled)
    }

    @Test("Editing Glass opacity enables its independent tint")
    func keyOpacityEnablesGlassTint() {
        var draft = makeDraft()
        #expect(!draft.customTheme.theme.colorEffects.glassKeyTintEnabled)

        draft.setKeyOpacity(0.55, special: false)

        #expect(draft.customTheme.theme.metrics.keyOpacity == 0.55)
        #expect(draft.customTheme.theme.colorEffects.glassKeyTintEnabled)
    }

    @Test("Glass overrides retain authored values while disabled")
    func overridesRetainValues() {
        var draft = makeDraft()
        draft.setBorderWidth(2.25)
        draft.setShadowOpacity(0.4)
        draft.setShadowRadius(9)
        draft.customTheme.theme.surfaceEffects.glassBorderOverrideEnabled = false
        draft.customTheme.theme.surfaceEffects.glassShadowOverrideEnabled = false

        #expect(draft.customTheme.theme.metrics.borderWidth == 2.25)
        #expect(draft.customTheme.theme.metrics.shadowOpacity == 0.4)
        #expect(draft.customTheme.theme.metrics.shadowRadius == 9)
    }

    @Test("Changing material preserves authored surface values")
    func materialPreservesValues() {
        var draft = makeDraft()
        draft.setBorderWidth(3)
        draft.customTheme.theme.material = .solid
        draft.customTheme.theme.material = .glass

        #expect(draft.customTheme.theme.metrics.borderWidth == 3)
        #expect(draft.customTheme.theme.surfaceEffects.glassBorderOverrideEnabled)
    }

    private func makeDraft() -> ThemeEditorDraft {
        let model = AppearanceModel(
            store: SettingsTestStore(configuration: .default),
            customStore: ThemeTestStore()
        )
        return model.makeDraft(for: .create(baseThemeID: KeyboardTheme.funputGlass.id))
    }
}
