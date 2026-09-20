import FunputShared
import KeyboardLayout
import KeyboardRenderer
import Testing
@testable import Funput

@MainActor
struct ToolbarConfigurationTests {
    @Test("The band stays for the clipboard when suggestions are off")
    func suggestionsOffKeepsTheBand() {
        var configuration = FunputConfiguration.default
        configuration.personalSuggestionsEnabled = false
        configuration.clipboardEnabled = true

        for preset in KeyboardLayoutPreset.allCases {
            configuration.layoutPreset = preset
            let presentation = KeyboardPreviewPresentation.make(configuration: configuration)
            #expect(presentation.layout.toolbar != nil)
        }
    }

    @Test(
        "Switching both features off removes the toolbar and its height",
        arguments: KeyboardLayoutPreset.allCases
    )
    func bothFeaturesOffRemovesTheBand(preset: KeyboardLayoutPreset) {
        var configuration = FunputConfiguration.default
        configuration.layoutPreset = preset
        let shown = KeyboardPreviewPresentation.make(configuration: configuration)

        configuration.personalSuggestionsEnabled = false
        configuration.clipboardEnabled = false
        let hidden = KeyboardPreviewPresentation.make(configuration: configuration)

        #expect(shown.layout.toolbar != nil)
        #expect(hidden.layout.toolbar == nil)
        #expect(hidden.layout.allowsEmojiPanel)
        #expect(
            KeyboardMetrics.phonePortraitHeight(for: hidden.layout)
                < KeyboardMetrics.phonePortraitHeight(for: shown.layout)
        )
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
