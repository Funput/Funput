#if os(iOS) && canImport(FunputCore)
import FunputShared
@testable import KeyboardInput
import KeyboardLayout
import Testing

/// The panel field and the document each keep their own composition, and the field
/// follows whatever the document is set to when it is configured.
@MainActor
struct LocalTextIsolationTests {
    @Test("Typing in the field leaves the document's word open")
    func documentUntouched() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        type("a", with: coordinator, into: document)

        let field = LocalTextComposer()
        coordinator.configure(field)
        typeLocal("s", into: field)
        type("s", with: coordinator, into: document)

        #expect(field.text == "s")
        #expect(document.text == "á")
    }

    @Test("Inserting a result into the document keeps the query composing")
    func fieldSurvivesDocumentInsert() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let field = LocalTextComposer()
        coordinator.configure(field)
        typeLocal("cuwowi", into: field)

        coordinator.insertLiteral("😀", writer: TestKeyboardWriter())
        typeLocal("f", into: field)

        #expect(field.text == "cười")
    }

    @Test("Follows the method the Telex/VNI key last chose")
    func followsRuntimeMethod() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        coordinator.toggleInputMethod()
        let field = LocalTextComposer()
        coordinator.configure(field)
        typeLocal("as", into: field)
        #expect(field.text == "á")
    }

    @Test("Carries the saved tone style")
    func followsToneStyle() {
        let modern = configuredField(.modern)
        let traditional = configuredField(.traditional)
        typeLocal("hoaf", into: modern)
        typeLocal("hoaf", into: traditional)
        #expect(modern.text == "hoà")
        #expect(traditional.text == "hòa")
    }

    private func configuredField(_ toneStyle: ToneStyleOption) -> LocalTextComposer {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(FunputConfiguration(inputMethod: .telex, toneStyle: toneStyle))
        let field = LocalTextComposer()
        coordinator.configure(field)
        return field
    }
}
#endif
