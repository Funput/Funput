import FunputShared
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    @discardableResult
    func ensureClipboardPanelView() -> ClipboardKeyboardView {
        if let clipboardPanelView {
            return clipboardPanelView
        }
        let clipboardPanelView = KeyboardLaunchTrace.makePanel(.clipboard, ClipboardKeyboardView())
        // Same as pasting from history: the user came here to fetch one thing and
        // then keep typing.
        clipboardPanelView.onPaste = { [weak self] paste in
            self?.pasteFromClipboard(paste)
            self?.showFunput()
        }
        clipboardPanelView.onSelect = { [weak self] entry in
            self?.pasteFromHistory(entry)
        }
        clipboardPanelView.onTogglePin = { [weak self] entry in
            self?.clipboardStore.setPinned(!entry.isPinned, id: entry.id)
            self?.refreshClipboardPanel()
        }
        clipboardPanelView.onRemove = { [weak self] entry in
            self?.clipboardStore.remove(id: entry.id)
            self?.refreshClipboardPanel()
        }
        clipboardPanelView.onClearAll = { [weak self] in
            self?.clearClipboardHistory()
        }
        clipboardPanelView.onDelete = { [weak self] in
            self?.deleteFromClipboardPanel()
        }
        clipboardPanelView.onReturn = { [weak self] in
            self?.showFunput()
        }
        clipboardPanelView.backgroundImage = cachedBackgroundImage
        self.clipboardPanelView = clipboardPanelView
        attachSupplementarySurface(clipboardPanelView)
        return clipboardPanelView
    }

    func showClipboardPanel() {
        guard currentPresentation.layout.toolbar != nil else { return }
        launchTrace.recordPanelFirstOpen(.clipboard)
        ensureClipboardPanelView()
        inputCoordinator.prepareForLiteralInput()
        clearPersonalSuggestions()
        refreshClipboardPanel()
        if isClipboardMonitoring {
            // The panel's paste row did not exist when the offer was last worked out.
            refreshClipboardOffer()
        } else {
            // Opened within the start delay: the user asked for the clipboard, so
            // there is nothing left to wait for. Starting refreshes the offer.
            cancelClipboardMonitoringStart()
            startClipboardMonitoring()
        }
        switchSurface(to: .clipboard)
    }

    func refreshClipboardPanel() {
        clipboardPanelView?.apply(
            presentation: currentPresentation,
            entries: clipboardStore.load().map(Self.entry),
            hasFullAccess: hasFullAccess
        )
    }

    private static func entry(_ item: ClipboardItem) -> KeyboardClipboardEntry {
        KeyboardClipboardEntry(
            id: item.id,
            text: item.text,
            capturedAt: item.capturedAt,
            isPinned: item.isPinned
        )
    }

    /// Pasting from history returns to the keyboard: the user came here to fetch one
    /// thing and then keep typing, unlike emoji or kaomoji where several in a row is
    /// the normal case.
    private func pasteFromHistory(_ entry: KeyboardClipboardEntry) {
        let effects = inputCoordinator.insertLiteral(entry.text, writer: makeDocumentWriter())
        showFunput()
        applyPostCommitEffects(effects)
    }

    /// Nothing re-imports what is still on the pasteboard: the history only grows
    /// when the user pastes, so a wipe stays a wipe.
    private func clearClipboardHistory() {
        clipboardStore.clear()
        refreshClipboardPanel()
        refreshClipboardOffer()
    }

    private func deleteFromClipboardPanel() {
        let effects = inputCoordinator.deleteBackward(writer: makeDocumentWriter())
        applyPostCommitEffects(effects)
    }
}
