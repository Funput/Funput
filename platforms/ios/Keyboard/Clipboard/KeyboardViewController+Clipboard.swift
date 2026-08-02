import FunputShared
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    func installClipboard() {
        keyboardView.onClipboardPaste = { [weak self] text in
            self?.pasteFromClipboard(text)
        }
    }

    /// Re-evaluates whether the toolbar may invite a paste.
    ///
    /// Reads pasteboard **metadata only** — `changeCount`, `hasStrings`, `hasURLs` —
    /// which raises no paste alert. The contents are never read here; they arrive
    /// only through the user's tap on the chip's `UIPasteControl`.
    ///
    /// Deliberately not called from `updateInputPresentation()`: that runs on every
    /// keystroke that moves the shift state, and the typing hot path stays free of
    /// pasteboard work.
    func refreshClipboardOffer() {
        let offer = ClipboardOfferPolicy.offer(
            snapshot: ClipboardSnapshot(.general),
            lastCapturedChangeCount: clipboardStore.lastCapturedChangeCount(),
            context: ClipboardOfferPolicy.Context(
                editorMode: inputCoordinator.state.editorMode,
                hasToolbar: keyboardView.presentation.layout.toolbar != nil,
                hasFullAccess: hasFullAccess
            )
        )
        keyboardView.updateClipboardHint(offer.map(Self.hint))
    }

    private static func hint(for offer: ClipboardOffer) -> KeyboardClipboardHint {
        switch offer.kind {
        case .text: .text
        case .link: .link
        }
    }

    private func pasteFromClipboard(_ text: String) {
        guard !text.isEmpty else { return }
        let effects = inputCoordinator.insertLiteral(text, writer: makeDocumentWriter())
        // Stamped with the live `changeCount` rather than the one the offer was built
        // from: the text just handed over is whatever is on the pasteboard now.
        clipboardStore.record(
            ClipboardItem(text: text, sourceChangeCount: UIPasteboard.general.changeCount)
        )
        refreshClipboardOffer()
        applyPostCommitEffects(effects)
    }
}
