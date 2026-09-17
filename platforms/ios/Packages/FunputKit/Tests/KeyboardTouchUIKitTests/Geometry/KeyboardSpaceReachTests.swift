#if canImport(UIKit)
import CoreGraphics
import KeyboardLayout
import KeyboardTouchUIKit
import Testing

/// A thumb typing without looking aims at the spacebar and lands on `n`: the row gap is split
/// at its midpoint, so the spacebar has 3.5pt of slack above it while the letters above have
/// their own keycap plus the same 3.5pt. `KeyboardSpaceReach` hands the whole gap and a few
/// points beyond it to the spacebar.
///
/// These fix both directions of the trade: the strip that used to answer `n` now answers
/// space, and `n` itself has to stay reachable everywhere else.
@MainActor
struct KeyboardSpaceReachTests {
    @Test("A tap that falls short of the spacebar still sends a space")
    func tapsAboveTheSpacebarStillSendSpace() {
        let (snapshot, geometry) = makeGeometry()
        let space = geometry.frame("space")
        let n = geometry.frame("character-n")

        // In the gap, where the midpoint split used to hand the tap to `n`.
        #expect(snapshot.touchHit(at: CGPoint(x: n.midX, y: space.minY - 2))?.key.id == "space")
        // Past the gap, on the bottom edge of the keycap above.
        #expect(snapshot.touchHit(at: CGPoint(x: n.midX, y: n.maxY - 1))?.key.id == "space")
    }

    @Test("The letters above keep every part of their keycap but the bottom edge")
    func lettersAboveKeepTheirKeycap() {
        let (snapshot, geometry) = makeGeometry()
        let n = geometry.frame("character-n")
        let justAboveTheReach = n.maxY - KeyboardSpaceReach.overlap - 0.5

        #expect(snapshot.touchHit(at: CGPoint(x: n.midX, y: n.midY))?.key.id == "character-n")
        #expect(
            snapshot.touchHit(at: CGPoint(x: n.midX, y: justAboveTheReach))?.key.id
                == "character-n"
        )
    }

    /// The claim spans the spacebar's own width and no further, so on a phone-width layout the
    /// keys past either end of it are untouched — `m` sits clear of its right end.
    @Test("Keys clear of the spacebar's width keep their full keycap")
    func keysBesideTheSpacebarAreUntouched() {
        let (snapshot, geometry) = makeGeometry()
        let space = geometry.frame("space")
        let m = geometry.frame("character-m")

        #expect(m.midX > space.maxX)
        #expect(snapshot.touchHit(at: CGPoint(x: m.midX, y: m.maxY - 0.5))?.key.id == "character-m")
    }

    /// Key height is a user setting. At the smallest scale a fixed 4pt would take a quarter of
    /// the row above instead of a tenth, so the fraction wins there.
    @Test("The reach never takes more than a tenth of the row above")
    func reachIsClampedToTheRowAbove() {
        let space = ResolvedKey(
            spec: KeySpec(id: "space", label: "", role: .space),
            frame: CGRect(x: 0, y: 100, width: 200, height: 40)
        )

        let roomy = KeyboardSpaceReach.claim(for: space, neighbourBottom: 93, neighbourHeight: 40)
        let cramped = KeyboardSpaceReach.claim(for: space, neighbourBottom: 93, neighbourHeight: 20)

        #expect(roomy?.minY == 89)
        #expect(roomy?.maxY == 100)
        #expect(cramped?.minY == 91)
        #expect(KeyboardSpaceReach.overlap == 4)
        #expect(KeyboardSpaceReach.maximumNeighbourFraction == 0.1)
    }

    @Test("No other key reaches into the row above", arguments: [
        KeyRole.character, .punctuation, .backspace, .enter, .shift, .symbols,
    ])
    func onlyTheSpacebarReaches(role: KeyRole) {
        let key = ResolvedKey(
            spec: KeySpec(id: "other", label: "", role: role),
            frame: CGRect(x: 0, y: 100, width: 40, height: 40)
        )

        #expect(KeyboardSpaceReach.claim(for: key, neighbourBottom: 93, neighbourHeight: 40) == nil)
    }

    private func makeGeometry() -> (KeyboardGeometrySnapshot, ResolvedKeyboard) {
        let geometry = KeyboardGeometry.resolve(
            layout: KeyboardLayoutResolver.resolve(inputMethod: .telex, mode: .letters),
            size: CGSize(width: 390, height: 304),
            sizing: KeyboardSizingProfile()
        )
        return (KeyboardGeometrySnapshot(revision: 1, geometry: geometry), geometry)
    }
}

private extension ResolvedKeyboard {
    func frame(_ id: String) -> CGRect {
        keys.first { $0.spec.id == id }!.frame
    }
}
#endif
