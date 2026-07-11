import KeyboardLayout
import KeyboardRenderer
import Testing
@testable import Funput

@MainActor
struct FunputTests {
    @Test("Keyboard Lab exposes the complete selector matrix")
    func selectorMatrix() {
        #expect(KeyboardInputMethod.allCases.count == 2)
        #expect(KeyboardEditorMode.allCases.count == 11)
        #expect(KeyboardLayoutMode.allCases.count == 3)
        #expect(KeyboardLanguage.allCases.count == 2)
        #expect(KeyboardLabEnterAction.allCases.count == 8)

        for method in KeyboardInputMethod.allCases {
            for editor in KeyboardEditorMode.allCases {
                for mode in KeyboardLayoutMode.allCases {
                    var configuration = KeyboardLabConfiguration.default
                    configuration.inputMethod = method
                    configuration.editorMode = editor
                    configuration.layoutMode = mode
                    #expect(!configuration.layout.rows.isEmpty)
                }
            }
        }
    }

    @Test("Keypads hide symbol page selection and resolve to letters")
    func keypadPolicy() {
        for editor in KeyboardEditorMode.allCases where editor.usesKeypad {
            var configuration = KeyboardLabConfiguration.default
            configuration.editorMode = editor
            configuration.layoutMode = .symbolsSecondary
            #expect(!configuration.supportsLayoutModeSelection)
            #expect(configuration.resolvedLayoutMode == .letters)
            #expect(configuration.layout.rows.count == 4)
        }
    }

    @Test("Language selection follows swipe support")
    func languagePolicy() {
        var configuration = KeyboardLabConfiguration.default
        #expect(configuration.supportsLanguageSwipe)

        configuration.editorMode = .search
        #expect(configuration.supportsLanguageSwipe)

        for editor in [KeyboardEditorMode.email, .url] {
            configuration.editorMode = editor
            #expect(!configuration.supportsLanguageSwipe)
        }

        configuration.layoutMode = .symbolsPrimary
        for editor in [KeyboardEditorMode.text, .search, .email, .url] {
            configuration.editorMode = editor
            #expect(configuration.supportsLanguageSwipe)
        }

        for editor in [
            KeyboardEditorMode.phone, .password, .pin,
            .number, .numberDecimal, .numberSigned, .numberSignedDecimal,
        ] {
            configuration.editorMode = editor
            #expect(!configuration.supportsLanguageSwipe)
        }
    }

    @Test("System switcher preview requires a toolbar")
    func systemSwitcherPolicy() {
        var configuration = KeyboardLabConfiguration.default
        configuration.showsSystemInputModeKey = true
        #expect(configuration.supportsSystemInputModePreview)
        #expect(configuration.layout.toolbar?.systemInputModeKey != nil)

        for editor in [KeyboardEditorMode.password, .phone, .pin, .number] {
            configuration.editorMode = editor
            #expect(!configuration.supportsSystemInputModePreview)
            #expect(configuration.layout.toolbar == nil)
        }
    }

    @Test("Presentation reflects all preview selections")
    func presentation() {
        var configuration = KeyboardLabConfiguration.default
        configuration.inputMethod = .vni
        configuration.layoutMode = .symbolsSecondary
        configuration.language = .english
        configuration.enterAction = .custom
        configuration.showsSystemInputModeKey = true

        let presentation = configuration.presentation
        #expect(presentation.layout.inputMethod == .vni)
        #expect(presentation.layout.id.contains("symbols-secondary"))
        #expect(presentation.layout.toolbar?.systemInputModeKey != nil)
        #expect(presentation.language == .english)
        #expect(presentation.enterAction == .custom("Apply"))
    }
}