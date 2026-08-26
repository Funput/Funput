import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    func updatePreferredHeight() {
        let changed = heightController.update(
            for: currentPresentation,
            traits: traitCollection
        )
        if changed { invalidateInputViewSize() }
    }

    func setPreferredHeightOverlayPad(_ pad: CGFloat) {
        guard heightController.setOverlayPad(pad) else { return }
        invalidateInputViewSize()
        view.layoutIfNeeded()
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
