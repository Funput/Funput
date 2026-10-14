import KeyboardLayout
import Testing

struct PadSymbolHintsTests {
    private static let expected: [String: String] = [
        "a": "@", "s": "#", "d": "$", "f": "&", "g": "*", "h": "(", "j": ")", "k": "'", "l": "\"",
        "z": "%", "x": "-", "c": "+", "v": "=", "b": "/", "n": ";", "m": ":",
    ]

    @Test("The compact page prints the stock symbols and offers them first on long press")
    func symbols() {
        let keys = pad(.text, showsNumberRow: false).rows.flatMap(\.keys)
        for (letter, symbol) in Self.expected {
            let key = keys.first { $0.label == letter }
            #expect(key?.secondaryLabel == symbol, "\(letter)")
            #expect(key?.alternates.first?.text == symbol, "\(letter)")
        }
    }

    @Test("Vietnamese alternates stay behind the symbol")
    func vietnameseAlternates() throws {
        let keys = pad(.text, showsNumberRow: false).rows.flatMap(\.keys)
        let a = try #require(keys.first { $0.label == "a" })
        #expect(a.alternates.count > 1)
    }

    @Test("No tone hint is printed on iPad")
    func noToneHints() {
        for showsNumberRow in [true, false] {
            let keys = pad(.text, showsNumberRow: showsNumberRow).rows.flatMap(\.keys)
            let hints = keys.filter { $0.role == .character }.compactMap(\.secondaryLabel)
            #expect(!hints.contains { ["´", "`", "̉", "˜", "̣", "×"].contains($0) })
        }
    }

    @Test("A page with a number row leaves the letters bare")
    func numberRowPage() {
        let keys = pad(.text, showsNumberRow: true).rows.flatMap(\.keys)
        #expect(keys.filter { ["a", "s", "z", "m"].contains($0.label) }.allSatisfy { $0.secondaryLabel == nil })
    }

    @Test("Email gets the symbols too, and the phone is unchanged")
    func scope() {
        let email = pad(.email, showsNumberRow: false).rows.flatMap(\.keys)
        #expect(email.first { $0.label == "a" }?.secondaryLabel == "@")
        let phone = KeyboardLayoutResolver.resolve(inputMethod: .telex, mode: .letters, formFactor: .phone)
        #expect(phone.rows.flatMap(\.keys).first { $0.label == "a" }?.secondaryLabel == nil)
    }

    private func pad(_ editor: KeyboardEditorMode, showsNumberRow: Bool) -> KeyboardLayout {
        KeyboardLayoutResolver.resolve(
            inputMethod: .telex,
            mode: .letters,
            editorMode: editor,
            showsNumberRow: showsNumberRow,
            preset: .system,
            formFactor: .pad
        )
    }
}
