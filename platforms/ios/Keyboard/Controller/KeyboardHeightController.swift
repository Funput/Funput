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
    private var requestedHeight: CGFloat?

    func install(on view: UIView) {
        guard heightConstraint == nil else { return }

        let constraint = view.heightAnchor.constraint(
            equalToConstant: requestedHeight ?? 0
        )
        constraint.identifier = Constants.constraintIdentifier
        constraint.priority = Constants.constraintPriority
        heightConstraint = constraint
    }

    func activate(
        for presentation: KeyboardPresentation,
        traits: UITraitCollection
    ) {
        update(for: presentation, traits: traits)

        guard let heightConstraint else {
            assertionFailure("Install the keyboard height controller before activating it.")
            return
        }
        guard !heightConstraint.isActive else { return }
        heightConstraint.isActive = true
    }

    func update(
        for presentation: KeyboardPresentation,
        traits: UITraitCollection
    ) {
        let height = KeyboardMetrics.recommendedHeight(
            for: presentation.layout,
            traits: traits,
            scale: presentation.sizing.heightScale
        )
        guard requestedHeight != height else { return }

        requestedHeight = height
        heightConstraint?.constant = height
    }

    @discardableResult
    func deactivate() -> Bool {
        guard let heightConstraint, heightConstraint.isActive else {
            return false
        }

        heightConstraint.isActive = false
        return true
    }
}
