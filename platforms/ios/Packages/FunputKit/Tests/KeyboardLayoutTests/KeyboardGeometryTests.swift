import CoreGraphics
import KeyboardLayout
import Testing

struct KeyboardGeometryTests {
    @Test("QWERTY key ids are unique")
    func uniqueKeyIDs() {
        let ids = KeyboardLayout.funputQWERTY.rows.flatMap(\.keys).map(\.id)
        #expect(Set(ids).count == ids.count)
    }

    @Test("Geometry stays inside representative keyboard sizes", arguments: [
        CGSize(width: 320, height: 238),
        CGSize(width: 390, height: 280),
        CGSize(width: 568, height: 220),
        CGSize(width: 744, height: 300),
    ])
    func framesStayInsideBounds(size: CGSize) {
        let resolved = KeyboardGeometry.resolve(
            layout: .funputQWERTY,
            size: size,
            sizing: .default,
            showsInputModeKey: true
        )

        for key in resolved.keys {
            #expect(key.frame.minX >= 0)
            #expect(key.frame.minY >= 0)
            #expect(key.frame.maxX <= size.width + 0.5)
            #expect(key.frame.maxY <= size.height + 0.5)
        }
    }

    @Test("Keys do not overlap within a row")
    func keysDoNotOverlap() {
        let resolved = KeyboardGeometry.resolve(
            layout: .funputQWERTY,
            size: CGSize(width: 390, height: 280),
            sizing: .default,
            showsInputModeKey: true
        )

        for row in resolved.rows {
            for pair in zip(row, row.dropFirst()) {
                #expect(pair.0.frame.maxX <= pair.1.frame.minX)
            }
        }
    }

    @Test("Insets, weights, and accessibility metadata are preserved")
    func semanticLayoutMetrics() {
        let resolved = KeyboardGeometry.resolve(
            layout: .funputQWERTY,
            size: CGSize(width: 390, height: 280),
            sizing: .default,
            showsInputModeKey: true
        )
        let firstCharacterRow = resolved.rows[1]
        let secondCharacterRow = resolved.rows[2]
        let thirdCharacterRow = resolved.rows[3]

        #expect(secondCharacterRow[0].frame.minX > firstCharacterRow[0].frame.minX)
        #expect(thirdCharacterRow[0].frame.width > thirdCharacterRow[1].frame.width)
        #expect(thirdCharacterRow.last!.frame.width > thirdCharacterRow[1].frame.width)
        #expect(resolved.keys.allSatisfy { !$0.spec.accessibilityLabel.isEmpty })
    }

    @Test("Layout matches the Android keyboard anatomy")
    func androidLayoutAnatomy() {
        let rows = KeyboardLayout.funputQWERTY.rows
        #expect(rows.count == 5)
        #expect(rows[0].keys.map(\.label) == ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0"])
        #expect(rows[4].keys.map(\.id) == ["symbols", "comma", "space", "period", "enter"])
        let space = rows[4].keys.first { $0.role == .space }
        #expect(space?.label == "Tiếng Việt")
        #expect(space?.accessibilityLabel.contains("Vuốt") == true)
        #expect(!rows.flatMap(\.keys).contains { $0.role == .inputMode })
    }

    @Test("Customization extremes remain valid", arguments: [
        (0.85, 3.0),
        (1.15, 10.0),
    ])
    func customizationExtremes(heightScale: Double, gap: Double) {
        var sizing = KeyboardSizingProfile.default
        sizing.heightScale = heightScale
        sizing.horizontalGap = gap
        sizing.verticalGap = gap + 2
        let height = 280 * heightScale
        let resolved = KeyboardGeometry.resolve(
            layout: .funputQWERTY,
            size: CGSize(width: 390, height: height),
            sizing: sizing,
            showsInputModeKey: true
        )

        #expect(resolved.keys.allSatisfy { $0.frame.width > 0 && $0.frame.height > 0 })
        #expect(resolved.keys.allSatisfy { $0.frame.maxX <= 390.5 && $0.frame.maxY <= height + 0.5 })
    }
}
