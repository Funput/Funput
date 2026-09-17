import CoreGraphics
import KeyboardLayout
import Testing

struct SystemKeySizingGeometryTests {
    @Test("Funput sizing keeps every row the same height")
    func funputRowsAreEqual() {
        let resolved = KeyboardGeometry.resolve(
            layout: StandardKeyboardLayouts.letters(.vni),
            size: CGSize(width: 402, height: 300),
            sizing: .default
        )
        let heights = Set(resolved.rows.map { $0[0].frame.height })
        #expect(heights.count == 1)
    }

    @Test("System sizing draws the number row shorter, with Apple's gaps")
    func systemNumberRowIsShorter() throws {
        let layout = StandardKeyboardLayouts.letters(.vni)
        #expect(layout.rows[0].isNumberRow)
        let resolved = KeyboardGeometry.resolve(
            layout: layout,
            size: CGSize(width: 440, height: 300),
            sizing: .system
        )
        let digit = try #require(resolved.rows.first?.first).frame
        let letter = resolved.rows[1][0].frame
        let secondLetter = resolved.rows[1][1].frame

        #expect(abs(digit.height / letter.height - SystemKeyMetrics.numberRowHeightRatio) < 0.02)
        #expect(abs(letter.minY - digit.maxY - SystemKeyMetrics.verticalGap) <= 0.5)
        #expect(abs(secondLetter.minX - letter.maxX - SystemKeyMetrics.horizontalGap) <= 0.5)
        #expect(abs(letter.minX - SystemKeyMetrics.horizontalPadding) <= 0.5)
    }

    @Test("A compact Telex layout has no number row")
    func telexCompactHasNoNumberRow() {
        #expect(!StandardKeyboardLayouts.letters(.telex, showsNumberRow: false).hasNumberRow)
    }
}
