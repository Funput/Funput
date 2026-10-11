import CoreGraphics
import KeyboardLayout
import Testing

struct PadSymbolLayoutTests {
    @Test("The first symbol page follows the stock iPad page")
    func primaryRows() {
        let layout = resolve(.symbolsPrimary)
        #expect(layout.rows.map { $0.keys.map(\.label) } == [
            ["tab", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "delete"],
            ["Telex", "@", "#", "\u{20AB}", "&", "*", "(", ")", "'", "\"", "return"],
            ["#+=", "%", "-", "+", "=", "/", ";", ":", ",", ".", "#+="],
            ["", "ABC", "Tiếng Việt", "ABC", ""],
        ])
        #expect(layout.rows[0].keys.map(\.role).first == .tab)
        #expect(layout.rows[0].keys.last?.role == .backspace)
        #expect(layout.rows[1].keys.map(\.role).first == .inputMethod)
        #expect(layout.rows[1].keys.last?.role == .enter)
        #expect(layout.rows[3].keys.map(\.role) == [.globe, .letters, .space, .letters, .dismissKeyboard])
    }

    @Test("The second page switches back with 123 at both ends")
    func secondaryRows() {
        let row = resolve(.symbolsSecondary).rows[2].keys
        #expect(row.first?.role == .symbols && row.last?.role == .symbols)
        #expect(row.first?.label == "123")
    }

    @Test("The method key names the method in use")
    func methodLabel() {
        #expect(resolve(.symbolsPrimary, method: .vni).rows[1].keys.first?.label == "VNI")
        #expect(resolve(.symbolsPrimary, method: .telexAdvanced).rows[1].keys.first?.label == "Telex")
    }

    @Test("Fields that do not compose leave the method slot empty", arguments: [
        KeyboardEditorMode.email, .url, .password,
    ])
    func plainFields(editor: KeyboardEditorMode) {
        let layout = resolve(.symbolsPrimary, editor: editor)
        #expect(layout.rows[1].keys.first?.role == .placeholder)
        #expect(layout.rows[3].keys.allSatisfy { $0.horizontalSwipeAction == nil })
        #expect((layout.toolbar == nil) == (editor == .password))
    }

    @Test("Symbol pages are budgeted for the letters page's height")
    func heightBudget() {
        for (method, numberRow, rows) in [
            (KeyboardInputMethod.telex, true, 5), (.telex, false, 4), (.vni, false, 5),
        ] {
            let letters = resolve(.letters, method: method, showsNumberRow: numberRow)
            #expect(letters.rows.count == rows)
            for mode in [KeyboardLayoutMode.symbolsPrimary, .symbolsSecondary] {
                let symbols = resolve(mode, method: method, showsNumberRow: numberRow)
                #expect(symbols.rows.count == 4)
                #expect(symbols.heightRowCount == rows)
            }
        }
    }

    @Test("Symbols keep the letters' columns", arguments: [
        KeyboardLayoutMode.symbolsPrimary, .symbolsSecondary,
    ])
    func columns(mode: KeyboardLayoutMode) {
        let size = CGSize(width: 1024, height: 400)
        let letters = KeyboardGeometry.resolve(layout: resolve(.letters, showsNumberRow: false), size: size, sizing: .default)
        let symbols = KeyboardGeometry.resolve(layout: resolve(mode), size: size, sizing: .default)
        for (letterRow, symbolRow) in zip(letters.rows.dropLast(), symbols.rows.dropLast()) {
            let letterX = letterRow.map(\.frame.minX)
            let symbolX = symbolRow.map(\.frame.minX)
            #expect(letterX.count == symbolX.count)
            #expect(zip(letterX, symbolX).allSatisfy { abs($0 - $1) <= 1 })
        }
        #expect(Set(symbols.keys.map { $0.spec.id }).count == symbols.keys.count)
    }

    private func resolve(
        _ mode: KeyboardLayoutMode,
        method: KeyboardInputMethod = .telex,
        editor: KeyboardEditorMode = .text,
        showsNumberRow: Bool = true
    ) -> KeyboardLayout {
        KeyboardLayoutResolver.resolve(
            inputMethod: method,
            mode: mode,
            editorMode: editor,
            showsNumberRow: showsNumberRow,
            preset: .system,
            formFactor: .pad
        )
    }
}
