#if canImport(UIKit)
@testable import KeyboardRenderer
import Testing
import UIKit

struct KeyboardAlternatePaletteLayoutTests {
    @Test("Palette stays in bounds and wraps for compact widths")
    func adaptiveGeometry() {
        for bounds in [
            CGRect(x: 0, y: 0, width: 320, height: 238),
            CGRect(x: 0, y: 0, width: 390, height: 304),
            CGRect(x: 0, y: 0, width: 744, height: 324),
        ] {
            let source = CGRect(x: bounds.maxX - 42, y: 170, width: 36, height: 44)
            let layout = KeyboardAlternatePaletteLayout.resolve(
                count: 18,
                sourceFrame: source,
                bounds: bounds
            )
            #expect(bounds.contains(layout.frame))
            #expect(layout.itemFrames.count == 18)
            #expect(layout.itemFrames.allSatisfy {
                CGRect(origin: .zero, size: layout.frame.size).contains($0)
            })
        }
    }

    @Test("Source selects the base and cells select their own index")
    func hitTesting() {
        let source = CGRect(x: 120, y: 220, width: 36, height: 44)
        let layout = KeyboardAlternatePaletteLayout.resolve(
            count: 18,
            sourceFrame: source,
            bounds: CGRect(x: 0, y: 0, width: 390, height: 304)
        )
        #expect(layout.index(at: CGPoint(x: source.midX, y: source.midY)) == 0)
        let cell = layout.itemFrames[10].offsetBy(dx: layout.frame.minX, dy: layout.frame.minY)
        #expect(layout.index(at: CGPoint(x: cell.midX, y: cell.midY)) == 10)
        #expect(layout.index(at: CGPoint(x: 389, y: 303)) == nil)
    }
}
#endif
