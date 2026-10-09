import KeyboardLayout
import Testing

struct PeriodKeyTests {
    @Test("Period catalog matches Android and leaves the period itself to a tap")
    func catalog() {
        #expect(PunctuationKeyAlternates.period.map(\.text) == [
            "@", "&", "%", "+", ";", "/", "(", ")", "\"", "'", "#", "-", ":", "!", "?",
        ])
        #expect(PunctuationKeyAlternates.periodColumns == 8)
        #expect(PunctuationKeyAlternates.period.allSatisfy {
            $0.text(for: .uppercase) == $0.text && $0.accessibilityLabel != $0.text
        })
    }

    @Test("Every text period key shares the catalog", arguments: KeyboardLayoutPreset.allCases)
    func sharedCatalog(preset: KeyboardLayoutPreset) {
        var checked = 0
        for method in KeyboardInputMethod.allCases {
            for editor in KeyboardEditorMode.allCases where !Self.decimalEditors.contains(editor) {
                for mode in KeyboardLayoutMode.allCases {
                    for numberRow in [true, false] {
                        for key in periods(method, mode, editor, numberRow, preset) {
                            #expect(key.alternates == PunctuationKeyAlternates.period, "\(key.id)")
                            #expect(key.alternateColumns == PunctuationKeyAlternates.periodColumns)
                            #expect(key.role == .punctuation)
                            #expect(key.accessibilityLabel == "Dấu chấm")
                            checked += 1
                        }
                    }
                }
            }
        }
        #expect(checked > 0)
    }

    @Test("The Funput letters page and both symbol presets offer the palette")
    func coverage() {
        #expect(!periods(.telex, .letters, .text, true, .funput).isEmpty)
        #expect(!periods(.telex, .letters, .email, false, .funput).isEmpty)
        #expect(!periods(.vni, .letters, .password, true, .funput).isEmpty)
        for preset in KeyboardLayoutPreset.allCases {
            #expect(!periods(.telex, .symbolsPrimary, .text, true, preset).isEmpty)
            #expect(!periods(.telex, .symbolsSecondary, .text, false, preset).isEmpty)
        }
    }

    @Test("A decimal keypad separator has no palette", arguments: decimalEditors)
    func decimalKeypad(editor: KeyboardEditorMode) throws {
        let separator = try #require(periods(.telex, .letters, editor, true, .funput).first)
        #expect(separator.alternates.isEmpty)
        #expect(separator.alternateColumns == nil)
    }

    static let decimalEditors: [KeyboardEditorMode] = [.numberDecimal, .numberSignedDecimal]

    private func periods(
        _ method: KeyboardInputMethod,
        _ mode: KeyboardLayoutMode,
        _ editor: KeyboardEditorMode,
        _ numberRow: Bool,
        _ preset: KeyboardLayoutPreset
    ) -> [KeySpec] {
        KeyboardLayoutResolver.resolve(
            inputMethod: method,
            mode: mode,
            editorMode: editor,
            showsNumberRow: numberRow,
            preset: preset
        ).rows.flatMap(\.keys).filter { $0.label == "." }
    }
}
