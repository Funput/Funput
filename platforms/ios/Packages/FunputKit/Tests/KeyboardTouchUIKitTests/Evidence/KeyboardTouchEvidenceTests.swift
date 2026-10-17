#if canImport(UIKit)
import CoreGraphics
import KeyboardLayout
import Testing

@testable import KeyboardTouchUIKit

/// What the keys around a touch say about it — the evidence typo correction scores.
@Suite("Touch evidence")
struct KeyboardTouchEvidenceTests {
    /// Three letter keys in a row, one pitch apart, each 40pt wide with a 10pt gap.
    private func snapshot() -> KeyboardGeometrySnapshot {
        let keys = ["a", "s", "d"].enumerated().map { index, label in
            ResolvedKey(
                spec: KeySpec(id: label, label: label, role: .character),
                frame: CGRect(x: CGFloat(index) * 50, y: 0, width: 40, height: 40)
            )
        }
        return KeyboardGeometrySnapshot(
            revision: 1,
            geometry: ResolvedKeyboard(
                size: CGSize(width: 150, height: 40),
                toolbarFrame: nil,
                rows: [keys],
                pitch: CGSize(width: 50, height: 50)
            )
        )
    }

    private func key(_ label: String) -> KeySpec {
        KeySpec(id: label, label: label, role: .character)
    }

    @Test("A touch dead on a key centre is reported at no distance")
    func centred() throws {
        let evidence = try #require(
            snapshot().touchEvidence(at: CGPoint(x: 70, y: 20), typed: key("s"))
        )
        #expect(evidence.typed == "s")
        #expect(evidence.typedDistance < 0.001)
    }

    @Test("Distances are measured in key pitches, not in points")
    func measuredInPitches() throws {
        // A whole key away from `s` is exactly where `a` sits: one pitch.
        let evidence = try #require(
            snapshot().touchEvidence(at: CGPoint(x: 20, y: 20), typed: key("s"))
        )
        #expect(abs(evidence.typedDistance - 1.0) < 0.001)
        #expect(evidence.first?.scalar == "a")
        #expect(abs((evidence.first?.distance ?? 0)) < 0.001)
    }

    @Test("A finger drifting towards a key names that key first")
    func drift() throws {
        // Landed on `s`, two fifths of a pitch towards `d`. Still nearer `s` — which
        // is the ordinary case, and why an alternate has to be weighed rather than
        // simply believed.
        let evidence = try #require(
            snapshot().touchEvidence(at: CGPoint(x: 90, y: 20), typed: key("s"))
        )
        #expect(evidence.first?.scalar == "d")
        #expect((evidence.first?.distance ?? 0) > evidence.typedDistance)
    }

    @Test("A key can be reported further away than its neighbour")
    func nearerNeighbour() throws {
        // The recovery policy commits the key a finger landed on even when it lifts
        // somewhere else, so the key that registered is not always the nearest one.
        // That gap is exactly the evidence a correction runs on.
        let evidence = try #require(
            snapshot().touchEvidence(at: CGPoint(x: 110, y: 20), typed: key("s"))
        )
        #expect(evidence.first?.scalar == "d")
        #expect((evidence.first?.distance ?? 1) < evidence.typedDistance)
    }

    @Test("A key nobody could have meant is not offered")
    func implausible() throws {
        let evidence = try #require(
            snapshot().touchEvidence(at: CGPoint(x: 20, y: 20), typed: key("a"))
        )
        #expect(evidence.first?.scalar == "s", "one pitch away is still plausible")
        #expect(evidence.second == nil, "two pitches away is not")
    }

    @Test("A key that types nothing has no evidence to give")
    func notACharacterKey() {
        let shift = KeySpec(id: "shift", label: "⇧", role: .shift)
        #expect(snapshot().touchEvidence(at: CGPoint(x: 70, y: 20), typed: shift) == nil)
    }

    @Test("A layout with no pitch reports nothing rather than dividing by zero")
    func noPitch() {
        let geometry = ResolvedKeyboard(
            size: CGSize(width: 150, height: 40),
            toolbarFrame: nil,
            rows: [[ResolvedKey(spec: key("a"), frame: CGRect(x: 0, y: 0, width: 40, height: 40))]]
        )
        let snapshot = KeyboardGeometrySnapshot(revision: 1, geometry: geometry)
        #expect(snapshot.touchEvidence(at: CGPoint(x: 20, y: 20), typed: key("a")) == nil)
    }
}
#endif
