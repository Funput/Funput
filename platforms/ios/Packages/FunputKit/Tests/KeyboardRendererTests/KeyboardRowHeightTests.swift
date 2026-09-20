#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

/// What a row measures once the keyboard, rather than the keys, absorbed the padding that
/// used to sit below them.
@MainActor
struct KeyboardRowHeightTests {
    @Test("Keys keep their height now that nothing pads the bottom")
    func rowsKeepTheirHeight() {
        let layout = StandardKeyboardLayouts.letters(.telex)
        let height = KeyboardMetrics.recommendedHeight(for: layout, traits: phonePortrait, scale: 1)
        let geometry = KeyboardGeometry.resolve(
            layout: layout,
            size: CGSize(width: 402, height: height),
            sizing: .default
        )

        // 43pt is what a row measured before the bottom padding went — the keyboard gave
        // up those 6pt, not the keys. (The geometry rounds, so 216pt over five rows and
        // four 7pt gaps lands here either way.)
        expectClose(geometry.rows[0][0].frame.height, 43)
        // And the rows now end flush with the view, since iOS's globe bar is below it.
        expectClose(geometry.rows[geometry.rows.count - 1][0].frame.maxY, height)
    }

    private var phonePortrait: UITraitCollection {
        UITraitCollection(traitsFrom: [
            UITraitCollection(userInterfaceIdiom: .phone),
            UITraitCollection(verticalSizeClass: .regular),
        ])
    }

    private func expectClose(_ actual: CGFloat, _ expected: CGFloat) {
        #expect(abs(actual - expected) <= 0.01)
    }
}
#endif
