import KeyboardInput
import UIKit

@MainActor
struct TextDocumentProxyAdapter: KeyboardDocument {
    let proxy: any UITextDocumentProxy

    var documentIdentifier: UUID { proxy.documentIdentifier }
    var contextBeforeInput: String? { proxy.documentContextBeforeInput }
    // `selectedText` is the most expensive proxy read; keep it off the hot path.
    var hasSelection: Bool { !(proxy.selectedText?.isEmpty ?? true) }

    func insertText(_ text: String) {
        proxy.insertText(text)
    }

    func deleteBackward() {
        proxy.deleteBackward()
    }
}
