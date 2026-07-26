import KeyboardLayout
import Testing

struct VietnameseKeyAlternatesTests {
    @Test("Vietnamese catalog contains every tone and shape in stable order")
    func catalog() {
        #expect(texts("a") == Array("aáàảãạăắằẳẵặâấầẩẫậ").map(String.init))
        #expect(texts("e") == Array("eéèẻẽẹêếềểễệ").map(String.init))
        #expect(texts("i") == Array("iíìỉĩị").map(String.init))
        #expect(texts("o") == Array("oóòỏõọôốồổỗộơớờởỡợ").map(String.init))
        #expect(texts("u") == Array("uúùủũụưứừửữự").map(String.init))
        #expect(texts("y") == Array("yýỳỷỹỵ").map(String.init))
        #expect(texts("d") == ["d", "đ"])
        #expect(texts("b").isEmpty)
    }

    @Test("Shifted variants are uppercase")
    func uppercase() {
        let values = VietnameseKeyAlternates.values(for: "o")
        #expect(values.map { $0.text(for: .uppercase) }
            == Array("OÓÒỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢ").map(String.init))
    }

    @Test("Only Text and Search alphabetic layouts expose alternates")
    func editorPolicy() {
        for method in KeyboardInputMethod.allCases {
            #expect(!alternates(
                in: EditorKeyboardLayouts.resolve(method, editorMode: .text)
            ).isEmpty)
            #expect(!alternates(
                in: EditorKeyboardLayouts.resolve(method, editorMode: .search)
            ).isEmpty)
            for mode in [KeyboardEditorMode.email, .url, .password] {
                #expect(alternates(
                    in: EditorKeyboardLayouts.resolve(method, editorMode: mode)
                ).isEmpty)
            }
        }
    }

    private func texts(_ character: Character) -> [String] {
        VietnameseKeyAlternates.values(for: character).map(\.text)
    }

    private func alternates(in layout: KeyboardLayout) -> [KeyAlternate] {
        layout.rows.flatMap(\.keys).flatMap(\.alternates)
    }
}
