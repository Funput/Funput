import UIKit

extension KeyboardViewController {
    func attachSupplementarySurface(_ surface: UIView) {
        surface.translatesAutoresizingMaskIntoConstraints = false
        surface.isHidden = true
        view.addSubview(surface)
        NSLayoutConstraint.activate([
            surface.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            surface.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            surface.topAnchor.constraint(equalTo: view.topAnchor),
            surface.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
    }

    func switchSurface(to surface: KeyboardSurface) {
        displayedSurface = surface
        keyboardView.isHidden = surface != .funput
        emojiView?.isHidden = surface != .emoji
        kaomojiView?.isHidden = surface != .kaomoji
        clipboardPanelView?.isHidden = surface != .clipboard
    }

    func releaseHiddenSupplementarySurfaces() {
        if displayedSurface != .emoji {
            releaseEmojiView()
        }
        if displayedSurface != .kaomoji {
            releaseKaomojiView()
        }
        if displayedSurface != .clipboard {
            releaseClipboardPanelView()
        }
    }

    private func releaseEmojiView() {
        guard let emojiView else { return }
        emojiView.removeFromSuperview()
        self.emojiView = nil
        launchTrace.recordPanelReleased(.emoji)
    }

    private func releaseKaomojiView() {
        guard let kaomojiView else { return }
        kaomojiView.removeFromSuperview()
        self.kaomojiView = nil
        launchTrace.recordPanelReleased(.kaomoji)
    }

    private func releaseClipboardPanelView() {
        guard let clipboardPanelView else { return }
        clipboardPanelView.removeFromSuperview()
        self.clipboardPanelView = nil
        launchTrace.recordPanelReleased(.clipboard)
    }
}
