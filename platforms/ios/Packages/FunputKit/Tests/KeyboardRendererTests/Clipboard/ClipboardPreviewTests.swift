#if canImport(UIKit)
@testable import KeyboardRenderer
import Testing
import UIKit

@MainActor
@Suite("Clipboard preview card")
struct ClipboardPreviewTests {
    /// A one-line row cannot show a long clip, so the long-press card carries the
    /// whole text — not the truncated string the row displays.
    @Test("The long-press card holds the clip in full and stays within its bounds")
    func previewShowsFullText() {
        let long = String(repeating: "dài ", count: 200)
        let controller = ClipboardPreviewController(text: long, width: 340)
        controller.loadViewIfNeeded()
        #expect(controller.textView.text == long)
        #expect(controller.preferredContentSize.width == 340)
        #expect(controller.preferredContentSize.height <= ClipboardPreviewController.maximumHeight)
    }

    /// A context-menu preview never receives gestures, so scrolling could only ever
    /// look broken. Overflow is truncated with an ellipsis instead.
    @Test("The card never pretends to scroll")
    func previewDoesNotScroll() {
        let controller = ClipboardPreviewController(
            text: String(repeating: "dài ", count: 200), width: 340
        )
        controller.loadViewIfNeeded()
        #expect(!controller.textView.isScrollEnabled)
        #expect(controller.textView.textContainer.lineBreakMode == .byTruncatingTail)
        #expect(controller.textView.textContainer.maximumNumberOfLines > 1)
    }

    @Test("A short clip gets a card that stops at its own height")
    func previewShrinksToShortText() {
        let controller = ClipboardPreviewController(text: "ngắn", width: 340)
        controller.loadViewIfNeeded()
        #expect(controller.preferredContentSize.height < ClipboardPreviewController.maximumHeight)
    }

    @Test("A narrow panel still gets a readable card")
    func previewHasMinimumWidth() {
        let controller = ClipboardPreviewController(text: "ngắn", width: 100)
        controller.loadViewIfNeeded()
        #expect(controller.preferredContentSize.width == ClipboardPreviewController.minimumWidth)
    }
}
#endif
