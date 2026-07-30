@testable import KeyboardInput
import Foundation
import Testing

@MainActor
struct KeyboardDocumentSynchronizerEchoOrderingTests {
    @Test("A current callback must not make a later stale authored echo external")
    func currentCallbackFollowedByStaleEcho() {
        let identifier = UUID()
        var synchronizer = KeyboardDocumentSynchronizer()
        synchronizer.accept(KeyboardDocumentSnapshot(
            documentIdentifier: identifier,
            contextBeforeInput: "",
            hasSelection: false
        ))

        for text in ["h", "o"] {
            synchronizer.beginMutation()
            synchronizer.recordInsertion(text)
            _ = synchronizer.finishMutation()
        }

        // UIKit callbacks are not a monotonic acknowledgement stream.
        let consumedCurrent = synchronizer.consumeAuthoredTextChange(
            documentIdentifier: identifier,
            contextBeforeInput: "ho"
        )
        let consumedStale = synchronizer.consumeAuthoredTextChange(
            documentIdentifier: identifier,
            contextBeforeInput: "h"
        )
        #expect(consumedCurrent)
        #expect(consumedStale)
        #expect(synchronizer.snapshot?.contextBeforeInput == "ho")
    }

    @Test("A shorter suffix alone is not proof that a callback was authored")
    func shorterSuffixIsExternal() {
        let identifier = UUID()
        var synchronizer = KeyboardDocumentSynchronizer()
        synchronizer.accept(KeyboardDocumentSnapshot(
            documentIdentifier: identifier,
            contextBeforeInput: "foobar",
            hasSelection: false
        ))

        let consumed = synchronizer.consumeAuthoredTextChange(
            documentIdentifier: identifier,
            contextBeforeInput: "bar"
        )
        #expect(!consumed)
    }
}
