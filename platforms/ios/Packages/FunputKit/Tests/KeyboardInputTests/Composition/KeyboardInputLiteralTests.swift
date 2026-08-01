#if os(iOS) && canImport(FunputCore)
import KeyboardLayout
@testable import KeyboardInput
import Testing

@MainActor
@Suite("Literal input")
struct KeyboardInputLiteralTests {
    @Test("Emoji bypasses Vietnamese composition")
    func insertLiteral() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        document.replaceTextExternally(with: "xin")
        coordinator.insertLiteral("👨‍👩‍👧", writer: document)
        #expect(document.text == "xin👨‍👩‍👧")
        #expect(coordinator.composer.buffer().isEmpty)
    }

    /// Kaomoji are multi-character literals containing spaces and punctuation, so
    /// they exercise the literal path harder than a single emoji glyph does.
    @Test("Kaomoji inserts whole and leaves Vietnamese composition working")
    func insertKaomoji() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        document.replaceTextExternally(with: "haha")
        coordinator.insertLiteral("(╯°□°)╯︵ ┻━┻", writer: document)
        #expect(document.text == "haha(╯°□°)╯︵ ┻━┻")
        #expect(coordinator.composer.buffer().isEmpty)

        coordinator.handle(testKey(.space), writer: document)
        type("chaof", with: coordinator, into: document)
        #expect(document.text == "haha(╯°□°)╯︵ ┻━┻ chào")
    }

    @Test("Literal delete uses document synchronization")
    func deleteLiteral() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        document.replaceTextExternally(with: "a😀")
        coordinator.deleteBackward(writer: document)
        #expect(document.text == "a")
        #expect(coordinator.composer.buffer().isEmpty)
    }
}
#endif
