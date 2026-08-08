import CoreGraphics
import KeyboardLayout
import Testing

struct SystemSymbolsParityTests {
    @Test("Both symbol pages are four rows", arguments: KeyboardInputMethod.allCases)
    func fourRows(method: KeyboardInputMethod) {
        #expect(SystemSymbolKeyboardLayouts.primary(method).rows.count == 4)
        #expect(SystemSymbolKeyboardLayouts.secondary(method).rows.count == 4)
    }

    @Test("Page one matches the stock keyboard")
    func primaryContent() {
        let rows = SystemSymbolKeyboardLayouts.primary(.vni).rows
        #expect(rows[0].keys.map(\.label).joined() == "1234567890")
        #expect(rows[0].keys.allSatisfy { $0.role == .punctuation })
        #expect(rows[1].keys.map(\.label) == ["-", "/", ":", ";", "(", ")", "đ", "&", "@", "\""])
        #expect(rows[2].keys.map(\.label) == ["#+=", ".", ",", "?", "!", "'", ""])
    }

    @Test("Page one carries the letter đ, not the currency sign")
    func primaryUsesTheLetterDe() {
        // "đ" U+0111 vs "₫" U+20AB. The Funput preset's symbol page carries the currency
        // sign, so a copy-paste between the two content files would look right in a diff
        // and be wrong on the keyboard.
        let labels = SystemSymbolKeyboardLayouts.primary(.vni).rows[1].keys.map(\.label)
        #expect(labels.contains("\u{0111}"))
        #expect(!labels.contains("\u{20AB}"))
    }

    @Test("Page two matches the stock keyboard")
    func secondaryContent() {
        let rows = SystemSymbolKeyboardLayouts.secondary(.vni).rows
        #expect(rows[0].keys.map(\.label) == ["[", "]", "{", "}", "#", "%", "^", "*", "+", "="])
        #expect(rows[1].keys.map(\.label) == ["_", "\\", "|", "~", "<", ">", "$", "¥", "€", "•"])
        #expect(rows[2].keys.map(\.label) == ["123", ".", ",", "?", "!", "'", ""])
    }

    @Test("Page switch keys keep their roles", arguments: KeyboardInputMethod.allCases)
    func switchKeys(method: KeyboardInputMethod) {
        let primary = SystemSymbolKeyboardLayouts.primary(method).rows[2].keys
        #expect(primary.first?.role == .moreSymbols)
        #expect(primary.first?.widthWeight == 1.5)
        #expect(primary.last?.role == .backspace)
        #expect(primary.last?.widthWeight == 1.5)

        let secondary = SystemSymbolKeyboardLayouts.secondary(method).rows[2].keys
        #expect(secondary.first?.role == .symbols)
        #expect(secondary.first?.widthWeight == 1.5)
    }

    @Test("Both pages share the letters action row", arguments: KeyboardInputMethod.allCases)
    func actionRow(method: KeyboardInputMethod) {
        for layout in [
            SystemSymbolKeyboardLayouts.primary(method),
            SystemSymbolKeyboardLayouts.secondary(method),
        ] {
            let keys = layout.rows.last?.keys ?? []
            #expect(keys.map(\.label) == ["ABC", "", "Tiếng Việt", ""])
            #expect(keys.map(\.role) == [.letters, .emoji, .space, .enter])
            #expect(SystemLettersParityTests.matches(
                keys.map(\.widthWeight),
                [1.56, 1.4, 5.64, 2.6]
            ))
            #expect(layout.toolbar?.keys.map(\.role) == [.clipboard, .emoji])
        }
    }

    @Test("The preset leaves every other editor mode untouched", arguments: KeyboardInputMethod.allCases)
    func onlyTextAndSearchAreAffected(method: KeyboardInputMethod) {
        // Email and URL differ from the stock keyboard along axes the preset does not
        // describe, and the keypads have no row of this shape at all. Password and PIN
        // additionally must keep `toolbar: nil`, which is what makes the emoji panel
        // unreachable in a secure field — selecting the preset must not open that door.
        for editor in KeyboardEditorMode.allCases where !editor.usesSystemPreset {
            for mode in KeyboardLayoutMode.allCases {
                for showsNumberRow in [true, false] {
                    let system = KeyboardLayoutResolver.resolve(
                        inputMethod: method,
                        mode: mode,
                        editorMode: editor,
                        showsNumberRow: showsNumberRow,
                        preset: .system
                    )
                    let funput = KeyboardLayoutResolver.resolve(
                        inputMethod: method,
                        mode: mode,
                        editorMode: editor,
                        showsNumberRow: showsNumberRow,
                        preset: .funput
                    )
                    #expect(system == funput)
                    #expect(!system.rows.flatMap(\.keys).contains { $0.role == .emoji })
                }
            }
        }
    }

    @Test("Symbol pages ignore the number row preference", arguments: KeyboardInputMethod.allCases)
    func numberRowIndependence(method: KeyboardInputMethod) {
        // Page one always carries the digits, so unlike the Funput preset there is no
        // compact variant to fall back to.
        for mode in [KeyboardLayoutMode.symbolsPrimary, .symbolsSecondary] {
            let shown = KeyboardLayoutResolver.resolve(
                inputMethod: method,
                mode: mode,
                showsNumberRow: true,
                preset: .system
            )
            let hidden = KeyboardLayoutResolver.resolve(
                inputMethod: method,
                mode: mode,
                showsNumberRow: false,
                preset: .system
            )
            #expect(shown == hidden)
        }
    }
}
