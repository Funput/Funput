import CoreGraphics
import KeyboardLayout
import Testing

struct ToolbarVisibilityTests {
    @Test("System preset hides its toolbar when suggestions are off", arguments: KeyboardLayoutMode.allCases)
    func hiddenWithoutSuggestions(mode: KeyboardLayoutMode) {
        let layout = resolve(preset: .system, mode: mode, showsToolbar: false)

        #expect(layout.toolbar == nil)
        #expect(layout.allowsEmojiPanel)
        #expect(layout.rows.last?.keys.contains { $0.role == .emoji } == true)
    }

    @Test("Hiding the system toolbar returns its area to the rows")
    func toolbarlessGeometry() {
        let shown = resolve(preset: .system, showsToolbar: true)
        let hidden = resolve(preset: .system, showsToolbar: false)
        let size = CGSize(width: 390, height: 304)
        let shownGeometry = KeyboardGeometry.resolve(layout: shown, size: size, sizing: .default)
        let hiddenGeometry = KeyboardGeometry.resolve(layout: hidden, size: size, sizing: .default)

        #expect(shownGeometry.toolbarFrame != nil)
        #expect(hiddenGeometry.toolbarFrame == nil)
        #expect(hiddenGeometry.rows[0][0].frame.minY < shownGeometry.rows[0][0].frame.minY)
    }

    @Test("Toolbar visibility is preset-independent")
    func everyPresetHides() {
        for preset in KeyboardLayoutPreset.allCases {
            for mode in KeyboardLayoutMode.allCases {
                let layout = resolve(preset: preset, mode: mode, showsToolbar: false)
                #expect(layout.toolbar == nil)
                #expect(layout.allowsEmojiPanel)
            }
        }
    }

    @Test("Email and URL hide the toolbar for either preset")
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
                #expect(layout.rows.last?.keys[1].role == .emoji)
            }
        }
    }

    @Test("Password layouts still block the emoji panels")
    func passwordStaysProtected() {
        let password = KeyboardLayoutResolver.resolve(
            inputMethod: .telex,
            mode: .letters,
            editorMode: .password,
            preset: .system,
            showsToolbar: false
        )

        #expect(!password.allowsEmojiPanel)
    }

    private func resolve(
        preset: KeyboardLayoutPreset,
        mode: KeyboardLayoutMode = .letters,
        showsToolbar: Bool
    ) -> KeyboardLayout {
        KeyboardLayoutResolver.resolve(
            inputMethod: .telex,
            mode: mode,
            preset: preset,
            showsToolbar: showsToolbar
        )
    }
}
