#if canImport(UIKit)
import UIKit

/// The stock iPad pull-down: as the finger pulls, the letter fades and shrinks while the
/// hint slides down into its place and grows to the letter's size.
///
/// The travelling hint is a second label set at the letter's size and scaled down at rest,
/// so it ends crisp rather than as a small label blown up. The printed hint rides along the
/// same path and fades out, which carries its colour into the letter's.
extension KeyboardKeyContentView {
    func setSwipeProgress(_ progress: CGFloat, animated: Bool) {
        swipeProgress = min(1, max(0, progress))
        guard animated else {
            applySwipe(swipeProgress)
            return
        }
        UIView.animate(
            withDuration: 0.15,
            delay: 0,
            options: [.beginFromCurrentState, .allowUserInteraction, .curveEaseOut]
        ) {
            self.applySwipe(self.swipeProgress)
        }
    }

    func configureSwipeHint() {
        swipeHintLabel.textAlignment = .center
        swipeHintLabel.isUserInteractionEnabled = false
        swipeHintLabel.isAccessibilityElement = false
        swipeHintLabel.alpha = 0
        addSubview(swipeHintLabel)
    }

    /// Runs at the end of `layoutSubviews`, which lays the labels out untransformed.
    func layoutSwipeHint() {
        swipeHintLabel.text = secondaryLabel.text
        swipeHintLabel.font = label.font
        swipeHintLabel.textColor = label.textColor
        swipeHintLabel.frame = label.frame
        applySwipe(swipeProgress)
    }

    /// Sets every transform for `progress`; 0 is the key at rest.
    func applySwipe(_ progress: CGFloat) {
        guard progress > 0, !secondaryLabel.isHidden, label.font.pointSize > 0 else {
            label.transform = .identity
            label.alpha = 1
            secondaryLabel.transform = .identity
            secondaryLabel.alpha = 1
            swipeHintLabel.transform = .identity
            swipeHintLabel.alpha = 0
            return
        }
        let start = secondaryLabel.center
        let end = label.center
        let restScale = secondaryLabel.font.pointSize / label.font.pointSize
        let remaining = 1 - progress

        let letterScale = 1 - 0.3 * progress
        label.transform = CGAffineTransform(scaleX: letterScale, y: letterScale)
        label.alpha = remaining

        let hintScale = 1 - (1 - restScale) * remaining
        swipeHintLabel.transform = CGAffineTransform(
            translationX: (start.x - end.x) * remaining,
            y: (start.y - end.y) * remaining
        ).scaledBy(x: hintScale, y: hintScale)
        swipeHintLabel.alpha = progress

        let printedScale = hintScale / restScale
        secondaryLabel.transform = CGAffineTransform(
            translationX: (end.x - start.x) * progress,
            y: (end.y - start.y) * progress
        ).scaledBy(x: printedScale, y: printedScale)
        secondaryLabel.alpha = remaining
    }
}
#endif
