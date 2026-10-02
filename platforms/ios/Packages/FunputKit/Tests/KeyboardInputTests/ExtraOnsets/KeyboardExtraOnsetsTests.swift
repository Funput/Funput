#if os(iOS) && canImport(FunputCore)
import FunputEngine
import FunputShared
import KeyboardLayout
import Testing
@testable import KeyboardInput

@MainActor
struct KeyboardExtraOnsetsTests {
    @Test("Shared letters map individually to the independent engine bits")
    func mapping() {
        #expect(ExtraOnsetLetters.z.engineOnsets.rawValue == 8)
        #expect(ExtraOnsetLetters.f.engineOnsets.rawValue == 1)
        #expect(ExtraOnsetLetters.w.engineOnsets.rawValue == 4)
        #expect(ExtraOnsetLetters.j.engineOnsets.rawValue == 2)
        #expect(ExtraOnsetLetters.all.engineOnsets.rawValue == 15)
        #expect(ExtraOnsetLetters().engineOnsets.isEmpty)
    }

    @Test("Coordinator applies selected onsets and subsequent disabling")
    func documentComposition() {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(FunputConfiguration(inputMethod: .telex, extraOnsets: [.z]))
        let document = TestKeyboardWriter()
        typeDocument("zoo fair ", with: coordinator, into: document)
        #expect(document.text == "zô fair ")
        coordinator.apply(FunputConfiguration(inputMethod: .telex, extraOnsets: [.z, .f]))
        typeDocument("fair food ", with: coordinator, into: document)
        #expect(document.text == "zô fair fải food ")
        coordinator.apply(FunputConfiguration(inputMethod: .telex))
        typeDocument("zoo ", with: coordinator, into: document)
        #expect(document.text.hasSuffix("zoo "))
    }

    @Test("Emoji search follows the coordinator's current onset selection")
    func localComposition() {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(FunputConfiguration(inputMethod: .telex, extraOnsets: [.z, .f]))
        let field = LocalTextComposer()
        coordinator.configure(field)
        typeLocal("zoo fair food", into: field)
        #expect(field.text == "zô fải food")
        coordinator.apply(FunputConfiguration(inputMethod: .telex))
        coordinator.configure(field)
        typeLocal("zoo fair", into: field)
        #expect(field.text == "zoo fair")
    }

    @Test("English bypasses extra onsets in the document and local field")
    func englishPassthrough() {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(FunputConfiguration(inputMethod: .telex, language: .english, extraOnsets: .all))
        let document = TestKeyboardWriter()
        typeDocument("zoo fair ", with: coordinator, into: document)
        #expect(document.text == "zoo fair ")
        let field = LocalTextComposer()
        coordinator.configure(field)
        typeLocal("zoo fair", into: field)
        #expect(field.text == "zoo fair")
    }

    private func typeDocument(
        _ keys: String, with coordinator: KeyboardInputCoordinator, into document: TestKeyboardWriter
    ) {
        for character in keys {
            type(String(character), role: character == " " ? .space : .character, with: coordinator, into: document)
        }
    }
}
#endif
