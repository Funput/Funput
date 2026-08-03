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
    func refreshClipboardOffer(
        retriesRemaining: Int = KeyboardViewController.clipboardRetryDelays.count
    ) {
        let context = ClipboardOfferPolicy.Context(
            editorMode: inputCoordinator.state.editorMode,
            hasToolbar: keyboardView.presentation.layout.toolbar != nil,
            hasFullAccess: hasFullAccess,
            isEnabled: configuration.clipboardEnabled
        )
        // Checked before anything else, so a refused pasteboard read can never leave
        // a paste invitation sitting in a password field.
        guard ClipboardOfferPolicy.allowsOffer(context: context) else {
            keyboardView.updateClipboardHint(nil)
            return
        }

        let snapshot = ClipboardSnapshot(.general)
        // iOS declines reads while the host app settles — Chrome is a frequent
        // offender — and answers as if the pasteboard were empty. Leave whatever is
        // on screen alone and ask again rather than believing it.
        guard !snapshot.isIndeterminate else {
            scheduleClipboardRetry(retriesRemaining: retriesRemaining)
            return
        }

        let offer = ClipboardOfferPolicy.offer(
            snapshot: snapshot,
            lastCapturedChangeCount: clipboardStore.lastCapturedChangeCount(),
            context: context
        )
        keyboardView.updateClipboardHint(offer.map(Self.hint))
    }

    /// Bounded and widening: two more attempts, then leave it until the next thing
    /// that would refresh the offer anyway.
    static let clipboardRetryDelays: [Duration] = [.milliseconds(350), .seconds(1)]

    private func scheduleClipboardRetry(retriesRemaining: Int) {
        let index = Self.clipboardRetryDelays.count - retriesRemaining
        guard Self.clipboardRetryDelays.indices.contains(index) else { return }
        let delay = Self.clipboardRetryDelays[index]
        Task { @MainActor [weak self] in
            try? await Task.sleep(for: delay)
            self?.refreshClipboardOffer(retriesRemaining: retriesRemaining - 1)
        }
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
        // The panel can already be open when the switch is turned off, so the write
        // is guarded here and not only where the chip is offered.
        if configuration.clipboardEnabled {
            clipboardStore.record(
                ClipboardItem(text: text, sourceChangeCount: UIPasteboard.general.changeCount)
            )
        }
        refreshClipboardOffer()
        applyPostCommitEffects(effects)
    }
}
