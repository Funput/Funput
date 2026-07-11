#if os(iOS) && canImport(FunputCore)
import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct KeyboardInputEditorPolicyTests {
    @Test("ASCII editor modes bypass Vietnamese composition")
    func asciiModes() {
        for mode in [KeyboardEditorMode.email, .url, .password] {
            let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
            let document = TestKeyboardDocument()
            coordinator.updateContext(editorMode: mode, enterAction: .done)

            type("as", with: coordinator, into: document)

            #expect(document.text == "as")
            #expect(coordinator.state.editorMode == mode)
            #expect(coordinator.state.enterAction == .done)
        }
    }

    @Test("Search mode keeps Vietnamese composition")
    func searchComposition() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardDocument()
        coordinator.updateContext(editorMode: .search, enterAction: .search)

        type("as", with: coordinator, into: document)

        #expect(document.text == "á")
        #expect(coordinator.state.enterAction == .search)
    }

    @Test("Every editor mode accepts direct input and exactly one Return")
    func completeEditorMatrix() {
        for mode in KeyboardEditorMode.allCases {
            let coordinator = KeyboardInputCoordinator()
            let document = TestKeyboardDocument()
            coordinator.updateContext(editorMode: mode, enterAction: .done)

            type("7", with: coordinator, into: document)
            coordinator.handle(testKey(.enter), document: document)

            #expect(document.text == "7\n")
        }
    }

    @Test("Keypad modes reject symbol-page navigation")
    func keypadNavigation() {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardDocument()
        coordinator.updateContext(editorMode: .numberDecimal, enterAction: .done)

        coordinator.handle(testKey(.symbols), document: document)

        #expect(coordinator.state.layoutMode == .letters)
    }

    @Test("Changing editor context clears active composition")
    func contextClearsComposition() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardDocument()
        type("a", with: coordinator, into: document)

        coordinator.updateContext(editorMode: .email, enterAction: .done)
        type("s", with: coordinator, into: document)

        #expect(document.text == "as")
        #expect(coordinator.state.shiftState == .lowercase)
        #expect(coordinator.state.layoutMode == .letters)
    }

    @Test("A host-requested initial symbol page clears active composition")
    func initialSymbolPageClearsComposition() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardDocument()
        type("a", with: coordinator, into: document)

        coordinator.updateContext(
            editorMode: .text,
            enterAction: .newLine,
            initialLayoutMode: .symbolsPrimary
        )
        coordinator.handle(testKey(.letters), document: document)
        type("s", with: coordinator, into: document)

        #expect(document.text == "as")
    }
}
#endif
