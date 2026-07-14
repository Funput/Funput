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

        #expect(synchronizer.pendingAuthoredContextCount == 256)
        #expect(synchronizer.snapshot?.contextBeforeInput?.count == 128)

        let consumed = synchronizer.consumeAuthoredTextChange(
            documentIdentifier: identifier,
            contextBeforeInput: synchronizer.snapshot?.contextBeforeInput
        )
        #expect(consumed)
        #expect(synchronizer.pendingAuthoredContextCount == 0)
    }
}
