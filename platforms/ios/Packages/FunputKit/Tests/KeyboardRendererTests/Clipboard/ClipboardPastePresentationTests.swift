#if canImport(UIKit)
import Foundation
import FunputShared
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

@MainActor
struct ClipboardPastePresentationTests {
    @Test func pastingRefreshesTheOpenPanel() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let store = ClipboardStore(directory: directory)
        let panel = ClipboardKeyboardView()
        let recorder = ClipboardPasteRecorder(
            gateway: PresentationGateway(), allowsRecording: { true },
            save: { store.record($0) }, marks: store,
            onUpdate: {
                panel.apply(presentation: KeyboardPresentation(), entries: store.load().map {
                    KeyboardClipboardEntry(
                        id: $0.id, text: $0.text, capturedAt: $0.capturedAt, isPinned: $0.isPinned
                    )
                }, hasFullAccess: true)
            }
        )
        recorder.begin()
        recorder.didPaste("đã dán", changeCount: 1)
        #expect(store.load().map(\.text) == ["đã dán"])
        #expect(panel.entry(at: IndexPath(item: 0, section: 0))?.text == "đã dán")
    }

    /// The row sits above the history and never hides it: it is an invitation, not
    /// a replacement for what is already saved.
    @Test func pasteRowShowsAboveHistoryOnlyWhileOffered() {
        let panel = ClipboardKeyboardView()
        panel.frame = CGRect(x: 0, y: 0, width: 390, height: 260)
        let entry = KeyboardClipboardEntry(id: UUID(), text: "saved", capturedAt: Date(), isPinned: false)
        panel.apply(presentation: KeyboardPresentation(), entries: [entry], hasFullAccess: true)
        #expect(panel.pasteChip.isHidden)

        panel.updatePasteHint(.text)
        panel.layoutIfNeeded()
        #expect(!panel.pasteChip.isHidden)
        #expect(!panel.collectionView.isHidden)
        #expect(panel.collectionView.frame.minY == ClipboardKeyboardView.pasteRowHeight)

        panel.updatePasteHint(nil)
        panel.layoutIfNeeded()
        #expect(panel.pasteChip.isHidden)
        #expect(panel.collectionView.frame.minY == 0)
    }

    /// Nothing pasted could be kept without full access, so the panel invites none.
    @Test func pasteRowStaysHiddenWithoutFullAccess() {
        let panel = ClipboardKeyboardView()
        panel.updatePasteHint(.link)
        panel.apply(presentation: KeyboardPresentation(), entries: [], hasFullAccess: false)
        #expect(panel.pasteChip.isHidden)
        panel.apply(presentation: KeyboardPresentation(), entries: [], hasFullAccess: true)
        #expect(!panel.pasteChip.isHidden)
    }

    @Test func pasteRowHandsTheControlsPasteOn() {
        let panel = ClipboardKeyboardView()
        var pasted: KeyboardClipboardPaste?
        panel.onPaste = { pasted = $0 }
        panel.pasteChip.onPaste?(KeyboardClipboardPaste(text: "từ Mac", changeCount: 4))
        #expect(pasted?.text == "từ Mac")
        #expect(pasted?.changeCount == 4)
    }
}

@MainActor
private struct PresentationGateway: ClipboardGateway {
    func snapshot() -> ClipboardSnapshot {
        ClipboardSnapshot(
            changeCount: 1, hasStrings: true, hasURLs: false, hasPlainText: true
        )
    }
}
#endif
