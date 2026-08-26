import FunputShared
import KeyboardLayout
import KeyboardRenderer
import Testing
@testable import Funput

@MainActor
struct ToolbarConfigurationTests {
    @Test("Disabled suggestions remove the system toolbar and its height")
    func disabledSuggestions() {
        var configuration = FunputConfiguration.default
        configuration.layoutPreset = .system
        configuration.personalSuggestionsEnabled = true
        let shown = KeyboardPreviewPresentation.make(configuration: configuration)

        configuration.personalSuggestionsEnabled = false
        let hidden = KeyboardPreviewPresentation.make(configuration: configuration)

        #expect(shown.layout.toolbar != nil)
        #expect(hidden.layout.toolbar == nil)
        #expect(hidden.layout.allowsEmojiPanel)
        #expect(
            KeyboardMetrics.phonePortraitHeight(for: hidden.layout)
                < KeyboardMetrics.phonePortraitHeight(for: shown.layout)
        )
    }

    @Test("Disabled suggestions also remove the Funput toolbar")
    func funputPresetAlsoHides() {
        var configuration = FunputConfiguration.default
        configuration.layoutPreset = .funput
        configuration.personalSuggestionsEnabled = false

        let presentation = KeyboardPreviewPresentation.make(configuration: configuration)

        #expect(presentation.layout.toolbar == nil)
        #expect(presentation.layout.allowsEmojiPanel)
    }

    @Test("Email and URL hide the toolbar independently of the preset")
    func webEditorsHide() {
        for editorMode in [KeyboardEditorMode.email, .url] {
            for preset in KeyboardLayoutPreset.allCases {
                let layout = KeyboardLayoutResolver.resolve(
                    inputMethod: .telex,
                    mode: .letters,
                    editorMode: editorMode,
                    preset: preset,
                    showsToolbar: false
                )

                #expect(layout.toolbar == nil)
                #expect(layout.allowsEmojiPanel)
            }
        }
    }
}
