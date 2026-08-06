#if canImport(UIKit)
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit
import Testing

/// The tracked geometry is 100x50 with a 12pt tolerance, so y = 90 is a release that lifted
/// clear of every key — what a fast two-thumb tap does when the finger rolls off the surface.
@MainActor
struct KeyboardTouchPipelineRecoveryTests {
    @Test("A release outside the tracked geometry still commits the key it landed on")
    func recoversReleaseOutside() {
        let fixture = makeTouchPipeline()
        fixture.consume(touchSample(1, .began, 0, .init(x: 10, y: 20)))
        let outside = fixture.consume(
            touchSample(1, .ended, 0.1, .init(x: 10, y: 90))
        )

        if case .resolved = outside {} else {
            Issue.record("Expected the release to resolve against the initial key")
        }
        #expect(fixture.emissions.keys == ["a"])
        #expect(fixture.pipeline.statistics.releasesOutside == 1)
        #expect(fixture.pipeline.statistics.recoveredReleasesOutside == 1)
    }

    @Test("A role outside the recovery policy still hands the release back")
    func policyOptsOut() {
        let fixture = makeTouchPipeline(
            policy: KeyboardTouchRecoveryPolicy(
                eligibleRoles: [.character],
                tapSlopRecoveringRoles: [.character],
                releaseOutsideRecoveringRoles: []
            )
        )
        fixture.consume(touchSample(1, .began, 0, .init(x: 10, y: 20)))
        let outside = fixture.consume(
            touchSample(1, .ended, 0.1, .init(x: 10, y: 90))
        )

        if case .fallback(_, .endedOutside) = outside {} else {
            Issue.record("Expected the release to fall back")
        }
        #expect(fixture.emissions.keys.isEmpty)
        #expect(fixture.pipeline.statistics.releasesOutside == 1)
        #expect(fixture.pipeline.statistics.recoveredReleasesOutside == 0)
    }
}
#endif
