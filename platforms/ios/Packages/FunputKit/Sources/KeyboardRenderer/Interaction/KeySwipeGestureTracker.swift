#if canImport(UIKit)
import CoreGraphics
import KeyboardLayout

struct KeySwipeGestureTracker {
    private let threshold: CGFloat
    private let horizontalDominance: CGFloat
    private var didTrigger = false

    init(threshold: CGFloat = 32, horizontalDominance: CGFloat = 1.25) {
        precondition(threshold > 0)
        precondition(horizontalDominance > 0)
        self.threshold = threshold
        self.horizontalDominance = horizontalDominance
    }

    mutating func reset() {
        didTrigger = false
    }

    mutating func update(
        translation: CGPoint,
        action: KeySwipeAction?
    ) -> KeySwipeAction? {
        guard !didTrigger, let action else { return nil }
        let horizontal = abs(translation.x)
        let vertical = abs(translation.y)
        guard horizontal >= threshold,
              horizontal > vertical * horizontalDominance else { return nil }
        didTrigger = true
        return action
    }
}
#endif
