import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
        let shouldReactivate = heightController.deactivate()
        coordinator.animate(alongsideTransition: nil) { [weak self] _ in
            guard let self, shouldReactivate else { return }
            updatePreferredHeight()
            activatePreferredHeight()
        }
    }

    func updatePreferredHeight() {
        let changed = heightController.update(
            for: currentPresentation,
            traits: traitCollection
        )
        if changed { invalidateInputViewSize() }
    }

    func activatePreferredHeightForAppearance() {
        updatePreferredHeight()
        markPreferredHeightVisible()
        activatePreferredHeight()
        launchTrace.recordHeightReady()
    }

    /// Puts the height in place before the host ever lays the keyboard out.
    ///
    /// Keeping this constraint active while hidden also gives a reused UIKit host the
    /// correct fitting size before the next `viewWillAppear` layout pass.
    func activatePreferredHeightForBootstrap() {
        updatePreferredHeight()
        activatePreferredHeight()
    }

    func activatePreferredHeight() {
        guard heightController.activate() else { return }
        invalidateInputViewSize()
    }

    private func invalidateInputViewSize() {
        inputView?.invalidateIntrinsicContentSize()
        inputView?.setNeedsUpdateConstraints()
    }
}
