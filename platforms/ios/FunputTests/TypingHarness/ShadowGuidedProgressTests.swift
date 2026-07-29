#if DEBUG
import Testing
@testable import Funput

@MainActor
struct ShadowGuidedProgressTests {
    @Test("Wrong phrase stays in place and retry clears only the error")
    func retry() {
        let fixture = ShadowTypingFixture.all[0]
        var progress = ShadowGuidedProgress()

        #expect(progress.check("sai", fixture: fixture) == .retry)
        #expect(progress.currentIndex == 0)
        #expect(progress.mismatchIndex == 0)

        progress.prepareRetry()
        #expect(progress.currentIndex == 0)
        #expect(progress.mismatchIndex == nil)
    }

    @Test("Correct phrases advance and reconstruct the full paragraph")
    func completes() {
        let fixture = ShadowTypingFixture.all[1]
        var progress = ShadowGuidedProgress()

        for (index, step) in fixture.steps.enumerated() {
            let outcome = progress.check(step.expected, fixture: fixture)
            if index + 1 == fixture.steps.count {
                #expect(outcome == .completed)
            } else {
                #expect(outcome == .advanced)
                #expect(progress.currentIndex == index + 1)
            }
        }
        #expect(progress.verifiedText == ShadowTypingFixture.expected)
    }
}
#endif
