#if canImport(UIKit)
import KeyboardLayout
@testable import KeyboardRenderer
import Testing
import UIKit

@MainActor
struct KeyboardMetricsPadTests {
    @Test("iPad rows follow the key width, within limits")
    func rowHeights() {
        // On a 1024pt screen a letter key is (1024 - 12 - 60) / 13 wide and 0.9 of that tall.
        let row = (1024.0 - 12 - 60) / 13 * 0.9
        // 6 top padding + 38 toolbar + 5 rows + 4 gaps of 7.
        expectClose(height(letters, width: 1024), 6 + 38 + 5 * row + 28)
        // Without a toolbar the 4pt top inset replaces the band: 10 + 4 rows + 3 gaps.
        expectClose(height(keypad, width: 1024), 10 + 4 * row + 21)
        // Narrow windows and very wide screens stop at the row height limits.
        expectClose(height(letters, width: 500), 6 + 38 + 5 * 50 + 28)
        expectClose(height(letters, width: 2000), 6 + 38 + 5 * 68 + 28)
    }

    @Test("The height setting scales the whole iPad keyboard")
    func heightScale() {
        let base = height(letters, width: 1024)
        expectClose(height(letters, width: 1024, scale: 1.2), base * 1.2)
        expectClose(height(letters, width: 1024, scale: 0.85), base * 0.85)
    }

    @Test("Switching pages keeps the iPad keyboard height")
    func pageSwitchKeepsHeight() {
        let heights = KeyboardLayoutMode.allCases.map { mode in
            height(
                KeyboardLayoutResolver.resolve(
                    inputMethod: .telex,
                    mode: mode,
                    showsNumberRow: false
                ),
                width: 1024
            )
        }
        #expect(Set(heights.map { ($0 * 100).rounded() }).count == 1)
    }

    private var letters: KeyboardLayout {
        StandardKeyboardLayouts.letters(.telex)
    }

    private var keypad: KeyboardLayout { PhoneKeyboardLayouts.resolve(.telex) }

    private func height(_ layout: KeyboardLayout, width: CGFloat, scale: CGFloat = 1) -> CGFloat {
        KeyboardMetrics.recommendedHeight(
            for: layout,
            traits: UITraitCollection(traitsFrom: [
                UITraitCollection(userInterfaceIdiom: .pad),
                UITraitCollection(verticalSizeClass: .regular),
            ]),
            sizing: KeyboardSizingProfile(heightScale: scale),
            screenWidth: width
        )
    }

    private func expectClose(_ actual: CGFloat, _ expected: CGFloat) {
        #expect(abs(actual - expected) <= 0.01)
    }
}
#endif
