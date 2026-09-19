import KeyboardLayout
import KeyboardRenderer
import UIKit

/// Owns Funput's optional height request while UIKit manages the keyboard host.
@MainActor
final class KeyboardHeightController {
    private enum Constants {
        static let constraintIdentifier = "Funput.Keyboard.preferredHeight"
        // UIKit's temporary host constraints must win during keyboard handoff.
        static let constraintPriority = UILayoutPriority(999)
    }

    private var heightConstraint: NSLayoutConstraint?
    private var baseHeight: CGFloat?

    func install(on view: UIView) {
        guard heightConstraint == nil else { return }

        let constraint = view.heightAnchor.constraint(
            equalToConstant: baseHeight ?? 0
        )
        constraint.identifier = Constants.constraintIdentifier
        constraint.priority = Constants.constraintPriority
        heightConstraint = constraint
    }

    @discardableResult
    func activate() -> Bool {
        guard let heightConstraint, baseHeight != nil else {
            assertionFailure("Install and update the keyboard height before activating it.")
            return false
        }
        guard !heightConstraint.isActive else { return false }
        heightConstraint.isActive = true
        return true
    }

    @discardableResult
    func update(
        for presentation: KeyboardPresentation,
        traits: UITraitCollection
    ) -> Bool {
        let height = KeyboardMetrics.recommendedHeight(
            for: presentation.layout,
            traits: traits,
            sizing: presentation.sizing
        )
        guard baseHeight != height else { return false }

        baseHeight = height
        applyHeight()
        return true
    }

    @discardableResult
    func deactivate() -> Bool {
        guard let heightConstraint, heightConstraint.isActive else {
            return false
        }

        heightConstraint.isActive = false
        return true
    }

    private func applyHeight() {
        guard let baseHeight else { return }
        UIView.performWithoutAnimation {
            heightConstraint?.constant = baseHeight
        }
    }
}
