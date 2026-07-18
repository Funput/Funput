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
        // The brand logo is decorative, so the interactive toolbar keys are the
        // emoji button plus the globe when relevant.
        let standard = StandardKeyboardLayouts.letters(method)
        #expect(standard.toolbar?.keys.map(\.role) == [.emoji])

        let layout = KeyboardLayoutResolver.resolve(
            inputMethod: method,
            mode: .letters,
            showsSystemInputModeKey: true
        )
        #expect(layout.toolbar?.keys.map(\.role) == [
            .systemInputMode, .emoji,
        ])
        #expect(layout.toolbar?.systemInputModeKey?.role == .systemInputMode)
        #expect(!layout.rows.flatMap(\.keys).contains { $0.role == .systemInputMode })
    }
}
