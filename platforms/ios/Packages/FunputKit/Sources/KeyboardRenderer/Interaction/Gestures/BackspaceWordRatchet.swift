#if canImport(UIKit)
import CoreGraphics

/// Recognizes a leftward rub on the Backspace key and meters it into whole words.
///
/// Claims as early as `activation` — inside the keycap — rather than at a swipe-sized
/// threshold. The contact has to be detached before the finger can drift onto a
/// neighbouring key, otherwise the touch pipeline commits that key on release.
struct BackspaceWordRatchet {
    private let activation: CGFloat
    private let dominance: CGFloat
    private let step: CGFloat
    private var emittedSteps = 0

    init(activation: CGFloat = 16, dominance: CGFloat = 1.25, step: CGFloat = 40) {
        precondition(activation > 0)
        precondition(step > 0)
        self.activation = activation
        self.dominance = dominance
        self.step = step
    }

    /// Whether this contact has become a word-delete rub.
    func shouldClaim(_ translation: CGPoint) -> Bool {
        translation.x <= -activation
            && abs(translation.x) > abs(translation.y) * dominance
    }

    /// How many further words the finger has asked to delete since the last call.
    ///
    /// Rightward travel never rewinds the anchor and never returns a count, which is how
    /// "swiping right does nothing" falls out without a special case.
    mutating func update(_ translation: CGPoint) -> Int {
        let steps = Int(max(0, -translation.x) / step)
        let delta = steps - emittedSteps
        guard delta > 0 else { return 0 }
        emittedSteps = steps
        return delta
    }

    /// True once the rub has deleted at least one word.
    var hasDeleted: Bool { emittedSteps > 0 }
}
#endif
