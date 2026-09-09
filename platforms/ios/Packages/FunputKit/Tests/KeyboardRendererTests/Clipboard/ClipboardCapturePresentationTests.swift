#if canImport(UIKit)
import Foundation
import FunputShared
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

@MainActor
struct ClipboardCapturePresentationTests {
    @Test func savingUpdatesOpenPanelAndKeepsPasteVisible() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let store = ClipboardStore(directory: directory)
        let gateway = PresentationGateway()
        let panel = ClipboardKeyboardView()
        let toolbar = KeyboardToolbarView()
        toolbar.updateSuggestions([KeyboardSuggestionCandidate(text: "gõ", generation: 1)])
        let controller = ClipboardCaptureController(
            gateway: gateway, allowsCapture: { true }, save: { store.capture($0) },
            marks: store,
            onUpdate: {
                panel.apply(presentation: KeyboardPresentation(), entries: store.load().map {
                    KeyboardClipboardEntry(
                        id: $0.id, text: $0.text, capturedAt: $0.capturedAt, isPinned: $0.isPinned
                    )
                }, hasFullAccess: true)
            }
        )
        controller.begin()
        controller.synchronize()
        let context = ClipboardOfferPolicy.Context(
            editorMode: .text, hasToolbar: true, hasFullAccess: true
        )
        let offer = ClipboardOfferPolicy.offer(
            snapshot: gateway.snapshot(), lastPastedChangeCount: controller.lastPastedChangeCount,
            context: context
        )
        toolbar.updateClipboardHint(offer == nil ? nil : .text)
        #expect(store.load().map(\.text) == ["copied"])
        #expect(panel.entry(at: IndexPath(item: 0, section: 0))?.text == "copied")
        #expect(!toolbar.clipboardChip.isHidden)
        #expect(toolbar.suggestionBar.isHidden)
    }

    @Test func retryBannerDoesNotHideExistingHistory() {
        let panel = ClipboardKeyboardView()
        let entry = KeyboardClipboardEntry(id: UUID(), text: "saved", capturedAt: Date(), isPinned: false)
        panel.apply(
            presentation: KeyboardPresentation(), entries: [entry], hasFullAccess: true, needsRetry: true
        )
        #expect(!panel.retryBanner.isHidden)
        #expect(!panel.collectionView.isHidden)
        #expect(panel.retryBanner.button.isEnabled)
        panel.apply(presentation: KeyboardPresentation(), entries: [entry], hasFullAccess: true)
        #expect(panel.retryBanner.isHidden)
    }
}

@MainActor
private struct PresentationGateway: ClipboardGateway {
    func snapshot() -> ClipboardSnapshot {
        ClipboardSnapshot(changeCount: 1, hasStrings: true, hasURLs: false)
    }
    func readText() -> String? { "copied" }
}
#endif
