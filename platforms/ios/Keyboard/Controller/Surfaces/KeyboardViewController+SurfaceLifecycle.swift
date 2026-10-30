import KeyboardRenderer
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
        // Only a panel that survives the switch needs to look fresh for next time.
        let retained = KeyboardPanelRetention.retained(whileShowing: surface.panel)
        if displayedSurface == .emoji, surface != .emoji, retained.contains(.emoji) {
            emojiView?.reset()
        }
        displayedSurface = surface
        keyboardView.isHidden = surface != .funput
        emojiView?.isHidden = surface != .emoji
        kaomojiView?.isHidden = surface != .kaomoji
        clipboardPanelView?.isHidden = surface != .clipboard
        scheduleSupplementarySurfaceRelease()
    }

    /// Releases the panels ``KeyboardPanelRetention`` no longer keeps, on the next
    /// turn of the run loop rather than now: a switch is often asked for from inside
    /// a panel's own callback (a clipboard row's selection), and UIKit is still
    /// working on that panel's views when the callback returns. The rule is applied
    /// to whatever shows by then, so a quick switch back keeps its panel.
    private func scheduleSupplementarySurfaceRelease() {
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            releasePanels(keeping: KeyboardPanelRetention.retained(whileShowing: displayedSurface.panel))
        }
    }

    /// Under memory pressure even the way back is not worth keeping.
    func releaseHiddenSupplementarySurfaces() {
        releasePanels(keeping: Set([displayedSurface.panel].compactMap { $0 }))
    }

    private func releasePanels(keeping retained: Set<KeyboardPanel>) {
        if !retained.contains(.emoji) { releaseEmojiView() }
        if !retained.contains(.kaomoji) { releaseKaomojiView() }
        if !retained.contains(.clipboard) { releaseClipboardPanelView() }
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

enum KeyboardSurface: String {
    case funput
    case emoji
    case kaomoji
    case clipboard

    /// The panel this surface shows; nil for the letters.
    var panel: KeyboardPanel? {
        switch self {
        case .funput: nil
        case .emoji: .emoji
        case .kaomoji: .kaomoji
        case .clipboard: .clipboard
        }
    }
}
