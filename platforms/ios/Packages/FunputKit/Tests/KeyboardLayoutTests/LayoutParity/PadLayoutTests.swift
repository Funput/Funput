import CoreGraphics
import KeyboardLayout
import Testing

struct PadLayoutTests {
    @Test("The iPad letters page follows the stock keyboard, with no dictation key", arguments: KeyboardLayoutPreset.allCases)
    func compactRows(preset: KeyboardLayoutPreset) {
        let layout = resolve(.telex, showsNumberRow: false, preset: preset)
        #expect(rows(layout) == [
            ["tab", "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", ""],
            ["caps lock", "a", "s", "d", "f", "g", "h", "j", "k", "l", ""],
            ["", "z", "x", "c", "v", "b", "n", "m", ",", ".", ""],
            ["", "?123", "Tiếng Việt", "?123", ""],
        ])
        #expect(layout.rows[0].keys.last?.role == .backspace)
        #expect(layout.rows[1].keys.last?.role == .enter)
        #expect(layout.rows[2].keys.map(\.role).filter { $0 == .shift }.count == 2)
        #expect(layout.rows[3].keys.map(\.role) == [.globe, .symbols, .space, .symbols, .dismissKeyboard])
        #expect(layout.toolbar != nil)
    }

    @Test("Both presets share one bar-for-bar layout on iPad")
    func presetsMatch() {
        let funput = resolve(.telex, showsNumberRow: true, preset: .funput)
        let system = resolve(.telex, showsNumberRow: true, preset: .system)
        #expect(funput.rows == system.rows)
        #expect(funput.id != system.id)
    }

    @Test("The digit row follows the preference, and VNI always keeps it")
    func numberRow() {
        #expect(resolve(.telex, showsNumberRow: true).hasNumberRow)
        #expect(!resolve(.telex, showsNumberRow: false).hasNumberRow)
        #expect(resolve(.vni, showsNumberRow: false).hasNumberRow)
        #expect(resolve(.telex, showsNumberRow: true).rows.count == 5)
    }

    @Test("A compact page hands the digits to the top row")
    func compactDigits() {
        let top = resolve(.telex, showsNumberRow: false).rows[0].keys
        let letters = top.filter { $0.role == .character }
        #expect(letters.compactMap { $0.alternates.first?.text } == "1234567890".map(String.init))
        #expect(top.first?.role == .tab)
    }

    @Test("Only the letters page of a text or search field changes")
    func scope() {
        for mode in [KeyboardLayoutMode.symbolsPrimary, .symbolsSecondary] {
            #expect(resolve(.telex, mode: mode) == phone(.telex, mode: mode))
        }
        for editor in KeyboardEditorMode.allCases where !editor.usesSystemPreset {
            #expect(resolve(.telex, editor: editor) == phone(.telex, editor: editor))
        }
        let search = resolve(.telex, editor: .search, showsNumberRow: false)
        #expect(search.rows.first?.keys.first?.role == .tab)
    }

    @Test("A phone is unchanged by the form factor")
    func phoneUnchanged() {
        let plain = KeyboardLayoutResolver.resolve(inputMethod: .telex, mode: .letters)
        #expect(plain == phone(.telex))
        #expect(!plain.rows.flatMap(\.keys).contains { $0.role == .tab })
    }

    @Test("Without a toolbar the emoji key moves into the action row")
    func toolbarless() {
        let layout = KeyboardLayoutResolver.resolve(
            inputMethod: .telex,
            mode: .letters,
            showsToolbar: false,
            formFactor: .pad
        )
        #expect(layout.toolbar == nil)
        #expect(layout.rows.last?.keys.contains { $0.role == .emoji } == true)
    }

    @Test("Pad geometry is valid for every input method and preset", arguments: KeyboardInputMethod.allCases)
    func geometry(method: KeyboardInputMethod) {
        for preset in KeyboardLayoutPreset.allCases {
            for showsNumberRow in [true, false] {
                let layout = resolve(method, showsNumberRow: showsNumberRow, preset: preset)
                let result = KeyboardGeometry.resolve(
                    layout: layout,
                    size: CGSize(width: 1024, height: 400),
                    sizing: .default
                )
                #expect(Set(result.keys.map { $0.spec.id }).count == result.keys.count)
                #expect(result.keys.allSatisfy { $0.frame.width > 0 && $0.frame.height > 0 })
                #expect(result.keys.allSatisfy { $0.frame.maxX <= result.size.width + 0.5 })
            }
        }
    }

    @Test("Letters keep their column from row to row")
    func lettersAlign() {
        let layout = resolve(.telex, showsNumberRow: false)
        let result = KeyboardGeometry.resolve(
            layout: layout,
            size: CGSize(width: 1024, height: 400),
            sizing: .default
        )
        let widths = result.rows.map { row in row.filter { $0.spec.role == .character }.map(\.frame.width) }
        let reference = widths[0][0]
        #expect(widths.flatMap { $0 }.allSatisfy { abs($0 - reference) <= 1.5 })
    }

    private func resolve(
        _ method: KeyboardInputMethod,
        mode: KeyboardLayoutMode = .letters,
        editor: KeyboardEditorMode = .text,
        showsNumberRow: Bool = true,
        preset: KeyboardLayoutPreset = .funput
    ) -> KeyboardLayout {
        KeyboardLayoutResolver.resolve(
            inputMethod: method,
            mode: mode,
            editorMode: editor,
            showsNumberRow: showsNumberRow,
            preset: preset,
            formFactor: .pad
        )
    }

    private func phone(
        _ method: KeyboardInputMethod,
        mode: KeyboardLayoutMode = .letters,
        editor: KeyboardEditorMode = .text
    ) -> KeyboardLayout {
        KeyboardLayoutResolver.resolve(
            inputMethod: method,
            mode: mode,
            editorMode: editor,
            formFactor: .phone
        )
    }

    /// Labels of the page's character rows; the digit row, when present, is skipped.
    private func rows(_ layout: KeyboardLayout) -> [[String]] {
        layout.rows.filter { !$0.isNumberRow }.map { $0.keys.map(\.label) }
    }
}
