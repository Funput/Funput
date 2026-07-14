import FunputShared
import SwiftUI
import Testing
import ThemeRuntime
import ThemeSchema
@testable import Funput

@MainActor
struct ThemeColorEditingTests {
    @Test("Editing Light preserves Dark and the authored alpha")
    func editsOnlyActiveMode() {
        var draft = makeDraft()
        let originalDark = draft.customTheme.theme.palette.backgroundStart.dark
        let originalAlpha = draft.customTheme.theme.palette.backgroundStart.light.alpha

        draft.previewMode = .light
        draft.setColor(Color(.sRGB, red: 0.2, green: 0.4, blue: 0.6), for: .backgroundStart)
        let edited = draft.customTheme.theme.palette.backgroundStart

        #expect(abs(edited.light.red - 0.2) < 0.01)
        #expect(edited.light.alpha == originalAlpha)
        #expect(edited.dark == originalDark)
        #expect(draft.customTheme.theme.colorEffects.glassBackgroundTintEnabled)
    }

    @Test("Editing key colors enables controlled Glass tint")
    func enablesKeyTint() {
        var draft = makeDraft()
        #expect(!draft.customTheme.theme.colorEffects.glassKeyTintEnabled)

        draft.setColor(.red, for: .specialKey)

        #expect(draft.customTheme.theme.colorEffects.glassKeyTintEnabled)
    }

    @Test("Pressed overlay retains its color while disabled")
    func pressedColorRetention() {
        var draft = makeDraft()
        draft.setColor(.green, for: .pressedOverlay)
        let selected = draft.customTheme.theme.colorEffects.pressedOverlay
        draft.customTheme.theme.colorEffects.pressedOverlayEnabled = true
        draft.customTheme.theme.colorEffects.pressedOverlayEnabled = false
        draft.customTheme.theme.colorEffects.pressedOverlayEnabled = true

        #expect(draft.customTheme.theme.colorEffects.pressedOverlay == selected)
    }

    @Test("Reset restores every editable property but keeps the name")
    func resetKeepsName() {
        var draft = makeDraft()
        draft.customTheme.theme.metadata.name = "My Colors"
        draft.customTheme.theme.geometry.horizontalGap = 10
        draft.customTheme.theme.palette.characterKey = solid(.red)
        draft.customTheme.theme.colorEffects.glassKeyTintEnabled = true
        draft.customTheme.theme.surfaceEffects.glassBorderOverrideEnabled = true
        draft.customTheme.theme.surfaceEffects.glassShadowOverrideEnabled = true
        draft.customTheme.theme.material = .solid
        draft.customTheme.theme.metrics.keyOpacity = 0.30
        draft.customTheme.theme.metrics.specialKeyOpacity = 0.35
        draft.customTheme.theme.metrics.cornerRadius = 18
        draft.customTheme.theme.metrics.borderWidth = 4
        draft.customTheme.theme.metrics.shadowOpacity = 0.5
        draft.customTheme.theme.metrics.shadowRadius = 12
        draft.customTheme.theme.metrics.pressedScale = 0.90

        draft.resetToBase()

        #expect(draft.customTheme.theme.metadata.name == "My Colors")
        #expect(draft.customTheme.theme.geometry == draft.baseTheme.geometry)
        #expect(draft.customTheme.theme.palette == draft.baseTheme.palette)
        #expect(draft.customTheme.theme.colorEffects == draft.baseTheme.colorEffects)
        #expect(draft.customTheme.theme.surfaceEffects == draft.baseTheme.surfaceEffects)
        #expect(draft.customTheme.theme.material == draft.baseTheme.material)
        #expect(draft.customTheme.theme.metrics.keyOpacity == draft.baseTheme.metrics.keyOpacity)
        #expect(draft.customTheme.theme.metrics.specialKeyOpacity == draft.baseTheme.metrics.specialKeyOpacity)
        #expect(draft.customTheme.theme.metrics.cornerRadius == draft.baseTheme.metrics.cornerRadius)
        #expect(draft.customTheme.theme.metrics.borderWidth == draft.baseTheme.metrics.borderWidth)
        #expect(draft.customTheme.theme.metrics.shadowOpacity == draft.baseTheme.metrics.shadowOpacity)
        #expect(draft.customTheme.theme.metrics.shadowRadius == draft.baseTheme.metrics.shadowRadius)
        #expect(draft.customTheme.theme.metrics.pressedScale == draft.baseTheme.metrics.pressedScale)
    }

    @Test("Low contrast warns but does not prevent saving")
    func warningDoesNotBlockSave() {
        let themeStore = ThemeTestStore()
        let model = AppearanceModel(
            store: SettingsTestStore(configuration: .default),
            customStore: themeStore
        )
        var draft = model.makeDraft(for: .create(baseThemeID: KeyboardTheme.funputGlass.id))
        let muddy = solid(ThemeRGBA(hex: 0x777777))
        draft.customTheme.theme.palette.characterKey = muddy
        draft.customTheme.theme.palette.label = muddy

        #expect(!ThemeValidator.validate(draft.customTheme.theme).isEmpty)
        #expect(draft.canSave)
        #expect(model.save(draft))
    }

    private func makeDraft() -> ThemeEditorDraft {
        let model = AppearanceModel(
            store: SettingsTestStore(configuration: .default),
            customStore: ThemeTestStore()
        )
        return model.makeDraft(for: .create(baseThemeID: KeyboardTheme.funputGlass.id))
    }

    private func solid(_ color: Color) -> AdaptiveThemeColor {
        let value = color == .red ? ThemeRGBA(hex: 0xFF0000) : ThemeRGBA(hex: 0x00FF00)
        return solid(value)
    }

    private func solid(_ color: ThemeRGBA) -> AdaptiveThemeColor {
        AdaptiveThemeColor(light: color, dark: color)
    }
}
