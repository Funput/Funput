#if os(iOS) && canImport(FunputCore)
import Foundation
import KeyboardInput
import Testing

@MainActor
struct KeyboardDocumentSynchronizationTests {
    @Test("Coordinator-authored mutations remain synchronized")
    func ownMutations() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()

        type("a", with: coordinator, into: document)
        coordinator.synchronizeDocument(document, event: .textChanged)
        type("s", with: coordinator, into: document)

        #expect(document.text == "á")
    }

    @Test("External text and caret context changes clear composition")
    func externalContextChange() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        type("a", with: coordinator, into: document)

        document.replaceTextExternally(with: "🙂a")
        coordinator.synchronizeDocument(document, event: .selectionChanged)
        type("s", with: coordinator, into: document)

        #expect(document.text == "🙂as")
    }

    @Test("A new document resets composition even with identical context")
    func documentChange() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        type("a", with: coordinator, into: document)

        document.documentIdentifier = UUID()
        coordinator.synchronizeDocument(document, event: .textChanged)
        type("s", with: coordinator, into: document)

        #expect(document.text == "as")
    }

    @Test("An active selection clears composition")
    func selection() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        type("a", with: coordinator, into: document)

        document.hasSelection = true
        coordinator.synchronizeDocument(document, event: .selectionChanged)
        document.hasSelection = false
        type("s", with: coordinator, into: document)

        #expect(document.text == "as")
    }

    @Test("Unavailable context does not disable Vietnamese composition")
    func unavailableContext() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        document.exposesContext = false

        type("as", with: coordinator, into: document)

        #expect(document.text == "á")
    }

    @Test("A delayed document proxy does not break rapid composition")
    func delayedProxyContext() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        document.delaysContextUpdates = true

        type("as", with: coordinator, into: document)

        #expect(document.text == "á")
        document.publishContext()
        coordinator.synchronizeDocument(document, event: .textChanged)
        type("n", with: coordinator, into: document)
        #expect(document.text == "án")
    }

    // Regression for the "Funput spits out stale raw keys" report. A chat app's own Send
    // button clears the field, and the resulting callback reports an empty context — which
    // is also a context Funput itself authored, both before the first key of the word and
    // between the two deletions that turn "do" into "đo". The echo ledger matched it, the
    // change was written off as a delayed host echo, and the composer kept "do9" alive to
    // leak in front of the next word.
    //
    // Nothing in the *content* of that callback separates it from the late echo that
    // `KeyboardDocumentSynchronizerEchoOrderingTests` requires Funput to honour, so the
    // clock is what settles it: a host echoes within a runloop turn, while reaching for
    // Send takes a human moment. Both tests below advance the echo clock past that moment.
    @Test("Host clearing the field (e.g. tapping Send) does not leak stale raw keys into the next word")
    func hostClearsFieldWithoutReturnKey() {
        var now = 10.0
        let coordinator = KeyboardInputCoordinator(echoClock: { now }) // VNI is the default.
        let document = TestKeyboardWriter()

        // VNI raw keys "d", "o", "9" compose "đo" — never reaches a word boundary.
        type("do", with: coordinator, into: document)
        type("9", role: .vniModifier, with: coordinator, into: document)
        #expect(document.text == "đo")

        // The host (e.g. Messenger's Send button) wipes the field on its own and
        // reports the change through the normal UIKit callback — no `.enter` key
        // ever goes through the coordinator.
        now += 1
        document.replaceTextExternally(with: "")
        coordinator.synchronizeDocument(document, event: .textChanged)

        // The user starts a brand new word.
        type("test", with: coordinator, into: document)

        #expect(document.text == "test")
    }

    // The same leak without a cleared field: any external edit landing on a context Funput
    // authored earlier in the word was swallowed, which is why this is not about Send
    // buttons or word boundaries at all.
    @Test("An external edit back to an authored prefix does not leak stale raw keys")
    func externalEditToAuthoredPrefix() {
        var now = 10.0
        let coordinator = KeyboardInputCoordinator(echoClock: { now })
        let document = TestKeyboardWriter()

        type("do", with: coordinator, into: document)
        type("9", role: .vniModifier, with: coordinator, into: document)

        now += 1
        document.replaceTextExternally(with: "do")
        coordinator.synchronizeDocument(document, event: .textChanged)
        type("test", with: coordinator, into: document)

        #expect(document.text == "dotest")
    }
}
#endif
