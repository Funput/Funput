#if canImport(UIKit)
import KeyboardLayout
import UIKit

extension KeyboardKeyControl {
    /// What a key takes from its spec outside a presentation pass. `init` and
    /// `adopt(_:)` both land here, so a reused control cannot keep a trait of the key
    /// it used to be: a keypad's blank cell must be able to turn back into a letter.
    func applySpec() {
        let isPlaceholder = spec.role == .placeholder
        accessibilityLabel = spec.accessibilityLabel
        isAccessibilityElement = !isPlaceholder
        isUserInteractionEnabled = !isPlaceholder
        isHidden = isPlaceholder
        updateSwipeRecognizer()
    }

    func configureInteraction() {
        interactionControl.isAccessibilityElement = false
        contentView.isUserInteractionEnabled = false
        interactionControl.isMultipleTouchEnabled = false
        // The iOS 27 remote keyboard host excludes fully transparent pixels from its hit region.
        // A nonzero alpha keeps the touch grid live without a perceptible visual change.
        interactionControl.backgroundColor = UIColor.white.withAlphaComponent(0.001)
        interactionControl.addAction(UIAction { [weak self] _ in
            self?.handleTouch(.pressed)
        }, for: .touchDown)
        interactionControl.addAction(UIAction { [weak self] _ in
            self?.handleTouch(.released)
        }, for: .touchUpInside)
        interactionControl.addAction(UIAction { [weak self] _ in
            self?.handleTouch(.cancelled)
        }, for: [.touchCancel, .touchUpOutside])
    }

    func emit(_ phase: KeyboardKeyEvent.Phase) {
        onEvent?(KeyboardKeyEvent(key: spec, phase: phase))
    }

    /// Only a key with a sideways action needs the recognizer, so it is made the
    /// first time one is adopted and switched off for every key without one.
    private func updateSwipeRecognizer() {
        let swipes = spec.horizontalSwipeAction != nil
        if swipes, swipeRecognizer == nil {
            let recognizer = UIPanGestureRecognizer(target: self, action: #selector(handlePan(_:)))
            recognizer.cancelsTouchesInView = false
            recognizer.maximumNumberOfTouches = 1
            interactionControl.addGestureRecognizer(recognizer)
            swipeRecognizer = recognizer
        }
        swipeRecognizer?.isEnabled = swipes
    }

    @objc private func handlePan(_ recognizer: UIPanGestureRecognizer) {
        if recognizer.state == .began {
            swipeTracker.reset()
        }
        guard recognizer.state == .changed || recognizer.state == .ended,
              let action = swipeTracker.update(
                  translation: recognizer.translation(in: interactionControl),
                  action: spec.horizontalSwipeAction
              ) else { return }
        emit(.swiped(action))
    }

    private func handleTouch(_ phase: KeyboardKeyEvent.Phase) {
        surface.setPressed(phase == .pressed, theme: theme, animated: true)
        emit(phase)
    }
}
#endif
