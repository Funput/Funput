#if os(iOS) && canImport(FunputCore)
import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct KeyboardInputLanguageTests {
    @Test("Vietnamese composes while English passes ASCII through")
    func languagePolicy() {
        let vietnamese = KeyboardInputCoordinator(inputMethod: .telex)
        let vietnameseDocument = TestKeyboardWriter()
        type("as", with: vietnamese, into: vietnameseDocument)

        let english = KeyboardInputCoordinator(inputMethod: .telex)
        let englishDocument = TestKeyboardWriter()
        english.toggleLanguage()
        type("as", with: english, into: englishDocument)

        #expect(vietnameseDocument.text == "á")
        #expect(englishDocument.text == "as")
        #expect(english.state.language == .english)
    }

    @Test("Toggling language clears active composition")
    func toggleClearsComposition() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        type("a", with: coordinator, into: document)

        coordinator.toggleLanguage()
        coordinator.toggleLanguage()
        type("s", with: coordinator, into: document)

        #expect(document.text == "as")
        #expect(coordinator.state.language == .vietnamese)
    }

    @Test("Language preference survives ASCII editor contexts")
    func contextPreservesLanguage() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        coordinator.toggleLanguage()

        coordinator.updateContext(inputContext(editorMode: .email, enterAction: .done))
        coordinator.updateContext(inputContext(editorMode: .text, enterAction: .newLine))
        type("as", with: coordinator, into: document)

        #expect(coordinator.state.language == .english)
        #expect(document.text == "as")
    }

    @Test("Returning to text re-enables Vietnamese composition")
    func contextReenablesComposition() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()

        coordinator.updateContext(inputContext(editorMode: .email, enterAction: .done))
        coordinator.updateContext(inputContext(editorMode: .text, enterAction: .newLine))
        type("as", with: coordinator, into: document)

        #expect(document.text == "á")
    }

    @Test("Input method changes do not change language")
    func methodIsIndependent() {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardWriter()
        coordinator.toggleLanguage()

        coordinator.handle(testKey(.inputMethod), writer: document)

        #expect(coordinator.state.inputMethod == .telex)
        #expect(coordinator.state.language == .english)
    }
}
#endif
