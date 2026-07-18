@testable import KeyboardInput
import Foundation
import Testing

@MainActor
struct KeyboardDocumentSynchronizerTests {
    @Test("Authored callbacks do not roll the shadow document backward")
    func delayedAuthoredCallbacks() {
        let identifier = UUID()
        var synchronizer = KeyboardDocumentSynchronizer()
        synchronizer.accept(KeyboardDocumentSnapshot(
            documentIdentifier: identifier,
            contextBeforeInput: "",
            hasSelection: false
        ))

        synchronizer.beginMutation()
        synchronizer.recordInsertion("a")
        _ = synchronizer.finishMutation()

        synchronizer.beginMutation()
        synchronizer.recordDeletion()
        synchronizer.recordInsertion("á")
        _ = synchronizer.finishMutation()

        let consumedIntermediate = synchronizer.consumeAuthoredTextChange(
            documentIdentifier: identifier,
            contextBeforeInput: "a"
        )
        #expect(consumedIntermediate)
        #expect(synchronizer.snapshot?.contextBeforeInput == "á")

        let consumedFinal = synchronizer.consumeAuthoredTextChange(
            documentIdentifier: identifier,
            contextBeforeInput: "á"
        )
        #expect(consumedFinal)
        #expect(synchronizer.snapshot?.contextBeforeInput == "á")
    }

    @Test("An echo older than the current burst is still authored, not an external edit")
    func echoOlderThanBurst() {
        let identifier = UUID()
        var synchronizer = KeyboardDocumentSynchronizer()
        synchronizer.accept(KeyboardDocumentSnapshot(
            documentIdentifier: identifier,
            contextBeforeInput: "",
            hasSelection: false
        ))

        for text in ["h", "o", "6"] {
            synchronizer.beginMutation()
            synchronizer.recordInsertion(text)
            _ = synchronizer.finishMutation()
        }

        // The host echoes the state from BEFORE the burst (textDidChange lags
        // fast typing). Treating it as external resets composition mid-word.
        let consumed = synchronizer.consumeAuthoredTextChange(
            documentIdentifier: identifier,
            contextBeforeInput: ""
        )
        #expect(consumed)
        #expect(synchronizer.snapshot?.contextBeforeInput == "ho6")
    }

    @Test("nil and empty contexts describe the same (empty) document")
    func nilAndEmptyContextsAreEquivalent() {
        let identifier = UUID()
        var synchronizer = KeyboardDocumentSynchronizer()
        synchronizer.accept(KeyboardDocumentSnapshot(
            documentIdentifier: identifier,
            contextBeforeInput: nil, // hosts report empty docs as nil or ""
            hasSelection: false
        ))

        synchronizer.beginMutation()
        synchronizer.recordInsertion("a")
        _ = synchronizer.finishMutation()

        // The first delayed callback can still report the pre-insertion nil
        // context. It must be retained in the authored ring as well.
        let consumedPreMutation = synchronizer.consumeAuthoredTextChange(
            documentIdentifier: identifier,
            contextBeforeInput: nil
        )
        #expect(consumedPreMutation)
        #expect(synchronizer.snapshot?.contextBeforeInput == "a")

        // The shadow after inserting into a nil-context document must be the
        // inserted text, so the host's echo of it is recognized as authored.
        #expect(synchronizer.snapshot?.contextBeforeInput == "a")
        let consumed = synchronizer.consumeAuthoredTextChange(
            documentIdentifier: identifier,
            contextBeforeInput: "a"
        )
        #expect(consumed)
    }

    @Test("Shadow context remains bounded while retaining the newest text")
    func boundedShadowContext() {
        var synchronizer = KeyboardDocumentSynchronizer()
        synchronizer.accept(KeyboardDocumentSnapshot(
            documentIdentifier: UUID(),
            contextBeforeInput: String(repeating: "a", count: 200),
            hasSelection: false
        ))

        synchronizer.beginMutation()
        synchronizer.recordInsertion("z")
        _ = synchronizer.finishMutation()

        let context = synchronizer.snapshot?.contextBeforeInput
        #expect(context?.count == 128)
        #expect(context?.hasSuffix("z") == true)
    }

    @Test("Rapid mutations keep pending acknowledgements bounded")
    func boundedPendingAcknowledgements() {
        let identifier = UUID()
        var synchronizer = KeyboardDocumentSynchronizer()
        synchronizer.accept(KeyboardDocumentSnapshot(
            documentIdentifier: identifier,
            contextBeforeInput: "",
            hasSelection: false
        ))

        for _ in 0..<10_000 {
            synchronizer.beginMutation()
            synchronizer.recordInsertion("a")
            _ = synchronizer.finishMutation()
        }

        #expect(synchronizer.pendingAuthoredContextCount > 0)
        #expect(synchronizer.pendingAuthoredContextCount <= 64)
        #expect(synchronizer.snapshot?.contextBeforeInput?.count == 128)

        let consumed = synchronizer.consumeAuthoredTextChange(
            documentIdentifier: identifier,
            contextBeforeInput: synchronizer.snapshot?.contextBeforeInput
        )
        #expect(consumed)
        // A current callback is not a monotonic watermark: UIKit can still
        // deliver an older callback that was already queued behind it.
        #expect(synchronizer.pendingAuthoredContextCount > 0)
        #expect(synchronizer.pendingAuthoredContextCount <= 64)
    }

}
