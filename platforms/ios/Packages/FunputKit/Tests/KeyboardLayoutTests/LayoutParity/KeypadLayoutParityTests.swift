import KeyboardLayout
import Testing

struct KeypadLayoutParityTests {
    @Test("Phone is a four by four dial pad")
    func phone() {
        let layout = resolve(.phone)
        #expect(rows(layout) == [
            ["1", "2", "3", ""],
            ["4", "5", "6", ""],
            ["7", "8", "9", "+"],
            ["*", "0", "#", ""],
        ])
        #expect(layout.rows[0].keys[3].role == .backspace)
        #expect(layout.rows[1].keys[3].role == .enter)
        #expect(layout.rows[2].keys[3].role == .punctuation)
        #expect(layout.rows[3].keys.map(\.role) == [
            .punctuation, .character, .punctuation, .placeholder,
        ])
        assertUnitWeights(layout)
        #expect(layout.toolbar == nil)
    }

    @Test("PIN contains digits, commands, and placeholders")
    func pin() {
        let layout = resolve(.pin)
        let text = layout.rows.flatMap(\.keys)
            .filter { $0.role == .character || $0.role == .punctuation }
            .map(\.label).joined()
        #expect(text == "1234567890")
        #expect(layout.rows[0].keys[3].role == .backspace)
        #expect(layout.rows[1].keys[3].role == .enter)
        #expect(layout.rows.flatMap(\.keys).filter { $0.role == .placeholder }.count == 4)
        assertUnitWeights(layout)
    }

    @Test("Numeric pages match Android labels, roles, and weights")
    func numericVariants() {
        let expected: [KeyboardEditorMode: [[String]]] = [
            .number: numberRows(period: "", sign: "", comma: ""),
            .numberDecimal: numberRows(period: ".", sign: "", comma: ","),
            .numberSigned: numberRows(period: "", sign: "-", comma: ""),
            .numberSignedDecimal: numberRows(period: ".", sign: "-", comma: ","),
        ]
        for (mode, expectedRows) in expected {
            let layout = resolve(mode)
            #expect(rows(layout) == expectedRows)
            #expect(layout.rows[0].keys[3].role == .backspace)
            #expect(layout.rows[1].keys[3].role == .enter)
            #expect(layout.rows[2].keys[3].role == (mode.allowsDecimal ? .punctuation : .placeholder))
            #expect(layout.rows[3].keys[0].role == (mode.allowsSigned ? .punctuation : .placeholder))
            #expect(layout.rows[3].keys[2].role == .placeholder)
            #expect(layout.rows[3].keys[3].role == (mode.allowsDecimal ? .punctuation : .placeholder))
            assertUnitWeights(layout)
        }
    }

    @Test("Keypad editor modes disable Vietnamese composition")
    func compositionPolicy() {
        let modes: [KeyboardEditorMode] = [
            .phone, .pin, .number, .numberDecimal, .numberSigned, .numberSignedDecimal, .password,
        ]
        #expect(modes.allSatisfy { !$0.supportsVietnameseComposition })
    }

    private func resolve(_ mode: KeyboardEditorMode) -> KeyboardLayout {
        KeyboardLayoutResolver.resolve(inputMethod: .vni, mode: .letters, editorMode: mode)
    }

    private func rows(_ layout: KeyboardLayout) -> [[String]] {
        layout.rows.map { $0.keys.map(\.label) }
    }

    private func numberRows(period: String, sign: String, comma: String) -> [[String]] {
        [
            ["1", "2", "3", ""],
            ["4", "5", "6", ""],
            ["7", "8", "9", period],
            [sign, "0", "", comma],
        ]
    }

    private func assertUnitWeights(_ layout: KeyboardLayout) {
        #expect(layout.rows.flatMap(\.keys).allSatisfy { $0.widthWeight == 1 })
    }
}
