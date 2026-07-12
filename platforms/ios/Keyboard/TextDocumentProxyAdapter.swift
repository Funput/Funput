import KeyboardInput
import UIKit

@MainActor
struct TextDocumentProxyAdapter: KeyboardDocument {
    let proxy: any UITextDocumentProxy

    var snapshot: KeyboardDocumentSnapshot {
        KeyboardDocumentSnapshot(
            documentIdentifier: proxy.documentIdentifier,
            contextBeforeInput: proxy.documentContextBeforeInput,
            hasSelection: !(proxy.selectedText?.isEmpty ?? true)
        )
    }

    func insertText(_ text: String) {
        proxy.insertText(text)
    }

    func deleteBackward() {
        proxy.deleteBackward()
    }
}
