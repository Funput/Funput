import FunputShared
import Testing
import ThemeRuntime
import ThemeSchema
@testable import Funput

/// The editor's "Chữ trên phím" card (issue #499, Android #551).
@MainActor
struct ThemeLabelEditingTests {
    @Test("Label size and weight are written to the theme being edited")
    func setsSizeAndWeight() {
        var draft = makeDraft()

        draft.setLabelScale(1.15)
        draft.setLabelWeight(.bold)

        #expect(draft.customTheme.theme.metrics.fontScale == 1.15)
        #expect(draft.customTheme.theme.labelWeight == .bold)
        #expect(draft.isDirty)
    }

    @Test("The size stays inside the range the renderer will draw")
    func clampsSize() {
        var draft = makeDraft()

        draft.setLabelScale(3)
        #expect(draft.labelScale == ThemeMetrics.fontScaleRange.upperBound)
        draft.setLabelScale(0)
        #expect(draft.labelScale == ThemeMetrics.fontScaleRange.lowerBound)
    }

    @Test("Restoring the base theme also restores its label size and weight")
    func resetRestoresLabels() {
        var draft = makeDraft()
        draft.setLabelScale(1.3)
        draft.setLabelWeight(.light)

        draft.resetToBase()

        #expect(draft.customTheme.theme.metrics.fontScale == draft.baseTheme.metrics.fontScale)
        #expect(draft.customTheme.theme.labelWeight == draft.baseTheme.labelWeight)
    }

    private func makeDraft() -> ThemeEditorDraft {
        let model = AppearanceModel(
            store: SettingsTestStore(configuration: .default),
            customStore: ThemeTestStore()
        )
        return model.makeDraft(for: .create(baseThemeID: KeyboardTheme.funputGlass.id))
    }
}
