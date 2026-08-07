#if canImport(UIKit)
import CoreGraphics
import KeyboardLayout
import KeyboardTouchUIKit
import Testing

/// The ASDF row is inset half a key on each side, standard for QWERTY. That leaves a ~25pt
/// strip to the left of `a` where the nearest keycap by plain distance is `q` on the row
/// above, not `a` on the row the finger is actually in.
///
/// Geometrically correct, and wrong about intent. A thumb resting in the ASDF row's vertical
/// band means an ASDF key; getting `q` in the middle of a Vietnamese word produces garbage,
/// which is what "the key did not register" feels like from the outside.
@MainActor
struct KeyboardRowBandsTests {
    @Test("The inset row owns its own vertical band at the rim")
    func insetRowRimStaysInItsRow() {
        let snapshot = makeSnapshot()

        // Upper part of the ASDF band, hard against each edge.
        #expect(snapshot.touchHit(at: CGPoint(x: 2, y: 157))?.key.id == "character-a")
        #expect(snapshot.touchHit(at: CGPoint(x: 388, y: 157))?.key.id == "character-l")
    }

    /// Lower in the same band the row below wins instead — `shift` and `backspace` run the full
    /// width, so they are nearer than the inset `a` and `l`. This half is the worse one: a
    /// modifier produces no character at all, so the tap looks like it was never seen.
    @Test("The rim does not fall through to the row below either")
    func insetRowRimDoesNotReachTheModifiers() {
        let snapshot = makeSnapshot()

        #expect(snapshot.touchHit(at: CGPoint(x: 2, y: 195))?.key.id == "character-a")
        #expect(snapshot.touchHit(at: CGPoint(x: 388, y: 195))?.key.id == "character-l")
    }

    /// Between two rows no band contains the point, so the global nearest-key search still
    /// decides. This is the path every row gap takes, and it must not change.
    @Test("A point in the row gap still falls back to the nearest key")
    func rowGapFallsBackToNearest() {
        let snapshot = makeSnapshot()
        // The 7pt gap between the qwerty row (…148.6) and the ASDF row (155.6…).
        let hit = snapshot.touchHit(at: CGPoint(x: 200, y: 152))

        #expect(hit != nil)
    }

    private func makeSnapshot() -> KeyboardGeometrySnapshot {
        KeyboardGeometrySnapshot(
            revision: 1,
            geometry: KeyboardGeometry.resolve(
                layout: KeyboardLayoutResolver.resolve(inputMethod: .vni, mode: .letters),
                size: CGSize(width: 390, height: 304),
                sizing: KeyboardSizingProfile()
            )
        )
    }
}
#endif
