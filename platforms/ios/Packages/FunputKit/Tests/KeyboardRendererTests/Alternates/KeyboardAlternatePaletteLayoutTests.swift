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

    @Test("The palette rises from the key instead of dropping below it")
    func risesAbove() {
        let bounds = CGRect(x: 0, y: 0, width: 390, height: 260)
        for row in [CGFloat(62), 111, 160, 209] {
            let source = CGRect(x: 156, y: row, width: 36, height: 40)
            let layout = KeyboardAlternatePaletteLayout.resolve(
                count: 13,
                sourceFrame: source,
                bounds: bounds
            )
            #expect(layout.frame.maxY <= source.minY)
            #expect(!layout.overlapsSource)
        }
    }

    @Test("The palette wraps into at most three rows instead of running wide")
    func wrapsWithinThreeRows() {
        let bounds = CGRect(x: 0, y: 0, width: 390, height: 260)
        for row in [CGFloat(62), 209] {
            let source = CGRect(x: 156, y: row, width: 36, height: 40)
            for count in [13, 18, 19] {
                let layout = KeyboardAlternatePaletteLayout.resolve(
                    count: count,
                    sourceFrame: source,
                    bounds: bounds
                )
                let rows = Set(layout.itemFrames.map(\.minY)).count
                #expect(rows > 1)
                #expect(rows <= 3)
                #expect(layout.frame.width <= bounds.width * 0.8)
            }
        }
    }

    @Test("Vietnamese diacritic keys keep the palette above a top-row hold")
    func vietnameseTopRow() {
        let bounds = CGRect(x: 0, y: 0, width: 390, height: 304)
        for count in [6, 12, 18] {
            let source = CGRect(x: 102, y: 62, width: 36, height: 40)
            let layout = KeyboardAlternatePaletteLayout.resolve(
                count: count,
                sourceFrame: source,
                bounds: bounds
            )
            #expect(layout.frame.maxY <= source.minY)
            #expect(!layout.overlapsSource)
        }
    }

    @Test("Short sets stay on one row")
    func shortSets() {
        let layout = KeyboardAlternatePaletteLayout.resolve(
            count: 2,
            sourceFrame: CGRect(x: 156, y: 160, width: 36, height: 40),
            bounds: CGRect(x: 0, y: 0, width: 390, height: 260)
        )
        #expect(Set(layout.itemFrames.map(\.minY)).count == 1)
        #expect(layout.itemFrames.count == 2)
    }

    @Test("A top-row Vietnamese catalog overflows above the surface instead of covering the key")
    func overflowsAboveSurface() {
        let source = CGRect(x: 300, y: 62, width: 36, height: 40)
        let layout = KeyboardAlternatePaletteLayout.resolve(
            count: 19,
            sourceFrame: source,
            bounds: CGRect(x: 0, y: 0, width: 390, height: 260)
        )
        #expect(layout.frame.maxY <= source.minY)
        #expect(!layout.overlapsSource)
        #expect(layout.overflowAbove > 0)
        let start = CGPoint(x: source.midX, y: source.midY)
        #expect(layout.selection(at: start, from: start) == 0)
    }

    @Test("A palette overlapping its key keeps the default until the finger travels")
    func holdSlopWhenOverlapping() {
        let source = CGRect(x: 300, y: 62, width: 36, height: 40)
        let layout = KeyboardAlternatePaletteLayout(
            frame: CGRect(x: 280, y: 50, width: 76, height: 60),
            itemFrames: [
                CGRect(x: 2, y: 2, width: 34, height: 38),
                CGRect(x: 38, y: 2, width: 36, height: 38),
            ],
            sourceFrame: source,
            overlapsSource: true
        )
        let start = CGPoint(x: source.midX, y: source.midY)
        #expect(layout.selection(at: start, from: start) == 0)
        #expect(layout.selection(at: CGPoint(x: start.x + 6, y: start.y), from: start) == 0)
        let covering = layout.index(at: start)
        #expect(covering != nil)
        #expect(covering != 0)
        #expect(layout.selection(at: start, from: CGPoint(x: start.x, y: start.y + 60))
            == covering)
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
