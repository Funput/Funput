#if canImport(UIKit)
import KeyboardLayout
@testable import KeyboardRenderer
import Testing
import UIKit

@MainActor
struct SystemKeySizingMetricsTests {
    /// Padding 12 + rows + 11pt gaps + toolbar chrome 40. VNI rows: four letter rows plus
    /// a number row at 0.85 of one.
    @Test("System sizing builds the height from Apple's rows", arguments: [
        (440.0, false, 265.0), // 12 + 45×4 + 33 + 40
        (402.0, false, 257.0), // 12 + 43×4 + 33 + 40
        (440.0, true, 299.7), // 12 + 42×4 + 35.7 + 44 + 40
        (402.0, true, 291.455), // 12 + 40.3×4 + 34.255 + 44 + 40
    ])
    func systemHeights(screenWidth: CGFloat, numberRow: Bool, expected: CGFloat) {
        let layout = StandardKeyboardLayouts.letters(numberRow ? .vni : .telex, showsNumberRow: numberRow)
        #expect(layout.hasNumberRow == numberRow)
        let height = KeyboardMetrics.recommendedHeight(
            for: layout,
            traits: phonePortrait,
            sizing: .system,
            screenWidth: screenWidth
        )
        #expect(abs(height - expected) <= 0.01)
    }

    @Test("System sizing falls back to Funput sizing where Apple's was not measured")
    func landscapeAndPadFallBack() {
        let layout = StandardKeyboardLayouts.letters(.telex)
        for traits in [phoneLandscape, padPortrait] {
            #expect(KeyboardMetrics.effectiveSizing(.system, traits: traits) == .default)
            #expect(
                KeyboardMetrics.recommendedHeight(for: layout, traits: traits, sizing: .system)
                    == KeyboardMetrics.recommendedHeight(for: layout, traits: traits, sizing: .default)
            )
        }
        #expect(KeyboardMetrics.effectiveSizing(.system, traits: phonePortrait) == .system)
    }

    @Test("System sizing ignores the height setting")
    func ignoresHeightScale() {
        var scaled = KeyboardSizingProfile.system
        scaled.heightScale = 1.2
        let layout = StandardKeyboardLayouts.letters(.telex)
        #expect(
            KeyboardMetrics.phonePortraitHeight(for: layout, sizing: scaled, screenWidth: 402)
                == KeyboardMetrics.phonePortraitHeight(for: layout, sizing: .system, screenWidth: 402)
        )
    }

    private var phonePortrait: UITraitCollection { traits(.phone, .regular) }
    private var phoneLandscape: UITraitCollection { traits(.phone, .compact) }
    private var padPortrait: UITraitCollection { traits(.pad, .regular) }

    private func traits(
        _ idiom: UIUserInterfaceIdiom,
        _ verticalSizeClass: UIUserInterfaceSizeClass
    ) -> UITraitCollection {
        UITraitCollection(traitsFrom: [
            UITraitCollection(userInterfaceIdiom: idiom),
            UITraitCollection(verticalSizeClass: verticalSizeClass),
        ])
    }
}
#endif
