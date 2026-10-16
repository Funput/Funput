/// One word typo correction can reach, decoded for Swift.
///
/// `touchScore` is minus the cost of the edits that reach the word, in nats, with the
/// touch evidence folded in where the keyboard reported it: never positive, closer to
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
