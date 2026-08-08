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

    @Test("An authored context stops acknowledging callbacks once no echo can be in flight")
    func staleEchoExpires() {
        var now = 10.0
        let identifier = UUID()
        var synchronizer = KeyboardDocumentSynchronizer()
        synchronizer.clock = { now }
        synchronizer.accept(KeyboardDocumentSnapshot(
            documentIdentifier: identifier,
            contextBeforeInput: "",
            hasSelection: false
        ))

        synchronizer.beginMutation()
        synchronizer.recordInsertion("ho")
        _ = synchronizer.finishMutation()

        now += KeyboardEchoEpoch.echoLifetime + 0.01
        // The document really is empty again — an external edit, not a late echo of the
        // empty context Funput saw before "ho".
        let consumed = synchronizer.consumeAuthoredTextChange(
            documentIdentifier: identifier,
            contextBeforeInput: ""
        )
        #expect(!consumed)
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
