import KeyboardInput
import UIKit

@MainActor
struct TextDocumentProxyAdapter: KeyboardDocument {
    let proxy: any UITextDocumentProxy

    func insertText(_ text: String) {
        proxy.insertText(text)
    }

    func deleteBackward() {
        proxy.deleteBackward()
    }
}
