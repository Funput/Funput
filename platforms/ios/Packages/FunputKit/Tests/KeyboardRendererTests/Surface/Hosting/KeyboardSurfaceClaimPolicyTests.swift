#if canImport(UIKit)
import KeyboardRenderer
import Testing

@Suite("Keyboard surface claim policy")
struct KeyboardSurfaceClaimPolicyTests {
    typealias Moment = KeyboardSurfaceClaimPolicy.Moment
    typealias Decision = KeyboardSurfaceClaimPolicy.Decision

    @Test("The holder keeps the surface, whatever the moment", arguments: [Moment.loading, .appearing], [true, false])
    func holderKeeps(moment: Moment, isAttached: Bool) {
        let decision = KeyboardSurfaceClaimPolicy.decide(
            at: moment, claimantHolds: true, hasHolder: true, isAttached: isAttached
        )
        #expect(decision == .keep)
    }

    @Test("A controller that is appearing takes the surface from anyone", arguments: [true, false], [true, false])
    func appearingTakes(hasHolder: Bool, isAttached: Bool) {
        let decision = KeyboardSurfaceClaimPolicy.decide(
            at: .appearing, claimantHolds: false, hasHolder: hasHolder, isAttached: isAttached
        )
        #expect(decision == .take)
    }

    /// A loading controller may never be shown. Taking a surface that is on screen
    /// would blank the keyboard the user is looking at.
    @Test("A controller that is loading leaves a surface that is being shown")
    func loadingWaitsForAShownSurface() {
        let decision = KeyboardSurfaceClaimPolicy.decide(
            at: .loading, claimantHolds: false, hasHolder: true, isAttached: true
        )
        #expect(decision == .wait)
    }

    @Test("A controller that is loading takes a surface nobody is showing", arguments: [
        (false, false), (true, false), (false, true),
    ])
    func loadingTakesAFreeSurface(hasHolder: Bool, isAttached: Bool) {
        let decision = KeyboardSurfaceClaimPolicy.decide(
            at: .loading, claimantHolds: false, hasHolder: hasHolder, isAttached: isAttached
        )
        #expect(decision == .take)
    }
}
#endif
