import KeyboardInput
import os
import UIKit

@MainActor
final class KeyboardDocumentWriter: KeyboardDocumentWriting {
    let proxy: any UITextDocumentProxy

    init(proxy: any UITextDocumentProxy) {
        self.proxy = proxy
    }

    var snapshot: KeyboardDocumentSnapshot {
        KeyboardDocumentSnapshot(
            documentIdentifier: proxy.documentIdentifier,
            contextBeforeInput: proxy.documentContextBeforeInput,
            hasSelection: !(proxy.selectedText?.isEmpty ?? true)
        )
    }

    func apply(_ transaction: InputTransaction) {
        let deleteCount = transaction.mutations.reduce(into: 0) {
            if case let .deleteBackward(count) = $1 { $0 += max(0, count) }
        }
        let signpost = OSSignpostID(log: KeyboardDocumentWriterSignpost.log)
        os_signpost(
            .begin,
            log: KeyboardDocumentWriterSignpost.log,
            name: "DocumentTransaction",
            signpostID: signpost,
            "sequence=%{public}llu mutations=%{public}d deletes=%{public}d",
            transaction.sequence,
            transaction.mutations.count,
            deleteCount
        )
        for mutation in transaction.mutations {
            switch mutation {
            case let .deleteBackward(count):
                for _ in 0..<max(0, count) { proxy.deleteBackward() }
            case let .insert(text):
                guard !text.isEmpty else { continue }
                proxy.insertText(text)
            }
        }
        os_signpost(
            .end,
            log: KeyboardDocumentWriterSignpost.log,
            name: "DocumentTransaction",
            signpostID: signpost
        )
    }
}
