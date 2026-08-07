#if canImport(UIKit)
import CoreGraphics
import KeyboardLayout
import KeyboardTouchUIKit
import Testing

/// Sweeps the whole keyboard face at half-point steps.
///
/// `touchHit` picks the nearest keycap by distance-to-rect with no cutoff, so the 5pt column
/// gaps and 7pt row gaps should never swallow a tap. This asserts that rather than trusting
/// the reasoning: a future distance threshold, or a layout that leaves a real hole, fails here.
@MainActor
struct KeyboardTrackingSweepTests {
    private let size = CGSize(width: 390, height: 304)

    @Test("Every point inside the tracking region resolves to a key")
    func noHolesInsideTheRegion() {
        let (snapshot, bounds, _) = makeGeometry()
        var holes: [CGPoint] = []

        forEachHalfPoint { point in
            guard bounds.contains(point), snapshot.touchHit(at: point) == nil else { return }
            holes.append(point)
        }

        #expect(holes.isEmpty, "\(holes.count) point(s) fell through, first: \(holes.first as Any)")
    }

    /// The stronger invariant: inside a row's band, the winner belongs to that row. Sweeping it
    /// rather than spot-checking the rim, because an inset row is not the only way a neighbour
    /// can end up nearer than the key the finger is actually over.
    @Test("Inside a row band the hit always belongs to that row")
    func bandOwnsItsRow() {
        let (snapshot, bounds, geometry) = makeGeometry()
        var strays: [(CGPoint, String)] = []

        for row in geometry.rows {
            let keys = row.filter { $0.spec.role != .placeholder }
            guard let minY = keys.map(\.frame.minY).min(),
                  let maxY = keys.map(\.frame.maxY).max() else { continue }
            let ids = Set(keys.map(\.spec.id))
            var y = minY
            while y <= maxY {
                var x = 0.0
                while x <= size.width {
                    let point = CGPoint(x: x, y: y)
                    if bounds.contains(point),
                       let hit = snapshot.touchHit(at: point),
                       !ids.contains(hit.key.id) {
                        strays.append((point, hit.key.id))
                    }
                    x += 0.5
                }
                y += 0.5
            }
        }

        #expect(strays.isEmpty, "\(strays.count) stray(s), first: \(strays.first as Any)")
    }

    /// The region is built by padding the keycap union, so it can reach past the surface's own
    /// bounds — and UIKit never routes a touch to a view outside those bounds. The slack that
    /// escapes is therefore dead, and the tolerance a finger actually gets at the rim is the
    /// layout's padding, not `outerTolerance`. Kept as an assertion so the gap between the two
    /// numbers stays visible instead of reading as a 12pt promise.
    @Test("Rim tolerance is bounded by the surface, not by outerTolerance")
    func rimToleranceIsClampedByTheSurface() {
        let (_, bounds, geometry) = makeGeometry()
        let keys = geometry.keys.map(\.frame)
        let leftmost = keys.map(\.minX).min() ?? 0

        #expect(bounds.minX < 0)
        #expect(bounds.maxX > size.width)
        // Outside the surface the region cannot receive anything, so the usable slack on the
        // left is the distance from the view edge to the first keycap.
        #expect(leftmost == 6)
        #expect(KeyboardTrackingBounds.outerTolerance == 12)
    }

    private func makeGeometry() -> (KeyboardGeometrySnapshot, CGRect, ResolvedKeyboard) {
        let geometry = KeyboardGeometry.resolve(
            layout: KeyboardLayoutResolver.resolve(inputMethod: .vni, mode: .letters),
            size: size,
            sizing: KeyboardSizingProfile()
        )
        return (
            KeyboardGeometrySnapshot(revision: 1, geometry: geometry),
            KeyboardTrackingBounds.resolve(for: geometry),
            geometry
        )
    }

    private func forEachHalfPoint(_ body: (CGPoint) -> Void) {
        var y = 0.0
        while y <= size.height {
            var x = 0.0
            while x <= size.width {
                body(CGPoint(x: x, y: y))
                x += 0.5
            }
            y += 0.5
        }
    }
}
#endif
