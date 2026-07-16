import KeyboardLayout
import Testing

struct CompactSymbolLayoutParityTests {
    @Test("Compact Telex pages keep a stable four-row family")
    func stableFamily() {
        for mode in KeyboardLayoutMode.allCases {
            #expect(resolve(mode).rows.count == 4)
        }
    }

    @Test("Compact primary page keeps digits and common symbols")
    func primary() {
        let layout = resolve(.symbolsPrimary)
        #expect(labels(layout, row: 0) == ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0"])
        #expect(labels(layout, row: 1) == ["@", "#", "₫", "$", "%", "&", "*", "+", "=", "/"])
        #expect(labels(layout, row: 2) == ["=\\<", "(", ")", "-", "_", ":", "!", "?", ""])
        assertContract(layout, switchRole: .moreSymbols)
    }

    @Test("Compact secondary page keeps the selected extended symbols")
    func secondary() {
        let layout = resolve(.symbolsSecondary)
        #expect(labels(layout, row: 0) == [";", "'", "\"", "~", "•", "…", "°", "×", "÷", "^"])
        #expect(labels(layout, row: 1) == ["[", "]", "{", "}", "<", ">", "\\", "|", "`", "€"])
        #expect(labels(layout, row: 2) == ["?123", "£", "¥", "©", "≠", "±", "≤", "≥", ""])
        assertContract(layout, switchRole: .symbols)
    }

    @Test("Compact pages omit only the agreed rare symbols")
    func omittedSymbols() {
        let layouts = [resolve(.symbolsPrimary), resolve(.symbolsSecondary)]
        let labels = Set(layouts.flatMap { $0.rows.flatMap(\.keys).map(\.label) })
        let omitted = ["§", "¢", "®", "™", "¶", "·", "✓", "≈", "√", "∞"]
        #expect(omitted.allSatisfy { !labels.contains($0) })
    }

    @Test("VNI and specialized editors ignore the compact preference")
    func standardOnlyContexts() {
        for mode in KeyboardLayoutMode.allCases {
            #expect(resolve(mode, method: .vni).rows.count == 5)
        }
        for editor in [KeyboardEditorMode.search, .email, .url, .password] {
            for mode in KeyboardLayoutMode.allCases {
                let layout = resolve(mode, editorMode: editor)
                #expect(layout.rows.count == 5)
            }
        }
    }

    private func resolve(
        _ mode: KeyboardLayoutMode,
        method: KeyboardInputMethod = .telex,
        editorMode: KeyboardEditorMode = .text
    ) -> KeyboardLayout {
        KeyboardLayoutResolver.resolve(
            inputMethod: method,
            mode: mode,
            editorMode: editorMode,
            showsNumberRow: false
        )
    }

    private func labels(_ layout: KeyboardLayout, row: Int) -> [String] {
        layout.rows[row].keys.map(\.label)
    }

    private func assertContract(_ layout: KeyboardLayout, switchRole: KeyRole) {
        let keys = layout.rows.flatMap(\.keys) + (layout.toolbar?.keys ?? [])
        #expect(Set(keys.map(\.id)).count == keys.count)
        #expect(keys.allSatisfy { !$0.accessibilityLabel.isEmpty })
        #expect(layout.rows[2].keys.map(\.role) == [
            switchRole, .punctuation, .punctuation, .punctuation, .punctuation,
            .punctuation, .punctuation, .punctuation, .backspace,
        ])
        let action = layout.rows[3].keys
        #expect(action.map(\.role) == [.letters, .punctuation, .space, .punctuation, .enter])
        #expect(action.first { $0.role == .space }?.horizontalSwipeAction == .toggleLanguage)
    }
}
