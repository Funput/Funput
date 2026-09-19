/// One word typo correction can reach, decoded for Swift.
///
/// `touchScore` is the touch evidence alone, in nats — always negative, closer to
/// zero is better. How likely the word is lives with the host's dictionary, which is
/// why `FunputComposer.chooseCorrection` takes the counts separately.
public struct FunputCorrectionCandidateText: Sendable, Equatable {
    public let text: String
    public let touchScore: Float
    public let edits: Int

    public init(text: String, touchScore: Float, edits: Int) {
        self.text = text
        self.touchScore = touchScore
        self.edits = edits
    }
}
