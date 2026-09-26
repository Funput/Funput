#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

@MainActor
struct KeyboardAlternatePaletteColumnsTests {
    @Test("A requested column count shapes the period palette at every width")
    func requestedColumns() {
        for bounds in [
            CGRect(x: 0, y: 0, width: 320, height: 238),
            CGRect(x: 0, y: 0, width: 390, height: 304),
            CGRect(x: 0, y: 0, width: 744, height: 324),
        ] {
            let source = CGRect(x: bounds.maxX * 0.7, y: bounds.maxY - 50, width: 36, height: 44)
            let layout = KeyboardAlternatePaletteLayout.resolve(
                count: 15,
                columns: 8,
                sourceFrame: source,
                bounds: bounds
            )
            #expect(bounds.contains(layout.frame))
            #expect(Set(layout.itemFrames.map(\.minY)).count == 2)
            #expect(Set(layout.itemFrames.map(\.minX)).count == 8)
            #expect(layout.frame.maxY <= source.minY)
        }
    }

    @Test("A request never widens the palette past what fits")
    func clampedByWidth() {
        let layout = KeyboardAlternatePaletteLayout.resolve(
            count: 15,
            columns: 15,
            sourceFrame: CGRect(x: 100, y: 120, width: 30, height: 40),
            bounds: CGRect(x: 0, y: 0, width: 200, height: 170)
        )
        #expect(Set(layout.itemFrames.map(\.minX)).count < 15)
        #expect(layout.frame.width <= 200)
    }

    @Test("Without a request the rows stay balanced as before")
    func automaticColumns() {
        let layout = KeyboardAlternatePaletteLayout.resolve(
            count: 15,
            sourceFrame: CGRect(x: 156, y: 209, width: 36, height: 40),
            bounds: CGRect(x: 0, y: 0, width: 390, height: 260)
        )
        #expect(Set(layout.itemFrames.map(\.minX)).count == 5)
        #expect(Set(layout.itemFrames.map(\.minY)).count == 3)
    }

    @Test("Period symbols keep their spoken names, even with Shift on")
    func periodAccessibility() throws {
        var presentation = KeyboardPresentation(layout: StandardKeyboardLayouts.letters(.telex))
        presentation.shiftState = .uppercase
        let surface = KeyboardSurfaceView(presentation: presentation)
        surface.frame = CGRect(x: 0, y: 0, width: 390, height: 304)
        surface.layoutIfNeeded()

        let key = try #require(controls(in: surface).first { $0.accessibilityLabel == "Dấu chấm" })
        let names = key.accessibilityCustomActions?.map(\.name) ?? []
        #expect(names.first == "Chọn a còng")
        #expect(names.contains("Chọn dấu hỏi"))
        #expect(names.count == PunctuationKeyAlternates.period.count)
    }

    private func controls(in view: UIView) -> [UIControl] {
        view.subviews.flatMap { child in
            let own = child as? UIControl
            return (own.map { [$0] } ?? []) + controls(in: child)
        }
    }
}
#endif
