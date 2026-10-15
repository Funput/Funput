import KeyboardLayout
import Testing

struct PadEditorLayoutTests {
    @Test("Email and URL put the character they reach for beside m", arguments: [
        (KeyboardEditorMode.email, "@"), (.url, "/"), (.password, ","), (.search, ","),
    ])
    func bottomRow(editor: KeyboardEditorMode, label: String) {
        let row = letterRows(resolve(editor))[2].keys.map(\.label)
        #expect(row == ["shift", "z", "x", "c", "v", "b", "n", "m", label, ".", "shift"])
    }

    @Test("Fields that do not compose drop the Telex hints and the language swipe", arguments: [
        KeyboardEditorMode.email, .url, .password,
    ])
    func plainLetters(editor: KeyboardEditorMode) {
        let keys = resolve(editor).rows.flatMap(\.keys)
        #expect(keys.filter { $0.role == .character }.allSatisfy { $0.secondaryLabel == nil && $0.alternates.isEmpty })
        #expect(keys.allSatisfy { $0.horizontalSwipeAction == nil && $0.role != .vniModifier })
        #expect(keys.first { $0.role == .space }?.label == "English")
    }

    @Test("A password keeps its digit row and has no toolbar")
    func password() {
        let layout = resolve(.password, showsNumberRow: false)
        #expect(layout.hasNumberRow)
        #expect(layout.toolbar == nil)
        #expect(layout.rows.last?.keys.contains { $0.role == .emoji } == false)
    }

    @Test("Email and URL follow the number row preference")
    func compact() {
        #expect(!resolve(.email, showsNumberRow: false).hasNumberRow)
        #expect(resolve(.url, showsNumberRow: true).hasNumberRow)
        #expect(resolve(.url).toolbar != nil)
    }

    @Test("Each field kind gets its own layout id")
    func ids() {
        let ids = [KeyboardEditorMode.text, .email, .url, .password].map { resolve($0).id }
        #expect(Set(ids).count == ids.count)
    }

    private func resolve(_ editor: KeyboardEditorMode, showsNumberRow: Bool = true) -> KeyboardLayout {
        KeyboardLayoutResolver.resolve(
            inputMethod: .telex,
            mode: .letters,
            editorMode: editor,
            showsNumberRow: showsNumberRow,
            preset: .system,
            formFactor: .pad
        )
    }

    private func letterRows(_ layout: KeyboardLayout) -> [KeyboardRow] {
        layout.rows.filter { !$0.isNumberRow }
    }
}
