import CoreGraphics
import FunputShared
import KeyboardLayout
import KeyboardRenderer
import Testing
import ThemeRuntime
import ThemeSchema
@testable import Funput

@MainActor
struct AppearancePreviewDockTests {
    @Test("The previewed theme's row follows system and custom themes")
    func previewSectionFollowsTheme() {
        let model = makeModel()
        #expect(model.previewSection == .system)

        model.selectTheme(KeyboardTheme.midnight.id)
        #expect(model.previewSection == .system)

        // Saving a system theme as a custom one moves the preview to "Của bạn".
        #expect(model.save(model.makeDraft(for: .create(baseThemeID: KeyboardTheme.midnight.id))))
        #expect(model.previewSection == .custom)

        #expect(model.deletePreviewCustomTheme())
        #expect(model.previewSection == .system)
    }

    @Test("Rows keep the identifiers the scroll view and UI tests rely on")
    func sectionIdentifiers() {
        #expect(ThemeGallerySection.system.scrollID == "appearance.section.system")
        #expect(ThemeGallerySection.custom.scrollID == "appearance.section.custom")
    }

    @Test("The preview keeps its natural height until the screen is too short")
    func previewHeightFitsLimit() {
        let presentation = makeModel().previewPresentation
        let natural = KeyboardMetrics.phonePortraitHeight(
            for: presentation.layout,
            sizing: presentation.previewSizing
        )

        #expect(presentation.previewHeight(limit: .infinity) == natural)
        #expect(presentation.previewHeight(limit: natural - 40) == natural - 40)
        #expect(presentation.previewHeight(limit: 10) == 148)
    }

    private func makeModel() -> AppearanceModel {
        AppearanceModel(
            store: SettingsTestStore(configuration: .default),
            customStore: ThemeTestStore()
        )
    }
}
