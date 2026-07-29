#if DEBUG
struct ShadowGuidedProgress: Equatable {
    enum Outcome: Equatable {
        case retry
        case advanced
        case completed
    }

    private(set) var currentIndex = 0
    private(set) var verifiedText = ""
    private(set) var mismatchIndex: Int?

    func step(in fixture: ShadowTypingFixture) -> ShadowTypingStep {
        fixture.steps[currentIndex]
    }

    mutating func reset() {
        currentIndex = 0
        verifiedText = ""
        mismatchIndex = nil
    }

    mutating func check(
        _ actual: String,
        fixture: ShadowTypingFixture
    ) -> Outcome {
        let current = step(in: fixture)
        guard actual == current.expected else {
            mismatchIndex = ShadowHarnessResult.mismatchIndex(
                actual,
                current.expected
            )
            return .retry
        }
        verifiedText += verifiedText.isEmpty
            ? current.expected : " " + current.expected
        mismatchIndex = nil
        guard currentIndex + 1 < fixture.steps.count else {
            return .completed
        }
        currentIndex += 1
        return .advanced
    }

    mutating func prepareRetry() {
        mismatchIndex = nil
    }
}
#endif
