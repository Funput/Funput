#if os(iOS) && canImport(FunputCore)
import FunputShared
import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct EnglishSuggestionInputTests {
    @Test(arguments: [KeyboardLanguage.vietnamese, .english])
    func tracksAndAcceptsInBothLanguages(_ language: KeyboardLanguage) {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        if language == .english { coordinator.toggleLanguage() }
        type("wh", with: coordinator, into: document)
        #expect(coordinator.takePersonalSuggestionUpdate().prefix == "wh")
        #expect(coordinator.acceptSuggestion("what", replacing: "wh", writer: document) != nil)
        #expect(document.text == "what ")
        #expect(coordinator.takePersonalSuggestionUpdate().completedToken == "what")
        #expect(coordinator.takePersonalSuggestionUpdate().completedToken == nil)
    }

    @Test func languageChangeAbandonsOldPrefix() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        type("wh", with: coordinator, into: document)
        coordinator.toggleLanguage()
        #expect(coordinator.takePersonalSuggestionUpdate() == .empty)
        #expect(coordinator.acceptSuggestion("what", replacing: "wh", writer: document) == nil)
        type("ip", with: coordinator, into: document)
        #expect(coordinator.takePersonalSuggestionUpdate().prefix == "ip")
    }

    @Test(arguments: KeyboardEditorMode.allCases)
    func editorPolicyStillAppliesInEnglish(_ mode: KeyboardEditorMode) {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        coordinator.toggleLanguage()
        coordinator.updateContext(inputContext(editorMode: mode, enterAction: .done))
        type("wh", with: coordinator, into: document)
        let prefix = coordinator.takePersonalSuggestionUpdate().prefix
        #expect(prefix == (mode.supportsVietnameseComposition ? "wh" : ""))
        let accepted = coordinator.acceptSuggestion("what", replacing: "wh", writer: document)
        #expect((accepted != nil) == mode.supportsVietnameseComposition)
    }

    @Test func disabledSuggestionsDoNotTrackEnglish() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        var configuration = FunputConfiguration.default
        configuration.language = .english
        configuration.personalSuggestionsEnabled = false
        coordinator.apply(configuration)
        type("wh", with: coordinator, into: document)
        #expect(coordinator.takePersonalSuggestionUpdate() == .empty)
        #expect(coordinator.acceptSuggestion("what", replacing: "wh", writer: document) == nil)
    }
}
#endif
