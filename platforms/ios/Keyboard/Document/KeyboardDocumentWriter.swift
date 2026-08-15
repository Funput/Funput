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
            documentIdentifier: resolvedDocumentIdentifier,
            contextBeforeInput: proxy.documentContextBeforeInput,
            hasSelection: !(proxy.selectedText?.isEmpty ?? true)
        )
    }

    /// Reads `documentIdentifier` without trusting its nullability annotation.
    ///
    /// `UIInputViewController.h` declares it `NSUUID *` inside the header's
    /// assume-nonnull region — the property right above it is explicitly `nullable`,
    /// this one is not — so Swift imports a non-optional `UUID`. Hosts nevertheless
    /// hand back nil before the document is bound, and the bridge traps on nil instead
    /// of returning one. Going through the ObjC runtime keeps the nil.
    ///
    /// Seen in the field as `EXC_BREAKPOINT` in
    /// `UUID._unconditionallyBridgeFromObjectiveC` under `textDidChange`, which lands
    /// before `viewWillAppear`: the extension died mid-launch and the host was left
    /// drawing an empty keyboard.
    private var resolvedDocumentIdentifier: UUID? {
        let selector = #selector(getter: UITextDocumentProxy.documentIdentifier)
        guard let object = proxy as? NSObject, object.responds(to: selector) else {
            return nil
        }
        return object.perform(selector)?.takeUnretainedValue() as? UUID
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
