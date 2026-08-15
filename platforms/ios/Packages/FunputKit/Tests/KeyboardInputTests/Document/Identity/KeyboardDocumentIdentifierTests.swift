#if os(iOS) && canImport(FunputCore)
import Foundation
import KeyboardInput
import KeyboardLayout
import Testing

/// A host can leave `documentIdentifier` nil, whatever UIKit's nullability annotation
/// claims — reading it unguarded killed the extension mid-launch and left the user
/// looking at an empty keyboard. These cover what the rest of the pipeline then does
/// with an identifier-less document.
@MainActor
struct KeyboardDocumentIdentifierTests {
    @Test("An absent identifier is not a new document on every keystroke")
    func absentIdentifierIsStable() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        document.documentIdentifier = nil
        coordinator.synchronizeDocument(document, event: .activated)

        // A composition only survives if the document is recognised as the same one
        // between keys; a changed identifier resets the composer.
        type("chaof", with: coordinator, into: document)

        #expect(document.text == "chào")
    }

    // Deliberately no test for "the identifier appearing resets composition": the
    // observable outcome there is dominated by the adopt/retone path, which has its
    // own coverage, so a test here would pin an incidental result rather than this
    // behaviour. `KeyboardDocumentSynchronizerTests` covers the reset rule directly.

    @Test("Snapshots without identifiers compare equal")
    func snapshotEquality() {
        let absent = KeyboardDocumentSnapshot(
            documentIdentifier: nil,
            contextBeforeInput: "x",
            hasSelection: false
        )
        let same = KeyboardDocumentSnapshot(
            documentIdentifier: nil,
            contextBeforeInput: "x",
            hasSelection: false
        )
        let named = KeyboardDocumentSnapshot(
            documentIdentifier: UUID(),
            contextBeforeInput: "x",
            hasSelection: false
        )

        #expect(absent == same)
        #expect(absent != named)
    }
}
#endif
