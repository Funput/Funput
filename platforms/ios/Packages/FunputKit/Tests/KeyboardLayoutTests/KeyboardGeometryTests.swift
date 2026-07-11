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
        let firstRow = resolved.rows[0]
        let secondRow = resolved.rows[1]
        let thirdRow = resolved.rows[2]

        #expect(secondRow[0].frame.minX > firstRow[0].frame.minX)
        #expect(thirdRow[0].frame.width > thirdRow[1].frame.width)
        #expect(thirdRow.last!.frame.width > thirdRow[1].frame.width)
        #expect(resolved.keys.allSatisfy { !$0.spec.accessibilityLabel.isEmpty })
    }

    @Test("Input mode key is conditional")
    func inputModeKeyVisibility() {
        let size = CGSize(width: 390, height: 280)
        let shown = KeyboardGeometry.resolve(
            layout: .funputQWERTY,
            size: size,
            sizing: .default,
            showsInputModeKey: true
        )
        let hidden = KeyboardGeometry.resolve(
            layout: .funputQWERTY,
            size: size,
            sizing: .default,
            showsInputModeKey: false
        )

        #expect(shown.keys.contains { $0.spec.role == .inputMode })
        #expect(!hidden.keys.contains { $0.spec.role == .inputMode })
        #expect(hidden.keys.first { $0.spec.role == .space }!.frame.width > shown.keys.first { $0.spec.role == .space }!.frame.width)
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
