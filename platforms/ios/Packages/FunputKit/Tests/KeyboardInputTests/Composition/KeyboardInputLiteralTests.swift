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
