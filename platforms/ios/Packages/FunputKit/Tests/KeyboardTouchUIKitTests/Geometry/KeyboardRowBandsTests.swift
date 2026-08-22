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

    @Test("The gap above the inset row is split at its midpoint")
    func upperRowGapIsSplit() {
        let snapshot = makeSnapshot()

        #expect(snapshot.touchHit(at: CGPoint(x: 2, y: 151))?.key.id == "character-q")
        #expect(snapshot.touchHit(at: CGPoint(x: 2, y: 153))?.key.id == "character-a")
        #expect(snapshot.touchHit(at: CGPoint(x: 388, y: 151))?.key.id == "character-p")
        #expect(snapshot.touchHit(at: CGPoint(x: 388, y: 153))?.key.id == "character-l")
    }

    @Test("The gap below the inset row is split at its midpoint")
    func lowerRowGapIsSplit() {
        let snapshot = makeSnapshot()

        #expect(snapshot.touchHit(at: CGPoint(x: 2, y: 200))?.key.id == "character-a")
        #expect(snapshot.touchHit(at: CGPoint(x: 2, y: 204))?.key.id == "shift")
        #expect(snapshot.touchHit(at: CGPoint(x: 388, y: 200))?.key.id == "character-l")
        #expect(snapshot.touchHit(at: CGPoint(x: 388, y: 204))?.key.id == "backspace")
    }

    @Test("Every alphabetic editor gives its side runway to A and L", arguments: [
        KeyboardEditorMode.text, .search, .email, .url, .password,
    ])
    func alphabeticEditorsOwnTheirRims(editorMode: KeyboardEditorMode) {
        let snapshot = makeSnapshot(editorMode: editorMode)
        let row = snapshot.geometry.rows.first { $0.contains { $0.spec.id == "character-a" } }!
        let y = row[0].frame.midY

        #expect(snapshot.touchHit(at: CGPoint(x: 2, y: y))?.key.id == "character-a")
        #expect(snapshot.touchHit(at: CGPoint(x: 388, y: y))?.key.id == "character-l")
    }

    private func makeSnapshot(editorMode: KeyboardEditorMode = .text) -> KeyboardGeometrySnapshot {
        KeyboardGeometrySnapshot(
            revision: 1,
            geometry: KeyboardGeometry.resolve(
                layout: KeyboardLayoutResolver.resolve(
                    inputMethod: .vni,
                    mode: .letters,
                    editorMode: editorMode
                ),
                size: CGSize(width: 390, height: 304),
                sizing: KeyboardSizingProfile()
            )
        )
    }
}
#endif
