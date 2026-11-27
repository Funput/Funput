import CoreGraphics
import FunputShared
import KeyboardLayout
import KeyboardRenderer
import SwiftUI
import Testing
import ThemeRuntime
import ThemeSchema
import UIKit
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

    /// At xxxLarge text on a 440pt-wide phone the bar squeezed "Áp dụng" onto two lines
    /// (60pt tall became 93pt) instead of switching to its icon-only row.
    @Test("The apply button never wraps, so the bar keeps one row height", arguments: [375.0, 402.0, 440.0])
    func applyLabelDoesNotWrap(width: Double) {
        func barHeight(isApplied: Bool) -> CGFloat {
            let bar = AppearancePreviewDockBar(
                mode: .constant(.light),
                isApplied: isApplied,
                isCustom: false,
                apply: {},
                customize: {},
                close: {}
            )
            .environment(\.dynamicTypeSize, .xxxLarge)
            return UIHostingController(rootView: bar)
                .sizeThatFits(in: CGSize(width: width, height: 400)).height
        }

        // The prominent button runs a couple of points taller than the status label;
        // a second line of text adds a whole line height.
        #expect(barHeight(isApplied: false) < barHeight(isApplied: true) + 10)
    }

    private func makeModel() -> AppearanceModel {
        AppearanceModel(
            store: SettingsTestStore(configuration: .default),
            customStore: ThemeTestStore()
        )
    }
}
