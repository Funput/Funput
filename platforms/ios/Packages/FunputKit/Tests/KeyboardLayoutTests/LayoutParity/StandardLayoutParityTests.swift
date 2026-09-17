import KeyboardLayout
import Testing

struct StandardLayoutParityTests {
    @Test("TELEX and VNI use five rows", arguments: KeyboardInputMethod.allCases)
    func fiveRows(method: KeyboardInputMethod) {
        #expect(StandardKeyboardLayouts.letters(method).rows.count == 5)
    }

    @Test("TELEX top row contains plain digits")
    func telexDigits() {
        let keys = StandardKeyboardLayouts.letters(.telex).rows[0].keys
        #expect(keys.map(\.label).joined() == "1234567890")
        #expect(keys.allSatisfy { $0.role == .character && $0.secondaryLabel == nil })
    }

    @Test("Telex tone keys expose the expected hints without changing semantics")
    func telexToneHints() {
        let keys = StandardKeyboardLayouts.letters(.telex).rows.flatMap(\.keys)
        let hinted = Dictionary(uniqueKeysWithValues: keys.compactMap { key in
            key.secondaryLabel.map { (key.label, $0) }
        })
        #expect(hinted == ["s": "´", "f": "`", "r": "̉", "x": "˜", "j": "̣", "z": "×"])
        #expect(keys.filter { hinted[$0.label] != nil }.allSatisfy {
            $0.role == .character && $0.widthWeight == 1 && $0.id == "character-\($0.label)"
        })
        #expect(keys.first { $0.label == "s" }?.accessibilityLabel == "S, dấu sắc")
        #expect(keys.first { $0.label == "z" }?.accessibilityLabel == "Z, xóa dấu")
    }

    @Test("VNI top row exposes modifier hints")
    func vniHints() {
        let keys = StandardKeyboardLayouts.letters(.vni).rows[0].keys
        #expect(keys.map(\.label).joined() == "1234567890")
        #expect(keys.allSatisfy { $0.role == .vniModifier })
        #expect(keys.compactMap(\.secondaryLabel) == ["´", "`", "̉", "˜", "̣", "ˆ", "+", "˘", "đ", "×"])
    }

    @Test("QWERTY rows match Android", arguments: KeyboardInputMethod.allCases)
    func qwertyRows(method: KeyboardInputMethod) {
        let rows = StandardKeyboardLayouts.letters(method).rows
        #expect(rows[1].keys.map(\.label).joined() == "qwertyuiop")
        #expect(rows[2].keys.map(\.label).joined() == "asdfghjkl")
        #expect(rows[3].keys.map(\.label) == ["", "z", "x", "c", "v", "b", "n", "m", ""])
        #expect(rows[1].keys.allSatisfy { $0.role == .character && $0.widthWeight == 1 })
        #expect(rows[2].keys.allSatisfy { $0.role == .character && $0.widthWeight == 1 })
        #expect(rows[3].keys.map(\.role) == [
            .shift, .character, .character, .character, .character,
            .character, .character, .character, .backspace,
        ])
        #expect(rows[3].keys.map(\.widthWeight) == [1.5, 1, 1, 1, 1, 1, 1, 1, 1.5])
    }

    @Test("Standard action row matches Android", arguments: KeyboardInputMethod.allCases)
    func actionRow(method: KeyboardInputMethod) {
        let keys = StandardKeyboardLayouts.letters(method).rows[4].keys
        #expect(keys.map(\.label) == ["?123", ",", "Tiếng Việt", ".", ""])
        #expect(keys.map(\.role) == [.symbols, .punctuation, .space, .punctuation, .enter])
        #expect(keys.map(\.widthWeight) == [1.7, 1, 5.8, 1, 1.7])
        #expect(keys[2].horizontalSwipeAction == .toggleLanguage)
    }

    @Test("Toolbar exposes the brand logo and optional system switcher", arguments: KeyboardInputMethod.allCases)
    func toolbar(method: KeyboardInputMethod) {
        // The brand logo is decorative, so the panel keys are the only interactive
        // toolbar keys. The resolver must not smuggle them into the rows either.
        let standard = StandardKeyboardLayouts.letters(method)
        #expect(standard.toolbar?.keys.map(\.role) == [.clipboard, .emoji])

        let layout = KeyboardLayoutResolver.resolve(
            inputMethod: method,
            mode: .letters,
            preset: .funput
        )
        #expect(layout.toolbar?.keys.map(\.role) == [.clipboard, .emoji])
        // Keeping emoji out of the rows is a Funput-preset invariant: the system preset
        // deliberately places one in its action row, where Apple puts it. See
        // `SystemLettersParityTests.actionRow`.
        #expect(!layout.rows.flatMap(\.keys).contains { $0.role == .emoji })
    }

    @Test("No preset puts a clipboard key in a row", arguments: KeyboardInputMethod.allCases)
    func clipboardStaysInTheToolbar(method: KeyboardInputMethod) {
        // `KeyboardKeyContentStyle.icon(for:)` has no `.clipboard` case, so a row-placed
        // clipboard key would render as a blank keycap rather than fail loudly.
        for preset in KeyboardLayoutPreset.allCases {
            for mode in KeyboardLayoutMode.allCases {
                let layout = KeyboardLayoutResolver.resolve(
                    inputMethod: method,
                    mode: mode,
                    preset: preset
                )
                #expect(!layout.rows.flatMap(\.keys).contains { $0.role == .clipboard })
            }
        }
    }
}
