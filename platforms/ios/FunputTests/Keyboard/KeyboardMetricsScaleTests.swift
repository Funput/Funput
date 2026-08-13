import KeyboardLayout
import KeyboardRenderer
import Testing
import UIKit

@MainActor
struct KeyboardMetricsScaleTests {
    @Test("Every layout family honors supported height scales", arguments: [0.85, 1.2])
    func layoutFamilies(scale: CGFloat) {
        let layouts = [
            StandardKeyboardLayouts.letters(.telex),
            PasswordKeyboardLayouts.text(.telex),
            PhoneKeyboardLayouts.resolve(.telex),
        ]
        let traits = [
            makeTraits(idiom: .phone, vertical: .regular),
            makeTraits(idiom: .phone, vertical: .compact),
            makeTraits(idiom: .pad, vertical: .regular),
        ]

        for layout in layouts {
            for trait in traits {
                let base = KeyboardMetrics.recommendedHeight(for: layout, traits: trait)
                let scaled = KeyboardMetrics.recommendedHeight(
                    for: layout,
                    traits: trait,
                    scale: scale
                )
                #expect(abs(scaled - base * scale) <= 0.01)
            }
        }
    }

    private func makeTraits(
        idiom: UIUserInterfaceIdiom,
        vertical: UIUserInterfaceSizeClass
    ) -> UITraitCollection {
        UITraitCollection { traits in
            traits.userInterfaceIdiom = idiom
            traits.verticalSizeClass = vertical
        }
    }
}
