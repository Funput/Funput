#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

@MainActor
struct KeyboardMetricsTests {
    @Test("Device traits select the correct standard height")
    func deviceBaseHeights() {
        let layout = StandardKeyboardLayouts.letters(.telex)
        expectHeight(layout, traits: phonePortrait, expected: 304)
        expectHeight(layout, traits: phoneLandscape, expected: 236)
        expectHeight(layout, traits: padPortrait, expected: 324)
    }

    @Test("Toolbar and row count determine variant heights")
    func layoutVariantHeights() {
        let cases: [(UITraitCollection, CGFloat, CGFloat)] = [
            (phonePortrait, 254, 204.2),
            (phoneLandscape, 186, 149.8),
            (padPortrait, 274, 220.2),
        ]
        let secureLayouts = [
            PasswordKeyboardLayouts.text(.telex),
            SymbolKeyboardLayouts.primary(.telex, secure: true),
            SymbolKeyboardLayouts.secondary(.telex, secure: true),
        ]
        let keypadLayouts = [
            PhoneKeyboardLayouts.resolve(.telex),
            PasswordKeyboardLayouts.pin(.telex),
            NumberKeyboardLayouts.resolve(.telex, mode: .number),
            NumberKeyboardLayouts.resolve(.telex, mode: .numberDecimal),
            NumberKeyboardLayouts.resolve(.telex, mode: .numberSigned),
            NumberKeyboardLayouts.resolve(.telex, mode: .numberSignedDecimal),
        ]

        for (traits, secureHeight, keypadHeight) in cases {
            for layout in secureLayouts {
                expectHeight(layout, traits: traits, expected: secureHeight)
            }
            for layout in keypadLayouts {
                expectHeight(layout, traits: traits, expected: keypadHeight)
            }
        }
    }

    @Test("Height scale stays within supported bounds")
    func scaleBounds() {
        let layout = StandardKeyboardLayouts.letters(.telex)
        expectHeight(layout, traits: phonePortrait, scale: 0.5, expected: 304 * 0.85)
        expectHeight(layout, traits: phonePortrait, scale: 2, expected: 304 * 1.15)
    }

    @Test("Compact Telex pages share one height below the standard family")
    func compactFamilyHeight() {
        let compact = KeyboardLayoutMode.allCases.map { mode in
            KeyboardLayoutResolver.resolve(
                inputMethod: .telex,
                mode: mode,
                showsNumberRow: false
            )
        }
        for traits in [phonePortrait, phoneLandscape, padPortrait] {
            let heights = compact.map {
                KeyboardMetrics.recommendedHeight(for: $0, traits: traits)
            }
            #expect(Set(heights).count == 1)
            #expect(heights[0] < KeyboardMetrics.recommendedHeight(
                for: StandardKeyboardLayouts.letters(.telex),
                traits: traits
            ))
        }
    }

    @Test("Surface intrinsic height follows presentation layout")
    func intrinsicHeightUpdates() {
        let surface = KeyboardSurfaceView(
            presentation: KeyboardPresentation(layout: StandardKeyboardLayouts.letters(.telex))
        )
        let standardHeight = surface.intrinsicContentSize.height
        surface.presentation.layout = PhoneKeyboardLayouts.resolve(.telex)
        let keypadHeight = surface.intrinsicContentSize.height

        #expect(keypadHeight < standardHeight)
        expectClose(
            keypadHeight,
            KeyboardMetrics.recommendedHeight(
                for: surface.presentation.layout,
                traits: surface.traitCollection
            )
        )
    }

    private var phonePortrait: UITraitCollection {
        traits(idiom: .phone, verticalSizeClass: .regular)
    }

    private var phoneLandscape: UITraitCollection {
        traits(idiom: .phone, verticalSizeClass: .compact)
    }

    private var padPortrait: UITraitCollection {
        traits(idiom: .pad, verticalSizeClass: .regular)
    }

    private func traits(
        idiom: UIUserInterfaceIdiom,
        verticalSizeClass: UIUserInterfaceSizeClass
    ) -> UITraitCollection {
        UITraitCollection { traits in
            traits.userInterfaceIdiom = idiom
            traits.verticalSizeClass = verticalSizeClass
        }
    }

    private func expectHeight(
        _ layout: KeyboardLayout,
        traits: UITraitCollection,
        scale: CGFloat = 1,
        expected: CGFloat
    ) {
        expectClose(
            KeyboardMetrics.recommendedHeight(for: layout, traits: traits, scale: scale),
            expected
        )
    }

    private func expectClose(_ actual: CGFloat, _ expected: CGFloat) {
        #expect(abs(actual - expected) <= 0.01)
    }
}
#endif
