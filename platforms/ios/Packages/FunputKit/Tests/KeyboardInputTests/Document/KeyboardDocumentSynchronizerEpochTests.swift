@testable import KeyboardInput
import Foundation
import Testing

@MainActor
struct KeyboardDocumentSynchronizerEpochTests {
    @Test("A word boundary retains only the immediately previous echo epoch")
    func wordBoundaryRotatesEchoEpochs() {
        let identifier = UUID()
        var synchronizer = KeyboardDocumentSynchronizer()
        synchronizer.accept(KeyboardDocumentSnapshot(
            documentIdentifier: identifier,
            contextBeforeInput: "",
            hasSelection: false
        ))

        synchronizer.beginMutation()
        synchronizer.recordInsertion("mot")
        _ = synchronizer.finishMutation()
        synchronizer.beginMutation(closesEpoch: true)
        synchronizer.recordInsertion(" ")
        _ = synchronizer.finishMutation()
        synchronizer.beginMutation()
        synchronizer.recordInsertion("hai")
        _ = synchronizer.finishMutation()

        let consumedPreviousEpoch = synchronizer.consumeAuthoredTextChange(
            documentIdentifier: identifier,
            contextBeforeInput: "mot"
        )
        #expect(consumedPreviousEpoch)

        synchronizer.beginMutation(closesEpoch: true)
        synchronizer.recordInsertion(" ")
        _ = synchronizer.finishMutation()
        synchronizer.beginMutation()
        synchronizer.recordInsertion("ba")
        _ = synchronizer.finishMutation()

        let consumedExpiredEpoch = synchronizer.consumeAuthoredTextChange(
            documentIdentifier: identifier,
            contextBeforeInput: "mot"
        )
        #expect(!consumedExpiredEpoch)
    }

    @Test("External correction preserves delayed authored echo recognition")
    func externalCorrectionPreservesPendingEchoRecognition() {
        let identifier = UUID()
        var synchronizer = KeyboardDocumentSynchronizer()
        synchronizer.accept(KeyboardDocumentSnapshot(
            documentIdentifier: identifier,
            contextBeforeInput: "helo",
            hasSelection: false
        ))
        synchronizer.beginMutation()
        synchronizer.recordInsertion("x")
        _ = synchronizer.finishMutation()

        synchronizer.accept(
            KeyboardDocumentSnapshot(
                documentIdentifier: identifier,
                contextBeforeInput: "hello",
                hasSelection: false
            ),
            preservingAuthoredEchoes: true
        )

        #expect(synchronizer.snapshot?.contextBeforeInput == "hello")
        let consumed = synchronizer.consumeAuthoredTextChange(
            documentIdentifier: identifier,
            contextBeforeInput: "helox"
        )
        #expect(consumed)
        #expect(synchronizer.snapshot?.contextBeforeInput == "hello")
    }
}
