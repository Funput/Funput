#if os(iOS) && canImport(FunputCore)
import KeyboardLayout
import Testing

@testable import KeyboardInput

/// How a touch report travels with the key it belongs to — and stops there.
@MainActor
struct KeyboardTouchReportTests {
    private func evidence(_ typed: Unicode.Scalar) -> KeyboardTouchEvidence {
        KeyboardTouchEvidence(
            typed: typed,
            typedDistance: 0.3,
            first: .init(scalar: "s", distance: 0.2)
        )
    }

    @Test("A touch never outlives the key it was reported for")
    func spentOnOneKey() {
        // The engine consumes a report on the next keystroke it sees, so one left
        // behind would attach itself to whatever the user typed next.
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardWriter()

        coordinator.handle(testKey(.character, label: "a"), touch: evidence("a"), writer: document)

        #expect(coordinator.pendingTouch == nil)
    }

    @Test("A key raised without a touch reports nothing")
    func withoutTouch() {
        // VoiceOver and key repeat produce keys with no lift point to describe.
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardWriter()

        coordinator.handle(testKey(.character, label: "a"), writer: document)

        #expect(coordinator.pendingTouch == nil)
    }

    @Test("Reporting a touch does not change what the key types")
    func typingIsUnchanged() {
        // Until the setting is switched on the engine ignores the report entirely,
        // and the document must not be able to tell the difference.
        let withTouch = KeyboardInputCoordinator()
        let withTouchDocument = TestKeyboardWriter()
        let without = KeyboardInputCoordinator()
        let withoutDocument = TestKeyboardWriter()

        for label in ["m", "a"] {
            let key = testKey(.character, label: label)
            let scalar = key.label.unicodeScalars.first ?? "a"
            withTouch.handle(key, touch: evidence(scalar), writer: withTouchDocument)
            without.handle(key, writer: withoutDocument)
        }
        let modifier = testKey(.vniModifier, label: "1")
        withTouch.handle(modifier, writer: withTouchDocument)
        without.handle(modifier, writer: withoutDocument)

        #expect(withTouchDocument.text == withoutDocument.text)
        #expect(withTouchDocument.text == "má")
    }
}
#endif
