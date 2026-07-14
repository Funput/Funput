import KeyboardLayout
import Testing

struct SymbolLayoutParityTests {
    @Test("Primary symbol page matches Android")
    func primary() {
        let layout = SymbolKeyboardLayouts.primary(.telex)
        #expect(layout.rows.count == 5)
        #expect(labels(layout, row: 0) == ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0"])
        #expect(labels(layout, row: 1) == ["@", "#", "₫", "$", "%", "&", "*", "+", "=", "/"])
        #expect(labels(layout, row: 2) == ["(", ")", "-", "_", ":", ";", "'", "\"", "!", "?"])
        #expect(labels(layout, row: 3) == ["=\\<", "~", "•", "…", "°", "×", "÷", "^", ""])
        #expect(layout.rows[3].keys[0].role == .moreSymbols)
        assertPageContract(layout, switchRole: .moreSymbols)
    }

    @Test("Secondary symbol page matches Android")
    func secondary() {
        let layout = SymbolKeyboardLayouts.secondary(.vni)
        #expect(labels(layout, row: 1) == ["[", "]", "{", "}", "<", ">", "\\", "|", "`", "§"])
        #expect(labels(layout, row: 2) == ["€", "£", "¥", "¢", "©", "®", "™", "¶", "·", "✓"])
        #expect(labels(layout, row: 3) == ["?123", "≠", "±", "≈", "≤", "≥", "√", "∞", ""])
        #expect(layout.rows[3].keys[0].role == .symbols)
        assertPageContract(layout, switchRole: .symbols)
    }

    @Test("Symbol page contracts hold for both input methods")
    func completeContracts() {
        for method in KeyboardInputMethod.allCases {
            let primary = SymbolKeyboardLayouts.primary(method)
            let secondary = SymbolKeyboardLayouts.secondary(method)
            assertPageContract(primary, switchRole: .moreSymbols)
            assertPageContract(secondary, switchRole: .symbols)
            for layout in [primary, secondary] {
                let keys = layout.rows.flatMap(\.keys) + (layout.toolbar?.keys ?? [])
                #expect(Set(keys.map(\.id)).count == keys.count)
                #expect(!keys.contains { $0.role == .placeholder })
            }
        }
    }

    @Test("Symbol glyphs are unique across pages")
    func uniqueGlyphs() {
        let primary = symbolGlyphs(SymbolKeyboardLayouts.primary(.telex))
        let secondary = symbolGlyphs(SymbolKeyboardLayouts.secondary(.telex))
        #expect(Set(primary + secondary).count == primary.count + secondary.count)
    }

    @Test("Secure symbols hide toolbar and language swipe")
    func secure() {
        for mode in KeyboardLayoutMode.allCases where mode != .letters {
            let layout = KeyboardLayoutResolver.resolve(
                inputMethod: .vni,
                mode: mode,
                editorMode: .password
            )
            let space = layout.rows.flatMap(\.keys).first { $0.role == .space }
            #expect(layout.toolbar == nil)
            #expect(space?.label == "English")
            #expect(space?.horizontalSwipeAction == nil)
        }
    }

    private func labels(_ layout: KeyboardLayout, row: Int) -> [String] {
        layout.rows[row].keys.map(\.label)
    }

    private func assertPageContract(_ layout: KeyboardLayout, switchRole: KeyRole) {
        for row in layout.rows.prefix(3) {
            #expect(row.keys.allSatisfy { $0.role == .punctuation })
            #expect(row.keys.allSatisfy { $0.widthWeight == 1 && $0.secondaryLabel == nil })
        }
        let switchRow = layout.rows[3].keys
        #expect(switchRow.map(\.role) == [
            switchRole, .punctuation, .punctuation, .punctuation, .punctuation,
            .punctuation, .punctuation, .punctuation, .backspace,
        ])
        #expect(switchRow.map(\.widthWeight) == [1.5, 1, 1, 1, 1, 1, 1, 1, 1.5])
        let action = layout.rows[4].keys
        #expect(action.map(\.label) == ["ABC", ",", "Tiếng Việt", ".", ""])
        #expect(action.map(\.role) == [.letters, .punctuation, .space, .punctuation, .enter])
        #expect(action.map(\.widthWeight) == [1.7, 1, 5.8, 1, 1.7])
        #expect(action[2].horizontalSwipeAction == .toggleLanguage)
    }

    private func symbolGlyphs(_ layout: KeyboardLayout) -> [String] {
        layout.rows.flatMap(\.keys)
            .filter { $0.id.hasPrefix("symbol-") }
            .map(\.label)
    }
}
